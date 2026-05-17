package vintagebadger.trainingplanner.models

import vintagebadger.trainingplanner.wiki.OutputItemRecipes

/**Character based, one skill one training method plan
 * */
data class TrainingPlan(
    val skill: String = "",
    val startLevel: Int = 0,
    val endLevel: Int = 0,
    val trainingMethod: OutputItemRecipes = OutputItemRecipes()
)
