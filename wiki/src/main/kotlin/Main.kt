package vintagebadger.trainingplanner.wiki

import com.google.gson.GsonBuilder
import java.nio.file.Files
import java.nio.file.Path

fun main(args: Array<String>) {
    val options = parseCliOptions(args)
    val graph = RecipeGraphBuilder().build(options.items)
    val gson = GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create()
    val json = gson.toJson(graph)

    if (options.outputPath == null) {
        println(json)
    } else {
        val path = Path.of(options.outputPath)
        path.parent?.let(Files::createDirectories)
        Files.writeString(path, json)
        System.err.println("Wrote ${graph.recipes.size} recipe item(s) to $path")
    }
}
