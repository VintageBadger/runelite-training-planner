package vintagebadger.trainingplanner.wiki

fun main(args: Array<String>) {
    val options = CliOptions.parse(args.toList())
    if (options.targets.isEmpty()) {
        error("At least one --target or --targets entry is required.")
    }

    val client = WikiClient(options.cacheDir, options.refreshCache)
    val parser = RecipeParser()
    val resolver = RecipeResolver(client, parser, options.maxDepth)
    val methods = options.targets.map { resolver.resolveMethod(it, options.skill) }

    KotlinEmitter().write(options.output, methods)
    println("Wrote ${methods.size} method(s) to ${options.output}")
}
