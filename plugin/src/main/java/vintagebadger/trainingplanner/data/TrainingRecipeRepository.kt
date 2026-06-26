package vintagebadger.trainingplanner.data

import com.google.gson.Gson
import org.slf4j.LoggerFactory
import vintagebadger.trainingplanner.models.Skill
import vintagebadger.trainingplanner.wiki.IngredientRef
import vintagebadger.trainingplanner.wiki.OutputItemRecipes
import vintagebadger.trainingplanner.wiki.RecipeGraph
import java.io.InputStreamReader

data class ResolvedRecipeStep(
    val outputId: Int,
    val outputName: String,
    val outputQuantity: Int,
    val recipeMethod: String,
    val skill: String,
    val level: Int,
    val xp: Double,
    val requires: List<IngredientRef> = emptyList(),
    val children: List<ResolvedRecipeStep> = emptyList(),
)

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

    private val recipesById: Map<Int, OutputItemRecipes> by lazy {
        graph.recipes.associateBy { it.id }
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

    fun resolveSteps(output: OutputItemRecipes, skill: Skill, requiredQuantity: Int): ResolvedRecipeStep? {
        return resolveSteps(output, skill, requiredQuantity = requiredQuantity, stack = emptySet())
    }

    private fun resolveSteps(
        output: OutputItemRecipes,
        skill: Skill,
        requiredQuantity: Int,
        stack: Set<Int>,
    ): ResolvedRecipeStep? {
        if (output.id in stack) return null

        val recipe = output.methods.firstOrNull() ?: return null
        val nextStack = stack + output.id

        val children = mutableListOf<ResolvedRecipeStep>()
        val leafIngredients = mutableListOf<IngredientRef>()

        for (ingredient in recipe.requires) {
            val dependency = recipesById[ingredient.id]
            if (dependency == null) {
                leafIngredients.add(ingredient.copy(quantity = ingredient.quantity * requiredQuantity))
            } else {
                val child = resolveSteps(dependency, skill, ingredient.quantity * requiredQuantity, nextStack)
                if (child != null) {
                    children.add(child)
                } else {
                    leafIngredients.add(ingredient.copy(quantity = ingredient.quantity * requiredQuantity))
                }
            }
        }

        val skillRequirement = recipe.skills.firstOrNull {
            it.skill.equals(skill.displayName, ignoreCase = true)
        }

        return ResolvedRecipeStep(
            outputId = output.id,
            outputName = output.name,
            outputQuantity = requiredQuantity,
            recipeMethod = recipe.method,
            skill = skillRequirement?.skill.orEmpty(),
            level = skillRequirement?.level ?: 0,
            xp = skillRequirement?.xp ?: 0.0,
            requires = leafIngredients,
            children = children,
        )
    }

    companion object {
        private const val RESOURCE_PATH = "/vintagebadger/trainingplanner/training-recipes.json"
    }
}
