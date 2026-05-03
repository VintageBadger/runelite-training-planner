package vintagebadger.trainingplanner.models

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.inject.Inject
import net.runelite.client.config.ConfigSerializer
import net.runelite.client.config.Serializer

@ConfigSerializer(TrainingPlanListSerializer::class)
data class TrainingPlanList(
    val plans: List<TrainingPlan> = emptyList()
)

class TrainingPlanListSerializer : Serializer<TrainingPlanList> {

    @Inject
    private lateinit var gson: Gson

    override fun serialize(value: TrainingPlanList): String = gson.toJson(value.plans)

    override fun deserialize(s: String): TrainingPlanList {
        if (s.isBlank()) return TrainingPlanList()
        val type = object : TypeToken<List<TrainingPlan>>() {}.type
        return TrainingPlanList(gson.fromJson(s, type))
    }
}