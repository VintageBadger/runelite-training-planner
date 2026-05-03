package vintagebadger.trainingplanner

import lombok.extern.slf4j.Slf4j
import net.runelite.api.Client
import net.runelite.client.ui.DynamicGridLayout
import net.runelite.client.ui.PluginPanel
import org.slf4j.LoggerFactory
import vintagebadger.trainingplanner.models.Skill
import vintagebadger.trainingplanner.models.TrainingPlan
import vintagebadger.trainingplanner.models.TrainingPlanList
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTabbedPane
import vintagebadger.trainingplanner.components.TrainingPlanCard

@Slf4j
class TrainingPlannerPanel(
    client: Client,
    private val config: TrainingPlannerConfig,
    ) : PluginPanel() {
    private val log = LoggerFactory.getLogger(TrainingPlannerPanel::class.java)
    val calculatorUi = LevelCalculatorUi()
    private lateinit var savedPlansPanel: JPanel


    init {
        add(JLabel("Training Planner"))

        val tabbedPane = JTabbedPane()
        tabbedPane.addTab("New Plan", buildNewPlanTab())
        tabbedPane.addTab("Saved Plans", buildSavedPlansTab())
        add(tabbedPane)
    }

    private fun buildNewPlanTab(): JPanel {
        val panel = JPanel().apply {
            layout = DynamicGridLayout(0, 1, 0, 3)
        }
        val dropdown = JComboBox(Skill.entries.toTypedArray())
        panel.add(dropdown)

        panel.add(calculatorUi)

        val addButton = JButton("Add Plan").apply {
            addActionListener {
                val selectedSkill = dropdown.selectedItem as? Skill ?: return@addActionListener
                val startLevel = calculatorUi.getStartLevel() ?: return@addActionListener
                val endLevel = calculatorUi.getEndLevel() ?: return@addActionListener

                val newPlan = TrainingPlan(
                    skill = selectedSkill.name,
                    startLevel = startLevel,
                    endLevel = endLevel
                )

                val existingPlans = config.getTrainingPlans().plans
                val updatedPlans = existingPlans + newPlan
                config.setTrainingPlans(TrainingPlanList(updatedPlans))

                // Verify immediate read-back
                refreshSavedPlans()
            }
        }
        panel.add(addButton)
        return panel
    }

    private fun buildSavedPlansTab(): JScrollPane {
        val wrapper = JPanel().apply {
            layout = javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)
            background = net.runelite.client.ui.ColorScheme.DARK_GRAY_COLOR
        }
        savedPlansPanel = wrapper
        refreshSavedPlans()
        return JScrollPane(savedPlansPanel)
    }

    fun refreshSavedPlans() {
        savedPlansPanel.removeAll()

        val plans = config.getTrainingPlans().plans
            .sortedWith(compareBy({ it.skill }, { it.startLevel }))

        if (plans.isEmpty()) {
            savedPlansPanel.add(JLabel("No plans yet").apply {
                alignmentX = CENTER_ALIGNMENT
            })
        } else {
            plans.forEachIndexed { index, plan ->
                savedPlansPanel.add(TrainingPlanCard(plan, index, config, ::refreshSavedPlans))
            }
        }

        savedPlansPanel.revalidate()
        savedPlansPanel.repaint()
    }
}
