package vintagebadger.trainingplanner.modals

data class TrainingPlan(
    val skill: String = "",
    val startLevel: Int = 0,
    val endLevel: Int = 0,
    val trainingMethod: TrainingMethod = TrainingMethod()
)

data class TrainingMethod(
    val name: String = "",
    val skill: String = "",
    val maxLevel: Int = 0,
    val steps: List<TrainingStep> = emptyList()
)

data class TrainingStep(
    val step: String = "",
    val skill: String = "",
    val xp: Int = 0
)