package vintagebadger.trainingplanner.wiki

class WikitextParser {
    fun findTemplates(text: String, baseOffset: Int = 0): List<WikiTemplate> {
        val result = mutableListOf<WikiTemplate>()
        var index = 0
        while (index < text.length - 1) {
            if (text[index] == '{' && text[index + 1] == '{') {
                val end = findTemplateEnd(text, index)
                if (end != -1) {
                    val raw = text.substring(index, end + 2)
                    val content = text.substring(index + 2, end)
                    val parsed = parseTemplate(raw, content, baseOffset + index, baseOffset + end + 2)
                    result += parsed
                    result += findTemplates(content, baseOffset + index + 2)
                    index = end + 2
                    continue
                }
            }
            index++
        }
        return result
    }

    private fun findTemplateEnd(text: String, start: Int): Int {
        var depth = 0
        var linkDepth = 0
        var index = start
        while (index < text.length - 1) {
            when {
                text[index] == '[' && text[index + 1] == '[' -> {
                    linkDepth++
                    index += 2
                    continue
                }

                text[index] == ']' && text[index + 1] == ']' && linkDepth > 0 -> {
                    linkDepth--
                    index += 2
                    continue
                }

                linkDepth == 0 && text[index] == '{' && text[index + 1] == '{' -> {
                    depth++
                    index += 2
                    continue
                }

                linkDepth == 0 && text[index] == '}' && text[index + 1] == '}' -> {
                    depth--
                    if (depth == 0) return index
                    index += 2
                    continue
                }
            }
            index++
        }
        return -1
    }

    private fun parseTemplate(raw: String, content: String, start: Int, end: Int): WikiTemplate {
        val parts = splitTopLevel(content, '|')
        val name = parts.firstOrNull()?.trim().orEmpty()
        val params = mutableListOf<String>()
        val named = linkedMapOf<String, String>()
        parts.drop(1).forEach { part ->
            val assignment = topLevelAssignment(part)
            if (assignment == null) {
                params += part.trim()
            } else {
                named[normalizeParamName(assignment.first)] = assignment.second.trim()
            }
        }
        return WikiTemplate(raw, name, params, named, start, end)
    }
}

data class WikiTemplate(
    val raw: String,
    val name: String,
    val params: List<String>,
    val named: Map<String, String>,
    val start: Int,
    val end: Int,
)

fun splitTopLevel(text: String, delimiter: Char): List<String> {
    val parts = mutableListOf<String>()
    var templateDepth = 0
    var linkDepth = 0
    var start = 0
    var index = 0
    while (index < text.length) {
        if (index < text.length - 1) {
            when {
                text[index] == '[' && text[index + 1] == '[' -> {
                    linkDepth++
                    index += 2
                    continue
                }

                text[index] == ']' && text[index + 1] == ']' && linkDepth > 0 -> {
                    linkDepth--
                    index += 2
                    continue
                }

                linkDepth == 0 && text[index] == '{' && text[index + 1] == '{' -> {
                    templateDepth++
                    index += 2
                    continue
                }

                linkDepth == 0 && text[index] == '}' && text[index + 1] == '}' && templateDepth > 0 -> {
                    templateDepth--
                    index += 2
                    continue
                }
            }
        }
        if (text[index] == delimiter && templateDepth == 0 && linkDepth == 0) {
            parts += text.substring(start, index)
            start = index + 1
        }
        index++
    }
    parts += text.substring(start)
    return parts
}

fun topLevelAssignment(text: String): Pair<String, String>? {
    val parts = splitTopLevel(text, '=')
    if (parts.size < 2) return null
    return parts.first().trim() to parts.drop(1).joinToString("=").trim()
}
