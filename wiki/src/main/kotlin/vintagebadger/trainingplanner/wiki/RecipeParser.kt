package vintagebadger.trainingplanner.wiki

import vintagebadger.trainingplanner.models.Skill
import kotlin.math.max

class RecipeParser {
    private val wikitext = WikitextParser()

    fun parseRecipes(page: WikiPage): List<ParsedRecipe> {
        if (page.wikitext.isBlank()) return emptyList()
        val templates = wikitext.findTemplates(page.wikitext)
        val tabberVariants = recipeTabberVariants(page.wikitext, templates)

        return templates
            .filter { normalizeTemplateName(it.name) == "recipe" }
            .mapNotNull { template ->
                parseRecipe(page.normalizedTitle, template, tabberVariants[template.start])
            }
    }

    fun parseItemIds(page: WikiPage, fallbackName: String): Map<String, Int> {
        val ids = linkedMapOf<String, Int>()
        val templates = wikitext.findTemplates(page.wikitext)
        templates
            .filter { normalizeTemplateName(it.name).startsWith("infobox") }
            .forEach { template ->
                val named = template.named
                val plainName = normalizeItemName(named["name"] ?: fallbackName)
                parseInt(named["id"])?.let { ids[plainName] = it }
                for (index in 1..20) {
                    val name = normalizeItemName(named["name$index"] ?: named["name $index"] ?: plainName)
                    parseInt(named["id$index"] ?: named["id $index"])?.let { ids[name] = it }
                }
            }
        return ids
    }

    private fun recipeTabberVariants(source: String, templates: List<WikiTemplate>): Map<Int, String> {
        val result = mutableMapOf<Int, String>()
        templates
            .filter { normalizeTemplateName(it.name) == "recipe tabber" }
            .forEach { tabber ->
                tabber.params.forEachIndexed { index, param ->
                    val variantName =
                        topLevelAssignment(param)?.first?.takeIf { it.isNotBlank() } ?: "variant ${index + 1}"
                    wikitext.findTemplates(param, tabber.start).filter {
                        normalizeTemplateName(it.name) == "recipe"
                    }.forEach {
                        result[it.start] = normalizeItemName(variantName)
                    }
                }
            }
        templates
            .filter { normalizeTemplateName(it.name) == "recipe" }
            .forEach { recipe ->
                htmlTabberVariantName(source, recipe.start)?.let {
                    result.putIfAbsent(recipe.start, it)
                }
            }
        return result
    }

    private fun htmlTabberVariantName(source: String, recipeStart: Int): String? {
        val tabberStart = source.lastIndexOf("<tabber>", recipeStart, ignoreCase = true)
        val tabberEnd = source.lastIndexOf("</tabber>", recipeStart, ignoreCase = true)
        if (tabberStart == -1 || tabberEnd > tabberStart) return null

        val marker = max(
            tabberStart + "<tabber>".length,
            source.lastIndexOf("|-|", recipeStart).takeIf { it > tabberStart }?.plus(3) ?: -1,
        )
        if (marker < 0 || marker >= recipeStart) return null
        val between = source.substring(marker, recipeStart)
        return Regex("(?m)([^=\\n|][^=\\n]*)=\\s*$")
            .find(between)
            ?.groupValues
            ?.get(1)
            ?.let(::normalizeItemName)
            ?.takeIf { it.isNotBlank() }
    }

    private fun parseRecipe(sourcePage: String, template: WikiTemplate, variantName: String?): ParsedRecipe? {
        val named = template.named
        val warnings = mutableListOf<String>()

        val skillRequirements = parseSkillRequirements(named, sourcePage, warnings)

        val inputs = parseItemList(named, true, warnings)
        val outputs = parseItemList(named, false, warnings)
        if (outputs.size > 1) warnings += "MULTIPLE_OUTPUTS:$sourcePage"
        variantName?.let { warnings += "RECIPE_TABBER_VARIANT:$sourcePage:$it" }

        if (inputs.isEmpty() && outputs.isEmpty()) return null

        return ParsedRecipe(
            sourcePage = sourcePage,
            variantName = variantName,
            primarySkill = skillRequirements.firstOrNull()?.skill.orEmpty(),
            maxLevel = skillRequirements.maxOfOrNull { it.level } ?: 0,
            xp = skillRequirements.firstOrNull()?.xp ?: 0.0,
            skillRequirements = skillRequirements,
            inputs = inputs,
            outputs = outputs,
            ticks = parseInt(firstNamed(named, "ticks", "tick")),
            notes = firstNamed(named, "notes", "note"),
            members = parseBoolean(firstNamed(named, "members")),
            facilities = firstNamed(named, "facilities", "facility"),
            warnings = warnings,
        )
    }

    private fun parseSkillRequirements(
        named: Map<String, String>,
        sourcePage: String,
        warnings: MutableList<String>,
    ): List<ParsedSkillRequirement> {
        val requirements = mutableListOf<ParsedSkillRequirement>()
        for (index in 1..8) {
            val skillRaw = firstNamed(named, "skill$index", "skill $index")
                ?: if (index == 1) firstNamed(named, "skill") else null
            if (skillRaw == null) continue
            val skill = normalizeItemName(skillRaw)
            val level = parseInt(
                firstNamed(
                    named,
                    "skill${index}lvl",
                    "skill${index}level",
                    "skill $index lvl",
                    "skill $index level",
                ) ?: if (index == 1) firstNamed(named, "level", "level1", "level 1") else null
            ) ?: 0
            val xpRaw = firstNamed(
                named,
                "skill${index}exp",
                "skill $index exp",
                "skill${index}xp",
                "skill $index xp",
            ) ?: if (index == 1) {
                firstNamed(named, "xp", "xp1", "experience", "experience1", "exp", "exp1")
            } else {
                null
            }
            if (xpRaw == null) warnings += "MISSING_XP:$sourcePage"
            if (skill.isNotBlank() && Skill.entries.none { it.displayName.equals(skill, ignoreCase = true) }) {
                warnings += "UNSUPPORTED_SKILL:$skill"
            }
            requirements += ParsedSkillRequirement(skill, level, parseDouble(xpRaw) ?: 0.0)
        }

        if (requirements.isEmpty()) {
            val xpRaw = firstNamed(named, "xp", "xp1", "experience", "experience1", "exp", "exp1")
            if (xpRaw == null) warnings += "MISSING_XP:$sourcePage"
            return listOf(
                ParsedSkillRequirement(
                    "",
                    parseInt(firstNamed(named, "level", "level1", "level 1")) ?: 0,
                    parseDouble(xpRaw) ?: 0.0
                )
            )
        }

        return requirements
    }

    private fun parseItemList(
        named: Map<String, String>,
        inputs: Boolean,
        warnings: MutableList<String>,
    ): List<ParsedItemQuantity> {
        val prefixes = if (inputs) {
            listOf("material", "ingredient", "input", "item", "mat")
        } else {
            listOf("output", "product", "result")
        }
        val containers = if (inputs) {
            listOf("materials", "ingredients", "inputs", "input", "items", "mats")
        } else {
            listOf("outputs", "output", "products", "product", "results", "result")
        }

        val items = mutableListOf<ParsedItemQuantity>()
        containers.mapNotNull { named[it] }.forEach { raw ->
            items += parseNestedItems(raw, warnings)
        }

        for (index in 1..30) {
            val nameKey = prefixes.firstNotNullOfOrNull { prefix ->
                listOf("$prefix$index", "$prefix $index", "${prefix}_${index}").firstOrNull { it in named }
            }
            if (nameKey != null) {
                val quantityKey = listOf(
                    "${nameKey}quantity",
                    "${nameKey} quantity",
                    "${nameKey}_quantity",
                    "${nameKey}qty",
                    "${nameKey} qty",
                    "${nameKey}_qty",
                    "quantity$index",
                    "qty$index",
                ).firstOrNull { it in named }
                val idKey = listOf(
                    "${nameKey}id",
                    "${nameKey} id",
                    "${nameKey}_id",
                    "id$index",
                ).firstOrNull { it in named }
                val noteKey = listOf(
                    "${nameKey}subtxt",
                    "${nameKey} subtxt",
                    "${nameKey}_subtxt",
                    "${nameKey}itemnote",
                    "${nameKey} itemnote",
                    "${nameKey}_itemnote",
                    "output${index}subtxt",
                    "output${index}itemnote",
                ).firstOrNull { it in named }
                items += parsedItem(
                    rawName = named.getValue(nameKey),
                    rawQuantity = quantityKey?.let { named[it] },
                    rawId = idKey?.let { named[it] },
                    rawNote = noteKey?.let { named[it] },
                    warnings = warnings,
                )
            }
        }

        return items
            .filter { it.name.isNotBlank() }
            .distinctBy { normalizeIdentity(it.name) to it.quantity }
    }

    private fun parseNestedItems(raw: String, warnings: MutableList<String>): List<ParsedItemQuantity> {
        val templates = wikitext.findTemplates(raw)
        val nested = templates.mapNotNull { template ->
            val templateName = normalizeTemplateName(template.name)
            if (templateName in itemTemplateNames) {
                val itemName = firstNamed(template.named, "name", "item", "link", "page")
                    ?: template.params.getOrNull(0)
                val quantity = firstNamed(template.named, "quantity", "qty", "amount")
                    ?: template.params.getOrNull(1)
                val id = firstNamed(template.named, "id", "itemid", "item id")
                parsedItem(itemName.orEmpty(), quantity, id, null, warnings)
            } else {
                null
            }
        }
        if (nested.isNotEmpty()) return nested

        return raw.split(Regex("\\n|<br\\s*/?>", RegexOption.IGNORE_CASE))
            .map { it.trim('*', ' ', '\t') }
            .filter { it.isNotBlank() }
            .map { line ->
                val quantityMatch = Regex("^(\\d+)\\s*[x×]\\s*(.+)$").find(stripMarkup(line))
                if (quantityMatch != null) {
                    parsedItem(quantityMatch.groupValues[2], quantityMatch.groupValues[1], null, null, warnings)
                } else {
                    parsedItem(line, null, null, null, warnings)
                }
            }
    }

    private fun parsedItem(
        rawName: String,
        rawQuantity: String?,
        rawId: String?,
        rawNote: String?,
        warnings: MutableList<String>,
    ): ParsedItemQuantity {
        val name = normalizeItemName(rawName)
        val quantity = parseQuantity(name, rawQuantity, warnings)
        val id = parseInt(rawId) ?: 0
        return ParsedItemQuantity(name, id, quantity, rawNote?.let(::normalizeItemName))
    }

    private fun parseQuantity(itemName: String, raw: String?, warnings: MutableList<String>): Int {
        val value = stripMarkup(raw.orEmpty()).trim()
        if (value.isBlank()) return 1
        val integer = parseInt(value)
        if (integer != null && integer.toString() == value.replace(",", "")) return integer
        warnings += "UNPARSED_QUANTITY:$itemName:$value"
        return 1
    }

    companion object {
        private val itemTemplateNames = setOf(
            "itemlist",
            "item list",
            "itemquantity",
            "item quantity",
            "plink",
            "ilink",
            "item",
        )
    }
}

data class ParsedRecipe(
    val sourcePage: String,
    val variantName: String?,
    val primarySkill: String,
    val maxLevel: Int,
    val xp: Double,
    val skillRequirements: List<ParsedSkillRequirement>,
    val inputs: List<ParsedItemQuantity>,
    val outputs: List<ParsedItemQuantity>,
    val ticks: Int?,
    val notes: String?,
    val members: Boolean?,
    val facilities: String?,
    val warnings: List<String>,
)

data class ParsedSkillRequirement(
    val skill: String,
    val level: Int,
    val xp: Double,
)

data class ParsedItemQuantity(
    val name: String,
    val itemId: Int,
    val quantity: Int,
    val note: String? = null,
)
