package vintagebadger.trainingplanner.wiki

import com.google.gson.GsonBuilder
import java.nio.file.Files
import java.nio.file.Path

fun main(args: Array<String>) {
    val options = parseCliOptions(args)
    WikiLog.log.debug("Parsed CLI options: {} requested item(s), output={}", options.items.size, options.outputPath)
    val graph = RecipeGraphBuilder().build(options.items)
    WikiLog.log.debug("Serializing {} recipe item(s) to JSON", graph.recipes.size)
    val gson = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
    val json = gson.toJson(graph)

    if (options.outputPath == null) {
        println(json)
    } else {
        val path = Path.of(options.outputPath)
        WikiLog.log.debug("Writing recipe JSON to '{}'", path)
        path.parent?.let(Files::createDirectories)
        Files.writeString(path, json)
        System.err.println("Wrote ${graph.recipes.size} recipe item(s) to $path")
    }
}
