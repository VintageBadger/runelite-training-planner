package vintagebadger.trainingplanner.data

import com.google.gson.Gson
import org.slf4j.LoggerFactory
import vintagebadger.trainingplanner.models.Skill
import vintagebadger.trainingplanner.wiki.OutputItemRecipes
import vintagebadger.trainingplanner.wiki.RecipeGraph
import java.io.InputStreamReader

class TrainingRecipeRepository(
    private val gson: Gson,
) {
    private val log = LoggerFactory.getLogger(TrainingRecipeRepository::class.java)

    private val graph: RecipeGraph by lazy {
        val stream = TrainingRecipeRepository::class.java.getResourceAsStream(RESOURCE_PATH)
        if (stream == null) {
            log.debug("Training recipes resource not found: {}", RESOURCE_PATH)
            RecipeGraph()
        } else {
            stream.use {
                InputStreamReader(it, Charsets.UTF_8).use { reader ->
                    gson.fromJson(reader, RecipeGraph::class.java) ?: RecipeGraph()
                }
            }
        }
    }

    fun methodsFor(skill: Skill): List<OutputItemRecipes> {
        return graph.recipes
            .filter { output ->
                val hasSelectedSkill = output.methods.firstOrNull()
                    ?.skills
                    ?.any { it.skill.equals(skill.displayName, ignoreCase = true) }
                hasSelectedSkill == true
            }
            .sortedBy { it.name }
    }

    companion object {
        private const val RESOURCE_PATH = "/vintagebadger/trainingplanner/training-recipes.json"
    }
}
