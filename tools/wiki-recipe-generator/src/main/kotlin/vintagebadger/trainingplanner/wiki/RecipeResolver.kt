package vintagebadger.trainingplanner.wiki

import vintagebadger.trainingplanner.models.Skill

class RecipeResolver(
    private val client: WikiClient,
    private val parser: RecipeParser,
    private val maxDepth: Int,
) {
    fun resolveMethod(target: String, skillHint: String?): ResolvedMethod {
        val state = ResolveState()
        val tree = resolveNode(target, 0, linkedSetOf(), state)
        val finalStep = tree.recipeStep
        val steps = tree.flattenSteps()
        val output = finalStep?.output ?: listOf(tree.item)
        val skill = skillHint ?: finalStep?.skill ?: ""

        return ResolvedMethod(
            name = normalizeTitle(target),
            wikiPage = tree.pageTitle ?: normalizeTitle(target),
            skill = skill,
            totalXp = steps.sumOf { it.xp },
            maxLevel = steps.maxOfOrNull { it.level } ?: 0,
            output = output,
            steps = steps,
            warnings = (state.warnings + tree.collectWarnings()).distinct(),
        )
    }

    private fun resolveNode(
        title: String,
        depth: Int,
        stack: LinkedHashSet<String>,
        state: ResolveState,
    ): ResolvedRecipeNode {
        val normalized = normalizeTitle(title)
        val identity = normalizeIdentity(normalized)
        val item = itemReference(normalized, state.itemIds[identity] ?: baseIngredientIds[identity] ?: 0, 1)

        if (depth > maxDepth) {
            return ResolvedRecipeNode(
                item = item,
                pageTitle = normalized,
                recipeStep = null,
                children = emptyList(),
                status = ResolutionStatus.MAX_DEPTH,
                warnings = listOf("MAX_DEPTH:$normalized"),
            )
        }
        if (identity in baseIngredientIdentities) {
            baseIngredientIds[identity]?.let {
                state.itemIds[identity] = it
            }
            return ResolvedRecipeNode(
                item = item.copy(id = state.itemIds[identity] ?: item.id),
                pageTitle = normalized,
                recipeStep = null,
                children = emptyList(),
                status = ResolutionStatus.BASE,
                warnings = emptyList(),
            )
        }
        if (!stack.add(identity)) {
            return ResolvedRecipeNode(
                item = item,
                pageTitle = normalized,
                recipeStep = null,
                children = emptyList(),
                status = ResolutionStatus.CYCLE,
                warnings = listOf("CYCLE_DETECTED:$normalized"),
            )
        }

        val page = client.fetchPage(normalized)
        state.mergeItemIds(parser.parseItemIds(page, normalized))
        val pageItem = item.copy(id = state.itemIds[identity] ?: item.id)

        val recipes = parser.parseRecipes(page)
        val selected = selectRecipe(normalized, recipes, state)

        if (selected == null) {
            val cleaning = tryHerbCleaning(normalized, state)
            stack.remove(identity)
            return if (cleaning != null) {
                ResolvedRecipeNode(
                    item = cleaning.output.firstOrNull() ?: pageItem,
                    pageTitle = page.normalizedTitle,
                    recipeStep = cleaning,
                    children = emptyList(),
                    status = ResolutionStatus.RESOLVED,
                    warnings = page.warnings,
                )
            } else {
                ResolvedRecipeNode(
                    item = pageItem,
                    pageTitle = page.normalizedTitle,
                    recipeStep = null,
                    children = emptyList(),
                    status = ResolutionStatus.UNRESOLVED,
                    warnings = page.warnings + if (page.wikitext.isNotEmpty()) listOf("NO_STRUCTURED_RECIPE:$normalized") else emptyList(),
                )
            }
        }

        selected.inputs.forEach {
            if (it.itemId != 0) state.itemIds[normalizeIdentity(it.name)] = it.itemId
        }
        selected.outputs.forEach {
            if (it.itemId != 0) state.itemIds[normalizeIdentity(it.name)] = it.itemId
        }

        val children = selected.inputs.map { input ->
            resolveNode(input.name, depth + 1, stack, state)
        }
        val step = selected.toResolvedStep(state)
        stack.remove(identity)
        return ResolvedRecipeNode(
            item = step.output.firstOrNull() ?: pageItem,
            pageTitle = page.normalizedTitle,
            recipeStep = step,
            children = children,
            status = ResolutionStatus.RESOLVED,
            warnings = page.warnings + selected.warnings,
        )
    }

    private fun selectRecipe(target: String, recipes: List<ParsedRecipe>, state: ResolveState): ParsedRecipe? {
        if (recipes.isEmpty()) return null
        val matches = recipes.filter { recipe ->
            recipe.outputs.any { outputMatchesTarget(it.name, target) }
        }
        if (matches.isEmpty()) return null
        if (matches.size == 1) return matches.first()
        if (normalizeIdentity(target) in HerbCleaning.known) return null

        val ranked = matches.sortedWith(
            compareBy<ParsedRecipe> { recipeVariantScore(it) }
                .thenBy { it.inputs.size }
                .thenBy { if (it.members == false) 0 else 1 }
                .thenBy { it.maxLevel }
        )
        val best = ranked.first()
        val second = ranked.getOrNull(1)
        if (second == null || recipeRank(best) < recipeRank(second)) {
            return best
        }

        val bestRank = recipeRank(best)
        val tiedBest = ranked.filter { recipeRank(it) == bestRank }
        val uniqueSignatures = tiedBest.map { recipeSignature(it) }.distinct()
        return if (uniqueSignatures.size == 1) {
            tiedBest.first()
        } else {
            state.warn("AMBIGUOUS_RECIPE:$target")
            tiedBest.minBy { matches.indexOf(it) }
        }
    }

    private fun recipeVariantScore(recipe: ParsedRecipe): Int {
        val variant = recipe.variantName.orEmpty().lowercase()
        val outputNotes = recipe.outputs.joinToString(" ") { it.note.orEmpty() }.lowercase()
        return when {
            "standard" in variant || "regular" in variant -> 0
            "standard" in outputNotes || "regular" in outputNotes -> 0
            recipe.facilities?.contains("furnace", ignoreCase = true) == true &&
                recipe.variantName?.contains("blast", ignoreCase = true) != true -> 0
            else -> 1
        }
    }

    private fun recipeRank(recipe: ParsedRecipe): RecipeRank {
        return RecipeRank(
            variantScore = recipeVariantScore(recipe),
            inputCount = recipe.inputs.size,
            membersScore = if (recipe.members == false) 0 else 1,
            maxLevel = recipe.maxLevel,
        )
    }

    private fun recipeSignature(recipe: ParsedRecipe): String {
        return "${recipe.skillRequirements}|${recipe.inputs.map { it.name to it.quantity }}|${recipe.outputs.map { it.name to it.quantity }}"
    }

    private fun outputMatchesTarget(output: String, target: String): Boolean {
        val outputIdentity = normalizeIdentity(output)
        val targetIdentity = normalizeIdentity(target)
        return outputIdentity == targetIdentity || potionFamilyIdentity(outputIdentity) == potionFamilyIdentity(
            targetIdentity
        )
    }

    private fun potionFamilyIdentity(value: String): String {
        return value.replace(Regex("\\([1-4]\\)$"), "")
    }

    private fun ParsedRecipe.toResolvedStep(state: ResolveState): ResolvedStep {
        val resolvedInputs = inputs.map { it.toItemReference(state) }
        val resolvedOutputs = outputs.map { it.toItemReference(state) }
        val primaryRequirement = skillRequirements.firstOrNull()
        val skillName = primaryRequirement?.skill.orEmpty()
        val outputLabel = resolvedOutputs.joinToString(" + ") { formatItem(it) }.ifBlank { sourcePage }
        val inputLabel = resolvedInputs.joinToString(" + ") { formatItem(it) }
        val action =
            if (resolvedInputs.size == 1 && resolvedInputs.first().name.startsWith("Grimy ", ignoreCase = true)) {
                "Clean ${resolvedInputs.first().name.replaceFirstChar { it.lowercase() }}"
            } else {
                "Make ${resolvedOutputs.firstOrNull()?.name ?: sourcePage}"
            }
        return ResolvedStep(
            step = action,
            name = if (inputLabel.isBlank()) outputLabel else "$inputLabel -> $outputLabel",
            skill = skillName,
            level = primaryRequirement?.level ?: 0,
            xp = primaryRequirement?.xp ?: 0.0,
            input = resolvedInputs,
            output = resolvedOutputs,
        )
    }

    private fun ParsedItemQuantity.toItemReference(state: ResolveState): ItemRef {
        val id = if (itemId != 0) itemId else state.itemIds[normalizeIdentity(name)] ?: 0
        if (id == 0) state.warn("MISSING_ITEM_ID:$name")
        return itemReference(name, id, quantity)
    }

    private fun tryHerbCleaning(cleanHerb: String, state: ResolveState): ResolvedStep? {
        val known = HerbCleaning.known[normalizeIdentity(cleanHerb)] ?: return null
        state.warn("HEURISTIC_HERB_CLEANING:${known.cleanName}")
        state.itemIds[normalizeIdentity(known.cleanName)] = known.cleanId
        state.itemIds[normalizeIdentity(known.grimyName)] = known.grimyId
        return ResolvedStep(
            step = "Clean ${known.grimyName.replaceFirstChar { it.lowercase() }}",
            name = "${known.grimyName} -> ${known.cleanName}",
            skill = Skill.HERBLORE.displayName,
            level = known.level,
            xp = known.xp,
            input = listOf(itemReference(known.grimyName, known.grimyId, 1)),
            output = listOf(itemReference(known.cleanName, known.cleanId, 1)),
        )
    }

    companion object {
        private val baseIngredientIdentities = setOf(
            "vial of water",
            "vial",
            "molten glass",
        )
        private val baseIngredientIds = mapOf(
            "vial of water" to 227,
            "vial" to 229,
            "molten glass" to 1775,
        )
    }
}

class ResolveState {
    val warnings = mutableListOf<String>()
    val itemIds = mutableMapOf<String, Int>()

    fun warn(warning: String) {
        if (warning.isNotBlank() && warning !in warnings) warnings += warning
    }

    fun mergeItemIds(ids: Map<String, Int>) {
        ids.forEach { (name, id) ->
            if (id != 0) itemIds[normalizeIdentity(name)] = id
        }
    }
}

data class ResolvedMethod(
    val name: String,
    val wikiPage: String,
    val skill: String,
    val totalXp: Double,
    val maxLevel: Int,
    val output: List<ItemRef>,
    val steps: List<ResolvedStep>,
    val warnings: List<String>,
)

data class RecipeRank(
    val variantScore: Int,
    val inputCount: Int,
    val membersScore: Int,
    val maxLevel: Int,
) : Comparable<RecipeRank> {
    override fun compareTo(other: RecipeRank): Int {
        return compareValuesBy(
            this,
            other,
            RecipeRank::variantScore,
            RecipeRank::inputCount,
            RecipeRank::membersScore,
            RecipeRank::maxLevel
        )
    }
}

data class ResolvedStep(
    val step: String,
    val name: String,
    val skill: String,
    val level: Int,
    val xp: Double,
    val input: List<ItemRef>,
    val output: List<ItemRef>,
)

data class ResolvedRecipeNode(
    val item: ItemRef,
    val pageTitle: String?,
    val recipeStep: ResolvedStep?,
    val children: List<ResolvedRecipeNode>,
    val status: ResolutionStatus,
    val warnings: List<String>,
) {
    fun flattenSteps(): List<ResolvedStep> {
        return children.flatMap { it.flattenSteps() } + listOfNotNull(recipeStep)
    }

    fun collectWarnings(): List<String> {
        return warnings + children.flatMap { it.collectWarnings() }
    }
}

enum class ResolutionStatus {
    RESOLVED,
    BASE,
    UNRESOLVED,
    MAX_DEPTH,
    CYCLE,
}
