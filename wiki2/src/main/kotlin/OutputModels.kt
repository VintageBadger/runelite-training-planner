package vintagebadger.trainingplanner.wiki2

data class RecipeGraph(
    val version: Int = 1,
    val recipes: List<OutputItemRecipes> = emptyList()
)

data class OutputItemRecipes(
    val id: Int = 0,
    val name: String = "",
    val methods: List<FlatRecipe> = emptyList()
)

data class FlatRecipe(
    val method: String = "",
    val skills: List<SkillRequirement> = emptyList(),
    val requires: List<IngredientRef> = emptyList()
)

data class SkillRequirement(
    val skill: String = "",
    val level: Int = 0,
    val xp: Double = 0.0
)

data class IngredientRef(
    val id: Int = 0,
    val name: String = "",
    val quantity: Int = 0
)
