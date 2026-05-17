package vintagebadger.trainingplanner.models

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
        val json = gson.toJson(value.plans)
        log.debug("Serializing TrainingPlanList: $json")
        return json
    }

    override fun deserialize(s: String): TrainingPlanList {
        log.debug("Deserializing TrainingPlanList: $s")
        if (s.isBlank()) return TrainingPlanList()
        val type = object : TypeToken<List<TrainingPlan>>() {}.type
        return TrainingPlanList(gson.fromJson(s, type))
    }
}