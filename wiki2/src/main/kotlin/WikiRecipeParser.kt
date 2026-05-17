internal data class ItemIdentity(
    val id: Int?,
    val name: String
)

internal data class RecipeRequirement(
    val method: String,
    val xp: List<SkillXp>,
    val items: List<IngredientRequirement>
)

internal data class IngredientRequirement(
    val name: String,
    val quantity: Int
)

internal fun parseIdentity(wikitext: String, requestedName: String): ItemIdentity? {
    val infobox = findTemplates(wikitext, "Infobox Item").firstOrNull() ?: return null
    val params = parseTemplateParams(infobox)

    val requested = normalizeTitle(requestedName)
    val variantNumbers = params.keys
        .mapNotNull { key -> Regex("""^name(\d+)$""").matchEntire(key)?.groupValues?.get(1)?.toInt() }

    for (number in variantNumbers) {
        val name = params["name$number"]?.let(::cleanWikiText) ?: continue
        if (normalizeTitle(name) == requested) {
            return ItemIdentity(params["id$number"]?.toIntOrNull(), name)
        }
    }

    val defaultVariant = params["defver"]?.trim()?.toIntOrNull()
    val name = params["name"]?.let(::cleanWikiText)
        ?: defaultVariant?.let { params["name$it"]?.let(::cleanWikiText) }
        ?: params["name1"]?.let(::cleanWikiText)
    val id = params["id"]?.toIntOrNull()
        ?: defaultVariant?.let { params["id$it"]?.toIntOrNull() }
        ?: params["id1"]?.toIntOrNull()
    return name?.let { ItemIdentity(id, it) }
}

internal fun parseRecipeRequirements(wikitext: String, targetName: String): List<RecipeRequirement> {
    val creation = extractCreationSection(wikitext) ?: return emptyList()

    return findTemplates(creation, "Recipe")
        .mapIndexed { index, recipe ->
            val params = parseTemplateParams(recipe)
            val outputs = params.entries
                .filter { (key, value) -> key.matches(Regex("""output\d+""")) && value.isNotBlank() }
                .map { (_, value) -> normalizeTitle(value) }

            if (outputs.isNotEmpty() && normalizeTitle(targetName) !in outputs) {
                return@mapIndexed null
            }

            val items = params
                .entries
                .filter { (key, value) -> key.matches(Regex("""mat\d+""")) && value.isNotBlank() }
                .sortedBy { (key, _) -> key.removePrefix("mat").toInt() }
                .mapNotNull { (key, value) ->
                    val name = cleanWikiText(value).takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val number = key.removePrefix("mat")
                    IngredientRequirement(
                        name = name,
                        quantity = params["mat${number}quantity"]?.trim()?.toIntOrNull() ?: 1
                    )
                }

            val method = recipeMethodName(params, targetName, index + 1)

            if (items.isEmpty()) {
                null
            } else {
                RecipeRequirement(
                    method = method,
                    xp = parseSkillXp(params, method),
                    items = items
                )
            }
        }
        .filterNotNull()
}

private fun parseSkillXp(params: Map<String, String>, method: String): List<SkillXp> {
    return params.keys
        .mapNotNull { key -> Regex("""^skill(\d+)$""").matchEntire(key)?.groupValues?.get(1)?.toInt() }
        .distinct()
        .sorted()
        .mapNotNull { number ->
            val skill = params["skill$number"]
                ?.let(::cleanWikiText)
                ?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            val rawAmount = params["skill${number}exp"]
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            val amount = rawAmount.replace(",", "").toDoubleOrNull()

            if (amount == null) {
                System.err.println("Warning: skipping XP '$rawAmount' for recipe '$method' because it is not a number")
                null
            } else {
                SkillXp(skill = skill, amount = amount)
            }
        }
}

internal fun warnIfRecipesTargetOtherOutputs(wikitext: String, targetName: String) {
    val creation = extractCreationSection(wikitext) ?: return
    val otherOutputs = findTemplates(creation, "Recipe")
        .flatMap { recipe ->
            parseTemplateParams(recipe)
                .entries
                .filter { (key, value) -> key.matches(Regex("""output\d+""")) && value.isNotBlank() }
                .map { (_, value) -> cleanWikiText(value) }
        }
        .filter { normalizeTitle(it) != normalizeTitle(targetName) }
        .distinct()

    if (otherOutputs.isNotEmpty()) {
        System.err.println(
            "Warning: no recipe matched '$targetName'; this page has recipe output(s): ${otherOutputs.joinToString()}"
        )
    }
}

private fun recipeMethodName(params: Map<String, String>, targetName: String, index: Int): String {
    val matchingOutputNumber = params.entries
        .firstOrNull { (key, value) ->
            key.matches(Regex("""output\d+""")) && normalizeTitle(value) == normalizeTitle(targetName)
        }
        ?.key
        ?.removePrefix("output")

    if (matchingOutputNumber != null) {
        params["output${matchingOutputNumber}subtxt"]
            ?.let(::cleanWikiText)
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }
    }

    params.entries
        .firstOrNull { (key, value) -> key.matches(Regex("""output\d+subtxt""")) && value.isNotBlank() }
        ?.value
        ?.let(::cleanWikiText)
        ?.takeIf { it.isNotBlank() }
        ?.let { return it }

    params["facilities"]?.let(::cleanWikiText)?.takeIf { it.isNotBlank() }?.let { return it }
    params["tools"]?.let(::cleanWikiText)?.takeIf { it.isNotBlank() }?.let { return it }
    params["skill1"]?.let(::cleanWikiText)?.takeIf { it.isNotBlank() }?.let { return it }

    return "Recipe $index"
}

private fun extractCreationSection(wikitext: String): String? {
    val heading = Regex("""(?m)^==\s*Creation\s*==\s*$""").find(wikitext) ?: return null
    val afterHeading = heading.range.last + 1
    val nextHeading = Regex("""(?m)^==[^=].*==\s*$""").find(wikitext, afterHeading)
    val end = nextHeading?.range?.first ?: wikitext.length
    return wikitext.substring(afterHeading, end)
}
