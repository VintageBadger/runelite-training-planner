package vintagebadger.trainingplanner.models

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.inject.Inject
import net.runelite.client.config.ConfigSerializer
import net.runelite.client.config.Serializer
import org.slf4j.LoggerFactory

@ConfigSerializer(TrainingPlanListSerializer::class)
data class TrainingPlanList(
    val plans: List<TrainingPlan> = emptyList()
)

class TrainingPlanListSerializer : Serializer<TrainingPlanList> {
    private val log = LoggerFactory.getLogger(TrainingPlanListSerializer::class.java)
    @Inject
    private lateinit var gson: Gson

    override fun serialize(value: TrainingPlanList): String {
        val json = TrainingPlanCodec(gson).serialize(value)
        log.debug("Serializing TrainingPlanList: $json")
        return json
    }

    override fun deserialize(s: String): TrainingPlanList {
        log.debug("Deserializing TrainingPlanList: $s")
        if (s.isBlank()) return TrainingPlanList()
        return TrainingPlanCodec(gson).deserialize(s)
    }
}

internal class TrainingPlanCodec(private val gson: Gson) {
    fun serialize(value: TrainingPlanList): String {
        return gson.toJson(
            StoredTrainingPlans(
                plans = value.plans.map(StoredTrainingPlan::fromDomain),
            ),
        )
    }

    fun deserialize(json: String): TrainingPlanList {
        val root = JsonParser().parse(json)
        if (!root.isJsonObject) return TrainingPlanList()
        val stored: StoredTrainingPlans = gson.fromJson(root, StoredTrainingPlans::class.java)
            ?: return TrainingPlanList()
        return TrainingPlanList(stored.plans.orEmpty().map(StoredTrainingPlan::toDomain))
    }

    private data class StoredTrainingPlans(
        val version: Int = CURRENT_VERSION,
        val plans: List<StoredTrainingPlan>? = emptyList(),
    )

    private data class StoredTrainingPlan(
        val skill: String? = null,
        val startLevel: Int = 0,
        val endLevel: Int = 0,
        val startXp: Long = 0,
        val targetXp: Long = 0,
        val rootRecipeId: Int = 0,
        val displayNameOverride: String? = null,
        val methodSelections: Map<Int, String>? = null,
        val ownedQuantities: Map<Int, Long>? = null,
    ) {
        fun toDomain() = TrainingPlan(
            skill = skill.orEmpty(),
            startLevel = startLevel,
            endLevel = endLevel,
            startXp = startXp.coerceAtLeast(0L),
            targetXp = targetXp.coerceAtLeast(0L),
            rootRecipeId = rootRecipeId,
            displayNameOverride = displayNameOverride,
            methodSelections = methodSelections.orEmpty(),
            ownedQuantities = ownedQuantities.orEmpty().mapValues { (_, quantity) -> quantity.coerceAtLeast(0L) },
        )

        companion object {
            fun fromDomain(plan: TrainingPlan) = StoredTrainingPlan(
                skill = plan.skill,
                startLevel = plan.startLevel,
                endLevel = plan.endLevel,
                startXp = plan.startXp,
                targetXp = plan.targetXp,
                rootRecipeId = plan.rootRecipeId,
                displayNameOverride = plan.displayNameOverride,
                methodSelections = plan.methodSelections,
                ownedQuantities = plan.ownedQuantities,
            )
        }
    }

    private companion object {
        const val CURRENT_VERSION = 2
    }
}
