package vintagebadger.trainingplanner.wiki

import java.security.MessageDigest

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
        val requestedTitles = titles
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy(::normalizeTitle)
        WikiLog.log.debug("Starting recipe graph build for {} requested item(s)", requestedTitles.size)
        requestedTitles.forEach(::crawl)
        WikiLog.log.debug("Finished recipe graph build with {} output item(s)", recipes.size)
        return RecipeGraph(recipes = recipes)
    }

    private fun crawl(title: String) {
        WikiLog.log.debug("Crawling recipe item '{}'", title)
        val page = fetchCached(title) ?: run {
            WikiLog.log.warn("Skipping recipe item '{}' because its wiki page was not found", title)
            return
        }
        val identity = parseIdentity(page.wikitext, requestedName = title) ?: ItemIdentity(null, page.title)
        val outputId = identity.id

        if (outputId == null) {
            System.err.println("Warning: skipping '${identity.name}' because no item ID was found")
            return
        }
        WikiLog.log.debug("Resolved '{}' to item '{}' (ID {})", title, identity.name, outputId)
        itemNameById[outputId] = identity.name

        if (!processedIds.add(outputId)) {
            WikiLog.log.debug("Skipping already processed item '{}' (ID {})", identity.name, outputId)
            return
        }

        val recipes = parseRecipeRequirements(page.wikitext, targetName = identity.name)
        WikiLog.log.debug("Parsed {} recipe method(s) for '{}'", recipes.size, identity.name)
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
                methodKey = buildMethodKey(
                    outputId = outputId,
                    method = recipe.method,
                    outputQuantity = recipe.outputQuantity,
                    skills = recipe.skills,
                    ingredients = resolvedIngredients,
                ),
                method = recipe.method,
                outputQuantity = recipe.outputQuantity,
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

        val ingredientIds = flatRecipes
            .flatMap { it.requires }
            .map { it.id }
            .distinct()
        WikiLog.log.debug("Recursing from '{}' into {} unique ingredient(s)", identity.name, ingredientIds.size)
        ingredientIds.forEach { ingredientId ->
                val ingredientName = itemNameById[ingredientId]
                if (ingredientName != null) {
                    crawl(ingredientName)
                }
            }
    }

    private fun fetchCached(title: String): WikiPage? {
        val key = normalizeTitle(title)
        pageCache[key]?.let {
            WikiLog.log.debug("Using cached wiki page for '{}'", title)
            return it
        }
        val page = wikiClient.fetchPage(title) ?: return null
        pageCache[key] = page
        return page
    }

    private fun resolveIngredient(ingredient: IngredientRequirement): ResolvedIngredient? {
        val key = normalizeTitle(ingredient.name)
        val cachedIdentity = identityCache[key]
        val identity = cachedIdentity ?: run {
            val page = fetchCached(ingredient.name) ?: return null
            identityCache.getOrPut(key) {
                parseIdentity(page.wikitext, requestedName = ingredient.name) ?: ItemIdentity(null, page.title)
            }
        }
        WikiLog.log.debug(
            "Resolved ingredient '{}'{} to '{}' (ID {})",
            ingredient.name,
            if (cachedIdentity == null) "" else " from cache",
            identity.name,
            identity.id
        )

        if (identity.id == null) {
            System.err.println("Warning: skipping '${ingredient.name}' because no item ID was found")
            return null
        }
        itemNameById[identity.id] = identity.name

        return ResolvedIngredient(id = identity.id, name = identity.name, quantity = ingredient.quantity)
    }

    private fun buildMethodKey(
        outputId: Int,
        method: String,
        outputQuantity: Int,
        skills: List<SkillRequirement>,
        ingredients: List<ResolvedIngredient>,
    ): String {
        val signature = buildString {
            append(outputId)
            append('|')
            append(normalizeTitle(method))
            append('|')
            append(outputQuantity)
            ingredients.sortedWith(compareBy(ResolvedIngredient::id, ResolvedIngredient::quantity)).forEach {
                append('|')
                append(it.id)
                append(':')
                append(it.quantity)
            }
            skills.sortedBy { normalizeTitle(it.skill) }.forEach {
                append('|')
                append(normalizeTitle(it.skill))
                append(':')
                append(it.level)
                append(':')
                append(it.xp)
            }
        }
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(signature.toByteArray(Charsets.UTF_8))
            .take(6)
            .joinToString("") { "%02x".format(it) }
        val prefix = normalizeTitle(method)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "method" }
        return "$prefix-$digest"
    }
}
