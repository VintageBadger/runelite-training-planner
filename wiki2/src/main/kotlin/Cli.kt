package vintagebadger.trainingplanner.wiki2

internal data class CliOptions(
    val items: List<String>,
    val outputPath: String?
)

internal fun parseCliOptions(args: Array<String>): CliOptions {
    val items = mutableListOf<String>()
    val positional = mutableListOf<String>()
    var outputPath: String? = System.getenv("OSRS_OUTPUT")?.takeIf { it.isNotBlank() }
    var index = 0

    fun requireValue(option: String): String {
        if (index + 1 >= args.size) {
            error("Missing value for $option")
        }
        index++
        return args[index]
    }

    while (index < args.size) {
        val arg = args[index]
        when {
            arg == "--output" || arg == "-o" -> outputPath = requireValue(arg)
            arg.startsWith("--output=") -> outputPath = arg.substringAfter('=').takeIf { it.isNotBlank() }
            arg == "--item" || arg == "-i" -> items += requireValue(arg)
            arg.startsWith("--item=") -> items += arg.substringAfter('=')
            arg == "--items" -> items += parseItemList(requireValue(arg))
            arg.startsWith("--items=") -> items += parseItemList(arg.substringAfter('='))
            arg.startsWith("-") -> error("Unknown option: $arg")
            else -> positional += arg
        }
        index++
    }

    items += System.getenv("OSRS_ITEMS")?.let(::parseItemList).orEmpty()
    System.getenv("OSRS_ITEM")?.takeIf { it.isNotBlank() }?.let { items += it }

    if (positional.isNotEmpty()) {
        items += parseItemList(positional.joinToString(" "))
    }

    val cleanedItems = items
        .map { it.trim() }
        .filter { it.isNotBlank() }

    return CliOptions(
        items = cleanedItems.ifEmpty { listOf("Cake") }.distinctBy(::normalizeTitle),
        outputPath = outputPath
    )
}

private fun parseItemList(value: String): List<String> {
    return value
        .split(',', ';', '\n')
        .map { it.trim() }
        .filter { it.isNotBlank() }
}
