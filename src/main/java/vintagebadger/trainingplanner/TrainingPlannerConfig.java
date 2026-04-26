package vintagebadger.trainingplanner;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigItem;

public interface TrainingPlannerConfig extends Config {
    @ConfigItem(
            keyName = "greeting",
            name = "Welcome Greeting",
            description = "The message to show to the user when they login"
    )
    default String greeting()
    {
        return "Hello";
    }
}