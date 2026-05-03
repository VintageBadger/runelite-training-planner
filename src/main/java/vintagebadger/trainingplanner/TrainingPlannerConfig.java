package vintagebadger.trainingplanner;

import vintagebadger.trainingplanner.models.TrainingPlanList;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("training-planner")
public interface TrainingPlannerConfig extends Config {

    @ConfigItem(
            keyName = "trainingPlans",
            name = "",
            description = "",
            hidden = true
    )
    default TrainingPlanList getTrainingPlans()
    {
        return new TrainingPlanList();
    }

    @ConfigItem(
            keyName = "trainingPlans",
            name = "",
            description = ""
    )
    void setTrainingPlans(TrainingPlanList list);
}