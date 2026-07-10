package vintagebadger.trainingplanner.wiki

data class RecipeGraph(
    val version: Int = 2,
    val recipes: List<OutputItemRecipes> = emptyList()
)

data class OutputItemRecipes(
    val id: Int = 0,
    val name: String = "",
    val methods: List<FlatRecipe> = emptyList()
)

data class FlatRecipe(
    val methodKey: String? = null,
    val method: String = "",
    val outputQuantity: Int = 1,
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
