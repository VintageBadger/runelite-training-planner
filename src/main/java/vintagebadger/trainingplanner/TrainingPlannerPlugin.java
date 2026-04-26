package vintagebadger.trainingplanner;

import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;

@Slf4j
@PluginDescriptor(
        name = "Training Planner"
)
public class TrainingPlannerPlugin  extends Plugin{

    @Inject
    private Client client;

    @Inject
    private TrainingPlannerConfig config;

    @Override
    protected void startUp() throws Exception
    {
        log.debug("Training Planner started!");
    }

    @Override
    protected void shutDown() throws Exception
    {
        log.debug("Training Planner stopped!");
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged)
    {
        if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
        {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Training Planner says " + config.greeting(), null);
        }
    }

    @Provides
    TrainingPlannerConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(TrainingPlannerConfig.class);
    }
}
