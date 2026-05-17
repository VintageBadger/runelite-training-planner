package vintagebadger.trainingplanner.wiki

private data class ResolvedIngredient(
    val id: Int,
    val name: String,
    val quantity: Int
)

internal class RecipeGraphBuilder(
    private val wikiClient: WikiClient = WikiClient()
) {
    private val pageCache = mutableMapOf<String, WikiPage>()
    private val identityCache = mutableMapOf<String, ItemIdentity>()
    private val itemNameById = mutableMapOf<Int, String>()
    private val processedIds = mutableSetOf<Int>()
    private val recipes = mutableListOf<OutputItemRecipes>()

    fun build(title: String): RecipeGraph {
        return build(listOf(title))
    }

    fun build(titles: Iterable<String>): RecipeGraph {
        titles
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy(::normalizeTitle)
            .forEach(::crawl)
        return RecipeGraph(recipes = recipes)
    }

    private fun crawl(title: String) {
        val page = fetchCached(title)
        val identity = parseIdentity(page.wikitext, requestedName = title) ?: ItemIdentity(null, page.title)
        val outputId = identity.id

        if (outputId == null) {
            System.err.println("Warning: skipping '${identity.name}' because no item ID was found")
            return
        }
        itemNameById[outputId] = identity.name

        if (!processedIds.add(outputId)) {
            return
        }

        val recipes = parseRecipeRequirements(page.wikitext, targetName = identity.name)
        if (recipes.isEmpty()) {
            warnIfRecipesTargetOtherOutputs(page.wikitext, identity.name)
        }

        val flatRecipes = recipes.mapNotNull { recipe ->
            val resolvedIngredients = recipe.items.mapNotNull { ingredient ->
                resolveIngredient(ingredient)
            }

            if (resolvedIngredients.size != recipe.items.size) {
                System.err.println("Warning: skipping recipe '${recipe.method}' for '${identity.name}' because an ingredient ID was missing")
                return@mapNotNull null
            }

            FlatRecipe(
                method = recipe.method,
                skills = recipe.skills,
                requires = resolvedIngredients.map {
                    IngredientRef(
                        id = it.id,
                        name = it.name,
                        quantity = it.quantity
                    )
                }
            )
        }

        if (flatRecipes.isNotEmpty()) {
            this.recipes += OutputItemRecipes(
                id = outputId,
                name = identity.name,
                methods = flatRecipes
            )
        }

        flatRecipes
            .flatMap { it.requires }
            .map { it.id }
            .distinct()
            .forEach { ingredientId ->
                val ingredientName = itemNameById[ingredientId]
                if (ingredientName != null) {
                    crawl(ingredientName)
                }
            }
    }

    private fun fetchCached(title: String): WikiPage {
        val key = normalizeTitle(title)
        return pageCache.getOrPut(key) {
            wikiClient.fetchPage(title)
        }
    }

    private fun resolveIngredient(ingredient: IngredientRequirement): ResolvedIngredient? {
        val key = normalizeTitle(ingredient.name)
        val identity = identityCache.getOrPut(key) {
            val page = fetchCached(ingredient.name)
            parseIdentity(page.wikitext, requestedName = ingredient.name) ?: ItemIdentity(null, page.title)
        }

        if (identity.id == null) {
            System.err.println("Warning: skipping '${ingredient.name}' because no item ID was found")
            return null
        }
        itemNameById[identity.id] = identity.name

        return ResolvedIngredient(id = identity.id, name = identity.name, quantity = ingredient.quantity)
    }
}
