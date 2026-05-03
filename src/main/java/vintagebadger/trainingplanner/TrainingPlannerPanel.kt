package vintagebadger.trainingplanner

import lombok.extern.slf4j.Slf4j
import net.runelite.api.Client
import net.runelite.client.ui.PluginPanel
import org.slf4j.LoggerFactory
import vintagebadger.trainingplanner.models.Skill
import vintagebadger.trainingplanner.models.TrainingPlan
import vintagebadger.trainingplanner.models.TrainingPlanList
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JLabel

@Slf4j
class TrainingPlannerPanel(
    client: Client,
    config: TrainingPlannerConfig,
    ) : PluginPanel() {
    private val log = LoggerFactory.getLogger(TrainingPlannerPanel::class.java)
    val calculatorUi = LevelCalculatorUi()

    /*
    RuneLite applies sane defaults since we're extending `PluginPanel` and not a generic `JPanel`.
    Default lay seems to be a vertical list by default, with a border of 6 pixels for spacing.

    Calling "add", adds a swing element to the ui, following whatever layout was set for this pannel.
    */
    init {
        add(JLabel("Training Planner"))
        add(JLabel("Add a plan"))

        val dropdown = JComboBox(Skill.entries.toTypedArray())
        add(dropdown)

        add(calculatorUi)

        val addButton = JButton("Add Plan").apply {
            addActionListener {
                val selectedSkill = dropdown.selectedItem as? Skill ?: return@addActionListener
                val startLevel = calculatorUi.getStartLevel() ?: return@addActionListener
                val endLevel = calculatorUi.getEndLevel() ?: return@addActionListener

                //TODO: will redo UI to update TrainingPlans instead of always adding
                val newPlan = TrainingPlan(
                    skill = selectedSkill.name,
                    startLevel = startLevel,
                    endLevel = endLevel
                )

                val existingPlans = config.getTrainingPlans().plans
                val updatedPlans = existingPlans + newPlan
                config.setTrainingPlans(TrainingPlanList(updatedPlans))

                // Verify immediate read-back
                val saved = config.getTrainingPlans()
                log.debug("After save, plans count: ${saved.plans.size}")
            }
        }
        add(addButton)
    }
}
