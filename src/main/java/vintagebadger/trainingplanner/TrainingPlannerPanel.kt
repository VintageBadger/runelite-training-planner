package vintagebadger.trainingplanner

import net.runelite.api.Client
import net.runelite.client.game.ItemManager
import net.runelite.client.ui.ColorScheme
import net.runelite.client.ui.DynamicGridLayout
import net.runelite.client.ui.PluginPanel
import vintagebadger.trainingplanner.components.TrainingMethodList
import vintagebadger.trainingplanner.components.TrainingPlanCard
import vintagebadger.trainingplanner.data.CookingMethods
import vintagebadger.trainingplanner.data.CraftingMethods
import vintagebadger.trainingplanner.data.HerbloreMethods
import vintagebadger.trainingplanner.data.SmithingMethods
import vintagebadger.trainingplanner.models.Skill
import vintagebadger.trainingplanner.models.TrainingMethod
import vintagebadger.trainingplanner.models.TrainingPlan
import vintagebadger.trainingplanner.models.TrainingPlanList
import java.awt.BorderLayout
import javax.swing.DefaultListCellRenderer
import javax.swing.JComboBox
import javax.swing.JList
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTabbedPane
import java.awt.Component
import java.awt.Dimension

class TrainingPlannerPanel(
    client: Client,
    private val config: TrainingPlannerConfig,
    private val itemManager: ItemManager,
) : PluginPanel() {

    private val calculatorUi = LevelCalculatorUi()
    private val methodList = TrainingMethodList(itemManager, ::tryAutoSave)

    private lateinit var savedPlansPanel: JPanel
    private lateinit var skillDropdown: JComboBox<Skill?>

    private var lastSavedPlanIndex: Int? = null

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

        skillDropdown = JComboBox<Skill?>(Skill.entries.toTypedArray()).apply {
            selectedItem = null
            renderer = object : DefaultListCellRenderer() {
                override fun getListCellRendererComponent(
                    list: JList<*>?, value: Any?, index: Int,
                    isSelected: Boolean, cellHasFocus: Boolean
                ): Component {
                    val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                    text = when {
                        value == null -> "Select a Skill"
                        value is Skill -> value.displayName
                        else -> value.toString()
                    }
                    return component
                }
            }
            addActionListener { onSkillChanged() }
        }
        panel.add(skillDropdown)

        panel.add(calculatorUi)

        calculatorUi.onExpRequiredChanged = { exp ->
            onExpRequiredChanged(exp)
        }

        panel.add(JLabel("Select a Training Method").apply {
            font = net.runelite.client.ui.FontManager.getRunescapeBoldFont()
            foreground = java.awt.Color.WHITE
        })
        panel.add(methodList)

        return panel
    }

    private fun buildSavedPlansTab(): JPanel {
        val wrapper = JPanel().apply {
            layout = javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)
            background = ColorScheme.DARK_GRAY_COLOR
        }
        savedPlansPanel = wrapper
        refreshSavedPlans()
        return JPanel(BorderLayout()).apply{
            add(savedPlansPanel, BorderLayout.CENTER)
            background = ColorScheme.DARK_GRAY_COLOR
        }
    }

    private fun onSkillChanged() {
        val skill = skillDropdown.selectedItem as? Skill ?: return
        val methods = loadMethodsForSkill(skill)
        methodList.setMethods(methods, calculatorUi.expRequired)
        tryAutoSave()
    }

    private fun onExpRequiredChanged(exp: Int?) {
        val skill = skillDropdown.selectedItem as? Skill ?: return
        val methods = loadMethodsForSkill(skill)
        methodList.setMethods(methods, exp)
        tryAutoSave()
    }

    private fun loadMethodsForSkill(skill: Skill): List<TrainingMethod> {
        return when (skill) {
            Skill.HERBLORE -> HerbloreMethods.methods
            Skill.SMITHING -> SmithingMethods.methods
            Skill.COOKING -> CookingMethods.methods
            Skill.CRAFTING -> CraftingMethods.methods
        }
    }

    private fun tryAutoSave() {
        val selectedSkill = skillDropdown.selectedItem as? Skill ?: return
        val startLevel = calculatorUi.getStartLevel() ?: return
        val endLevel = calculatorUi.getEndLevel() ?: return
        val selectedMethod = methodList.getSelectedMethod() ?: return

        val plan = TrainingPlan(
            skill = selectedSkill.name,
            startLevel = startLevel,
            endLevel = endLevel,
            trainingMethod = selectedMethod
        )

        val currentPlans = config.getTrainingPlans().plans.toMutableList()

        if (lastSavedPlanIndex != null && lastSavedPlanIndex!! in currentPlans.indices) {
            val existing = currentPlans[lastSavedPlanIndex!!]
            if (existing.skill == plan.skill &&
                existing.startLevel == plan.startLevel &&
                existing.endLevel == plan.endLevel &&
                existing.trainingMethod.name == plan.trainingMethod.name
            ) {
                return
            }
            currentPlans[lastSavedPlanIndex!!] = plan
        } else {
            currentPlans.add(plan)
            lastSavedPlanIndex = currentPlans.lastIndex
        }

        config.setTrainingPlans(TrainingPlanList(currentPlans))
        refreshSavedPlans()
    }

    fun refreshSavedPlans() {
        savedPlansPanel.removeAll()

        val plans = config.trainingPlans.plans
            .sortedWith(compareBy({ it.skill }, { it.startLevel }))

        if (plans.isEmpty()) {
            savedPlansPanel.add(JLabel("No plans yet").apply {
                alignmentX = CENTER_ALIGNMENT
            })
        } else {
            val unsortedPlans = config.trainingPlans.plans
            plans.forEach { plan ->
                val originalIndex = unsortedPlans.indexOfFirst {
                    it.skill == plan.skill &&
                        it.startLevel == plan.startLevel &&
                        it.endLevel == plan.endLevel &&
                        it.trainingMethod.name == plan.trainingMethod.name
                }
                val card = TrainingPlanCard(plan, originalIndex, config, itemManager, ::onPlanChanged)
                card.alignmentX = CENTER_ALIGNMENT
                card.maximumSize = Dimension(Int.MAX_VALUE, Int.MAX_VALUE)
                savedPlansPanel.add(card)
            }
        }

        savedPlansPanel.revalidate()
        savedPlansPanel.repaint()
    }

    private fun onPlanChanged() {
        lastSavedPlanIndex = null
        refreshSavedPlans()
    }
}
