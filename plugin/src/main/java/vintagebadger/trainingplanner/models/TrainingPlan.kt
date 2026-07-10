package vintagebadger.trainingplanner.models

/** Character based, one skill, one selected training recipe plan. */
data class TrainingPlan(
    val skill: String = "",
    val startLevel: Int = 0,
    val endLevel: Int = 0,
    val startXp: Long = 0,
    val targetXp: Long = 0,
    val rootRecipeId: Int = 0,
    val displayNameOverride: String? = null,
    val methodSelections: Map<Int, String> = emptyMap(),
    val ownedQuantities: Map<Int, Long> = emptyMap(),
) {
    val xpRequired: Long
        get() = maxOf(0L, targetXp - startXp)
}
