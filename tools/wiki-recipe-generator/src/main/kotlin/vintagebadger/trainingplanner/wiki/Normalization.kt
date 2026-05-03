package vintagebadger.trainingplanner.wiki

fun firstNamed(named: Map<String, String>, vararg keys: String): String? {
    return keys.firstNotNullOfOrNull { named[normalizeParamName(it)] }
}

fun normalizeParamName(name: String): String {
    return stripMarkup(name).trim().lowercase().replace("_", " ").replace(Regex("\\s+"), " ")
}

fun normalizeTemplateName(name: String): String {
    return name.trim().removePrefix("Template:").lowercase().replace("_", " ").replace(Regex("\\s+"), " ")
}

fun normalizeTitle(title: String): String {
    val cleaned = normalizeItemName(title).replace('_', ' ').replace(Regex("\\s+"), " ").trim()
    return cleaned.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
}

fun normalizeIdentity(value: String): String {
    return normalizeItemName(value).replace('_', ' ').replace(Regex("\\s+"), " ").trim().lowercase()
}

fun normalizeItemName(raw: String): String {
    return stripMarkup(raw)
        .replace("&nbsp;", " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

fun stripMarkup(raw: String): String {
    var value = raw.trim()
    value = value.replace(Regex("<!--.*?-->", setOf(RegexOption.DOT_MATCHES_ALL)), "")
    value = value.replace(Regex("<ref[^>/]*/>", RegexOption.IGNORE_CASE), "")
    value = value.replace(Regex("<ref[^>]*>.*?</ref>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
    value = value.replace(Regex("'''?"), "")
    value = value.replace(Regex("\\[\\[([^]|#]+)(?:#[^]|]*)?(?:\\|[^]]*)?]]")) { match ->
        match.groupValues[1].trim()
    }
    value = value.replace(Regex("\\[https?://[^ ]+ ([^]]+)]")) { match -> match.groupValues[1] }
    return value.trim()
}

fun parseInt(raw: String?): Int? {
    val cleaned = stripMarkup(raw.orEmpty()).replace(",", "").trim()
    return Regex("^\\d+$").find(cleaned)?.value?.toIntOrNull()
}

fun parseDouble(raw: String?): Double? {
    val cleaned = stripMarkup(raw.orEmpty()).replace(",", "").trim()
    return Regex("^\\d+(?:\\.\\d+)?$").find(cleaned)?.value?.toDoubleOrNull()
}

fun parseBoolean(raw: String?): Boolean? {
    return when (stripMarkup(raw.orEmpty()).trim().lowercase()) {
        "yes", "true", "1" -> true
        "no", "false", "0" -> false
        else -> null
    }
}

fun formatItem(item: ItemRef): String {
    return if (item.quantity == 1) item.name else "${item.name} x${item.quantity}"
}
