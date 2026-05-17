data class RecipeGraph(
    val version: Int = 1,
    val recipes: List<OutputItemRecipes>
)

data class OutputItemRecipes(
    val id: Int,
    val name: String,
    val methods: List<FlatRecipe>
)

data class FlatRecipe(
    val method: String,
    val skills: List<SkillRequirement>,
    val requires: List<IngredientRef>
)

data class SkillRequirement(
    val skill: String,
    val level: Int,
    val xp: Double
)

data class IngredientRef(
    val id: Int,
    val name: String,
    val quantity: Int
)
