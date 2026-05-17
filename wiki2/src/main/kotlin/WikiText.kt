package vintagebadger.trainingplanner.wiki2

internal fun findTemplates(text: String, templateName: String): List<String> {
    val results = mutableListOf<String>()
    var index = 0

    while (index < text.length) {
        val start = text.indexOf("{{", index)
        if (start == -1) break

        val nameStart = start + 2
        val nameEnd = findTemplateNameEnd(text, nameStart)
        val foundName = text.substring(nameStart, nameEnd).trim()

        if (foundName.equals(templateName, ignoreCase = true)) {
            val end = findBalancedTemplateEnd(text, start)
            if (end != -1) {
                results += text.substring(start, end)
                index = end
                continue
            }
        }

        index = start + 2
    }

    return results
}

private fun findTemplateNameEnd(text: String, start: Int): Int {
    var index = start
    while (index < text.length && text[index] != '|' && text[index] != '}') {
        index++
    }
    return index
}

private fun findBalancedTemplateEnd(text: String, start: Int): Int {
    var index = start
    var depth = 0

    while (index < text.length - 1) {
        when {
            text[index] == '{' && text[index + 1] == '{' -> {
                depth++
                index += 2
            }

            text[index] == '}' && text[index + 1] == '}' -> {
                depth--
                index += 2
                if (depth == 0) return index
            }

            else -> index++
        }
    }

    return -1
}

internal fun parseTemplateParams(template: String): Map<String, String> {
    val body = template.removePrefix("{{").removeSuffix("}}")
    val parts = splitTopLevel(body, '|')
    return parts.drop(1)
        .mapNotNull { part ->
            val equalsIndex = indexOfTopLevelEquals(part)
            if (equalsIndex == -1) return@mapNotNull null

            val key = part.substring(0, equalsIndex).trim()
            val value = part.substring(equalsIndex + 1).trim()
            if (key.isEmpty()) null else key to value
        }
        .toMap()
}

private fun splitTopLevel(text: String, delimiter: Char): List<String> {
    val parts = mutableListOf<String>()
    var start = 0
    var index = 0
    var templateDepth = 0
    var linkDepth = 0

    while (index < text.length) {
        when {
            text.startsWith("{{", index) -> {
                templateDepth++
                index += 2
            }

            text.startsWith("}}", index) -> {
                if (templateDepth > 0) templateDepth--
                index += 2
            }

            text.startsWith("[[", index) -> {
                linkDepth++
                index += 2
            }

            text.startsWith("]]", index) -> {
                if (linkDepth > 0) linkDepth--
                index += 2
            }

            text[index] == delimiter && templateDepth == 0 && linkDepth == 0 -> {
                parts += text.substring(start, index)
                start = index + 1
                index++
            }

            else -> index++
        }
    }

    parts += text.substring(start)
    return parts
}

private fun indexOfTopLevelEquals(text: String): Int {
    var index = 0
    var templateDepth = 0
    var linkDepth = 0

    while (index < text.length) {
        when {
            text.startsWith("{{", index) -> {
                templateDepth++
                index += 2
            }

            text.startsWith("}}", index) -> {
                if (templateDepth > 0) templateDepth--
                index += 2
            }

            text.startsWith("[[", index) -> {
                linkDepth++
                index += 2
            }

            text.startsWith("]]", index) -> {
                if (linkDepth > 0) linkDepth--
                index += 2
            }

            text[index] == '=' && templateDepth == 0 && linkDepth == 0 -> return index
            else -> index++
        }
    }

    return -1
}

internal fun cleanWikiText(value: String): String {
    var result = value.trim()

    if (result.startsWith("[[") && result.endsWith("]]")) {
        result = result.removePrefix("[[").removeSuffix("]]")
        result = result.substringBefore('|')
        result = result.substringBefore('#')
    }

    return result
        .replace("&nbsp;", " ")
        .trim()
}

internal fun normalizeTitle(value: String): String {
    return cleanWikiText(value)
        .replace('_', ' ')
        .trim()
        .lowercase()
}
