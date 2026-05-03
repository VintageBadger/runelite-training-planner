package vintagebadger.trainingplanner

import lombok.extern.slf4j.Slf4j
import net.runelite.api.Client
import net.runelite.client.ui.PluginPanel
import javax.swing.JComboBox
import javax.swing.JLabel

@Slf4j
class TrainingPlannerPanel(client: Client) : PluginPanel() {
    val calculatorUi = LevelCalculatorUi()

    /*
    RuneLite applies sane defaults since we're extending `PluginPanel` and not a generic `JPanel`.
    Default lay seems to be a vertical list by default, with a border of 6 pixels for spacing.

    Calling "add", adds a swing element to the ui, following whatever layout was set for this pannel.
    */
    init {
        add(JLabel("Training Planner"))
        add(JLabel("Add a plan"))

        // for now, these are just a list of strings, but they can be any object
        // in the future we will probably make this a data class to store extra information
        val skills = arrayOf("herby", "smithy", "cooky")
        val dropdown = JComboBox(skills)
        add(dropdown)

        add(calculatorUi)
    }
}
