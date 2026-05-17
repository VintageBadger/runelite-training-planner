package vintagebadger.trainingplanner.wiki

import java.nio.file.Path
import java.nio.file.Paths

data class CliOptions(
    val targets: List<String>,
    val skill: String?,
    val output: Path,
    val cacheDir: Path,
    val refreshCache: Boolean,
    val maxDepth: Int,
) {
    companion object {
        fun parse(args: List<String>): CliOptions {
            val explicitTargets = mutableListOf<String>()
            var skill: String? = null
            var output: Path? = null
            var cacheDir = Paths.get("tools/wiki-recipe-generator/.wiki-cache")
            var refreshCache = false
            var maxDepth = 12

            var i = 0
            while (i < args.size) {
                when (val arg = args[i]) {
                    "--target" -> explicitTargets.add(args.valueAfter(i++, arg))
                    "--skill" -> skill = args.valueAfter(i++, arg)
                    "--output" -> output = Paths.get(args.valueAfter(i++, arg))
                    "--cache-dir" -> cacheDir = Paths.get(args.valueAfter(i++, arg))
                    "--refresh-cache" -> refreshCache = true
                    "--max-depth" -> maxDepth = args.valueAfter(i++, arg).toInt()
                    "--help", "-h" -> printUsageAndExit()
                    else -> error("Unknown argument: $arg")
                }
                i++
            }

            val seen = mutableSetOf<String>()
            val allTargets = explicitTargets
                .map { normalizeTitle(it) }
                .filter { it.isNotEmpty() }
                .filter { seen.add(normalizeIdentity(it)) }

            return CliOptions(
                targets = allTargets,
                skill = skill?.trim()?.takeIf { it.isNotEmpty() },
                output = output ?: error("--output is required."),
                cacheDir = cacheDir,
                refreshCache = refreshCache,
                maxDepth = maxDepth,
            )
        }

        private fun List<String>.valueAfter(index: Int, flag: String): String {
            if (index + 1 >= size) error("$flag requires a value.")
            return this[index + 1]
        }

        private fun printUsageAndExit(): Nothing {
            println(
                """
                Usage:
                  ./gradlew :tools:wiki-recipe-generator:run --args='--skill Herblore --target "Prayer potion" --output src/main/java/vintagebadger/trainingplanner/data/HerbloreMethods.kt'

                Options:
                  --target <title>       Add one wiki item target. Repeatable.
                  --targets <file>       Add newline-separated targets.
                  --skill <skill>        Optional generated method grouping hint.
                  --output <file>        Required Kotlin output path.
                  --cache-dir <dir>      Optional cache directory.
                  --refresh-cache        Refetch pages even when cached.
                  --max-depth <n>        Recursive tracing depth limit. Default: 12.
                """.trimIndent()
            )
            kotlin.system.exitProcess(0)
        }
    }
}
