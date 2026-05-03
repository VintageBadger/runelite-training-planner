package vintagebadger.trainingplanner;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.ui.PluginPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

@Slf4j
public class TrainingPlannerPanel extends PluginPanel {
    public TrainingPlannerPanel(Client client){
        super();
        setBorder(new EmptyBorder(10, 10, 10, 10));
        setLayout(new GridBagLayout());

        final JComboBox<String> dropdown = new JComboBox<>();
        dropdown.setMaximumRowCount(3);
        dropdown.addItem("herby");
        dropdown.addItem("smithy");
        dropdown.addItem("cooky");

        // create the screen
        final GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        c.gridx = 0;
        c.gridy = 0;

        add(dropdown, c);
    }
}
