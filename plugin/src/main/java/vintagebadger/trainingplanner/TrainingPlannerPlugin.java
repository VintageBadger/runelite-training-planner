package vintagebadger.trainingplanner;

import com.google.gson.Gson;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;
import vintagebadger.trainingplanner.data.OwnedQuantitySnapshotService;

import javax.inject.Inject;
import java.awt.image.BufferedImage;

@Slf4j
@PluginDescriptor(
        name = "Training Planner"
)
public class TrainingPlannerPlugin  extends Plugin{
    private static final BufferedImage ICON = ImageUtil.loadImageResource(TrainingPlannerPlugin.class, "training-planner.png");

    @Inject
    private Client client;
    @Inject
    private ClientToolbar clientToolbar;
    @Inject
    private ItemManager itemManager;
    @Inject
    private Gson gson;
    @Inject
    private TrainingPlannerConfig config;
    @Inject
    private OwnedQuantitySnapshotService snapshotService;

    private NavigationButton navButton;
    private TrainingPlannerPanel panel;

    @Override
    protected void startUp() throws Exception
    {
        log.debug("Training Planner started!");
        panel = new TrainingPlannerPanel(config, itemManager, gson, snapshotService);
        navButton = NavigationButton.builder()
                .tooltip("Training Planner")
                .icon(ICON)
                .priority(1000)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);
    }

    @Override
    protected void shutDown() throws Exception
    {
        log.debug("Training Planner stopped!");
        clientToolbar.removeNavigation(navButton);
        if (panel != null)
        {
            panel.shutDown();
        }
        panel = null;
        navButton = null;
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged)
    {
        if (gameStateChanged.getGameState() == GameState.LOGIN_SCREEN)
        {
            snapshotService.clearSnapshots();
        }
        if (gameStateChanged.getGameState() == GameState.LOGGED_IN)
        {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Training Planner plugin started.", null);
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        snapshotService.onItemContainerChanged(event);
    }

    @Provides
    TrainingPlannerConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(TrainingPlannerConfig.class);
    }
}
