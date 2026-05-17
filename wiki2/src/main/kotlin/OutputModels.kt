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
    val xp: List<SkillXp>,
    val requires: List<IngredientRef>
)

data class SkillXp(
    val skill: String,
    val amount: Double
)

data class IngredientRef(
    val id: Int,
    val name: String,
    val quantity: Int
)
