package vintagebadger.trainingplanner

import com.google.gson.Gson
import net.runelite.api.Client
import net.runelite.client.game.ItemManager
import net.runelite.client.ui.ColorScheme
import net.runelite.client.ui.DynamicGridLayout
import net.runelite.client.ui.PluginPanel
import vintagebadger.trainingplanner.components.TrainingMethodList
import vintagebadger.trainingplanner.components.TrainingPlanCard
import vintagebadger.trainingplanner.data.TrainingRecipeRepository
import vintagebadger.trainingplanner.models.Skill
import vintagebadger.trainingplanner.models.TrainingPlan
import vintagebadger.trainingplanner.models.TrainingPlanList
import vintagebadger.trainingplanner.wiki.OutputItemRecipes
import java.awt.BorderLayout
import javax.swing.DefaultListCellRenderer
import javax.swing.JComboBox
import javax.swing.JList
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTabbedPane
import java.awt.Component

class TrainingPlannerPanel(
    client: Client,
    private val config: TrainingPlannerConfig,
    private val itemManager: ItemManager,
    gson: Gson,
) : PluginPanel() {

    private val calculatorUi = LevelCalculatorUi()
    private val methodList = TrainingMethodList(itemManager, ::tryAutoSave)
    private val recipeRepository = TrainingRecipeRepository(gson)

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
            addActionListener { onExpRequiredChanged(calculatorUi.expRequired) }
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

        return JPanel(BorderLayout()).apply {
            add(panel, BorderLayout.NORTH)
            background = ColorScheme.DARK_GRAY_COLOR
        }
    }

    private fun buildSavedPlansTab(): JPanel {
        val wrapper = JPanel().apply {
            layout = javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)
            background = ColorScheme.DARK_GRAY_COLOR
        }
        savedPlansPanel = wrapper
        refreshSavedPlans()
        return JPanel(BorderLayout()).apply {
            add(savedPlansPanel, BorderLayout.NORTH)
            background = ColorScheme.DARK_GRAY_COLOR
        }
    }

    private fun onExpRequiredChanged(exp: Int?) {
        val skill = skillDropdown.selectedItem as? Skill ?: return
        val maxLevel = calculatorUi.getStartLevel() ?: return
        val methods = loadMethodsForSkill(skill, maxLevel)
        methodList.setMethods(methods, skill, exp)
        tryAutoSave()
    }

    private fun loadMethodsForSkill(skill: Skill, maxLevel: Int): List<OutputItemRecipes> =
        recipeRepository.methodsFor(skill, maxLevel)

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
                existing.trainingMethod.id == plan.trainingMethod.id
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
                            it.trainingMethod.id == plan.trainingMethod.id
                }
                val card = TrainingPlanCard(plan, originalIndex, config, itemManager, recipeRepository, ::onPlanChanged)
                card.alignmentX = CENTER_ALIGNMENT
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
