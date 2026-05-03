package vintagebadger.trainingplanner.models

/**Character based, one skill one training method plan
 * */
data class TrainingPlan(
    val skill: String = "",
    val startLevel: Int = 0,
    val endLevel: Int = 0,
    val trainingMethod: TrainingMethod = TrainingMethod()
)

/**One recipe and its substeps
 * */
data class TrainingMethod(
    val name: String = "",
    val wikiPage: String = "",
    val totalXp: Double = 0.0, //xp for completing all substeps for one recipe
    val skill: String = "", //should match the Skill enum
    val maxLevel: Int = 0, //max level requirement of any step
    val output: List<ItemReference> = emptyList(),
    val steps: List<TrainingStep> = emptyList()
)

/**Low level step
 * */
data class TrainingStep(
    val step: String = "",
    val name: String = "",
    val skill: String = "",
    val level: Int = 0,
    val xp: Int = 0,
    val input: List<ItemReference> = emptyList(),
    val output: List<ItemReference> = emptyList(),
)

/** ID can be used to show the item's image
 * */
data class ItemReference(
    val name: String = "",
    val id: Int = 0,
    val quantity: Int = 0,
)