package vintagebadger.trainingplanner;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class LocalDevTest {
    public static void main(String[] args) throws Exception {
        ExternalPluginManager.loadBuiltin(TrainingPlannerPlugin.class);
        RuneLite.main(args);
    }
}
