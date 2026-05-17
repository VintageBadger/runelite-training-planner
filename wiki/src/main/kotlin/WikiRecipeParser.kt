package vintagebadger.trainingplanner.wiki

internal data class ItemIdentity(
    val id: Int?,
    val name: String
)

internal data class RecipeRequirement(
    val method: String,
    val skills: List<SkillRequirement>,
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
                    skills = parseSkillRequirements(params, method),
                    items = items
                )
            }
        }
        .filterNotNull()
}

private fun parseSkillRequirements(params: Map<String, String>, method: String): List<SkillRequirement> {
    val skillNumbers = params.keys
        .mapNotNull { key -> Regex("""^skill\s*(\d+)$""").matchEntire(key)?.groupValues?.get(1)?.toInt() }
        .plus(if ("skill" in params) listOf(1) else emptyList())
        .distinct()
        .sorted()

    return skillNumbers.mapNotNull { number ->
        val skill = (firstParam(params, "skill$number", "skill $number")
            ?: if (number == 1) firstParam(params, "skill") else null
            )
            ?.let(::cleanWikiText)
            ?.takeIf { it.isNotBlank() }
            ?: return@mapNotNull null
        val level = firstParam(
            params,
            "skill${number}lvl",
            "skill${number}level",
            "skill $number lvl",
            "skill $number level",
        )
            ?: if (number == 1) {
                firstParam(params, "skilllvl", "skilllevel", "skill lvl", "skill level", "level", "level1", "level 1")
            } else {
                null
            }
        val rawXp = firstParam(
            params,
            "skill${number}exp",
            "skill $number exp",
            "skill${number}xp",
            "skill $number xp",
        )
            ?: if (number == 1) {
                firstParam(params, "skillxp", "skill xp", "xp", "xp1", "experience", "experience1", "exp", "exp1")
            } else {
                null
            }
        val xp = rawXp
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.replace(",", "")
            ?.toDoubleOrNull()

        if (rawXp != null && xp == null) {
            System.err.println("Warning: skipping XP '$rawXp' for recipe '$method' because it is not a number")
        }

        xp?.let {
            SkillRequirement(
                skill = skill,
                level = level?.trim()?.replace(",", "")?.toIntOrNull() ?: 0,
                xp = it
            )
        }
    }
}

private fun firstParam(params: Map<String, String>, vararg keys: String): String? {
    return keys.firstNotNullOfOrNull { params[it] }
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
