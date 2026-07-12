package vintagebadger.trainingplanner

import com.google.gson.Gson
import net.runelite.client.game.ItemManager
import net.runelite.client.ui.ColorScheme
import net.runelite.client.ui.DynamicGridLayout
import net.runelite.client.ui.PluginPanel
import vintagebadger.trainingplanner.components.OwnedQuantityPanel
import vintagebadger.trainingplanner.components.TrainingMethodList
import vintagebadger.trainingplanner.components.TrainingPlanCard
import vintagebadger.trainingplanner.data.OwnedQuantitySnapshot
import vintagebadger.trainingplanner.data.OwnedQuantitySnapshotService
import vintagebadger.trainingplanner.data.ResolvedRecipeGraph
import vintagebadger.trainingplanner.data.TrainingRecipeRepository
import vintagebadger.trainingplanner.models.Skill
import vintagebadger.trainingplanner.models.TrainingPlan
import vintagebadger.trainingplanner.models.TrainingPlanList
import vintagebadger.trainingplanner.planning.PlanResult
import vintagebadger.trainingplanner.planning.RecipePlanCalculator
import vintagebadger.trainingplanner.wiki.OutputItemRecipes
import java.awt.BorderLayout
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JTabbedPane
import javax.swing.SwingUtilities

class TrainingPlannerPanel(
    private val config: TrainingPlannerConfig,
    private val itemManager: ItemManager,
    gson: Gson,
    private val snapshotService: OwnedQuantitySnapshotService,
) : PluginPanel() {
    private val calculatorUi = LevelCalculatorUi()
    private val recipeRepository = TrainingRecipeRepository(gson)
    private val planCalculator = RecipePlanCalculator()
    private val methodList = TrainingMethodList(itemManager, ::onMethodSelected)
    private val ownedQuantityPanel = OwnedQuantityPanel(itemManager)
    private val saveButton = JButton("Save Plan").apply {
        isEnabled = false
        addActionListener { saveNewPlan() }
    }
    private val tabbedPane = JTabbedPane()

    private lateinit var savedPlansPanel: JPanel
    private lateinit var skillDropdown: JComboBox<Skill?>
    private var availableMethods: List<OutputItemRecipes> = emptyList()
    private var selectedGraph: ResolvedRecipeGraph? = null
    private var currentResults: Map<Int, Result<PlanResult>> = emptyMap()
    private var currentOwnershipSnapshot = OwnedQuantitySnapshot(emptyMap(), includesBank = false)
    private val savedPlanCards = mutableMapOf<Int, TrainingPlanCard>()

    init {
        add(JLabel("Training Planner"))
        tabbedPane.apply {
            addTab("New Plan", buildNewPlanTab())
            addTab("Saved Plans", buildSavedPlansTab())
        }
        add(tabbedPane)
        snapshotService.setListener(::onOwnershipSnapshotChanged)
    }

    private fun buildNewPlanTab(): JPanel {
        val panel = JPanel().apply {
            layout = DynamicGridLayout(0, 1, 0, 3)
            background = ColorScheme.DARK_GRAY_COLOR
        }
        skillDropdown = JComboBox<Skill?>(Skill.entries.toTypedArray()).apply {
            selectedItem = null
            renderer = object : DefaultListCellRenderer() {
                override fun getListCellRendererComponent(
                    list: JList<*>?,
                    value: Any?,
                    index: Int,
                    isSelected: Boolean,
                    cellHasFocus: Boolean,
                ): Component {
                    val component = super.getListCellRendererComponent(
                        list,
                        value,
                        index,
                        isSelected,
                        cellHasFocus,
                    )
                    text = when (value) {
                        null -> "Select a Skill"
                        is Skill -> value.displayName
                        else -> value.toString()
                    }
                    return component
                }
            }
            addActionListener { refreshMethods() }
        }
        panel.add(skillDropdown)
        panel.add(calculatorUi)
        calculatorUi.onTargetChanged = { refreshMethods() }
        panel.add(JLabel("Select a Training Method").apply {
            font = net.runelite.client.ui.FontManager.getRunescapeBoldFont()
            foreground = java.awt.Color.WHITE
        })
        panel.add(methodList)
        panel.add(ownedQuantityPanel)
        panel.add(saveButton)

        return JPanel(BorderLayout()).apply {
            add(panel, BorderLayout.NORTH)
            background = ColorScheme.DARK_GRAY_COLOR
        }
    }

    private fun buildSavedPlansTab(): JPanel {
        savedPlansPanel = JPanel().apply {
            layout = javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS)
            background = ColorScheme.DARK_GRAY_COLOR
        }
        refreshSavedPlans()
        return JPanel(BorderLayout()).apply {
            add(savedPlansPanel, BorderLayout.NORTH)
            background = ColorScheme.DARK_GRAY_COLOR
        }
    }

    private fun refreshMethods() {
        val skill = skillDropdown.selectedItem as? Skill
        val target = calculatorUi.target
        if (skill == null || target == null) {
            availableMethods = emptyList()
            currentResults = emptyMap()
            selectedGraph = null
            methodList.setMethods(emptyList())
            ownedQuantityPanel.clearGraph()
            updateSaveButtonState()
            return
        }

        availableMethods = recipeRepository.methodsFor(skill, target.startLevel)
        methodList.setMethods(availableMethods)
        updateSelectedGraph()
        refreshPlanResults()
        updateSaveButtonState()
    }

    private fun onMethodSelected() {
        updateSelectedGraph()
        refreshPlanResults()
        updateSaveButtonState()
    }

    private fun updateSelectedGraph() {
        val skill = skillDropdown.selectedItem as? Skill ?: return
        val method = methodList.getSelectedMethod() ?: run {
            selectedGraph = null
            ownedQuantityPanel.clearGraph()
            return
        }
        selectedGraph = runCatching {
            recipeRepository.resolveGraph(method, skill)
        }.getOrNull()
        selectedGraph?.let { graph ->
            ownedQuantityPanel.setGraph(graph)
        } ?: ownedQuantityPanel.clearGraph()
    }

    private fun refreshPlanResults() {
        val skill = skillDropdown.selectedItem as? Skill ?: return
        val target = calculatorUi.target ?: return
        val owned = ownedQuantityPanel.getOwnedQuantities()
        val targetTenths = Math.multiplyExact(target.xpRequired, 10L)
        currentResults = availableMethods.associate { output ->
            output.id to runCatching {
                val graph = recipeRepository.resolveGraph(output, skill)
                planCalculator.solve(graph, targetTenths, owned)
            }
        }
        methodList.setPlanResults(currentResults)
    }

    private fun onOwnershipSnapshotChanged(snapshot: OwnedQuantitySnapshot) {
        currentOwnershipSnapshot = snapshot
        ownedQuantityPanel.setSnapshot(snapshot)
        refreshPlanResults()
        savedPlanCards.values.forEach { card ->
            card.updateOwnedQuantities(snapshot.quantities)
        }
        updateSaveButtonState()
    }

    fun shutDown() {
        snapshotService.clearListener()
    }

    private fun buildCurrentPlan(): TrainingPlan? {
        val selectedSkill = skillDropdown.selectedItem as? Skill ?: return null
        val target = calculatorUi.target ?: return null
        if (target.targetXp <= target.startXp) return null
        val selectedMethod = methodList.getSelectedMethod() ?: return null
        if (currentResults[selectedMethod.id]?.isSuccess != true) return null
        return TrainingPlan(
            skill = selectedSkill.name,
            startLevel = target.startLevel,
            endLevel = target.endLevel,
            startXp = target.startXp,
            targetXp = target.targetXp,
            rootRecipeId = selectedMethod.id,
            methodSelections = emptyMap(),
            ownedQuantities = relevantOwnedQuantities(),
        )
    }

    private fun updateSaveButtonState() {
        saveButton.isEnabled = buildCurrentPlan() != null
    }

    private fun saveNewPlan() {
        val plan = buildCurrentPlan() ?: return
        val currentPlans = config.getTrainingPlans().plans.toMutableList()
        currentPlans += plan
        val savedPlanIndex = currentPlans.lastIndex
        config.setTrainingPlans(TrainingPlanList(currentPlans))
        clearNewPlan()
        refreshSavedPlans(expandedPlanIndex = savedPlanIndex)
        tabbedPane.selectedIndex = SAVED_PLANS_TAB_INDEX
    }

    private fun clearNewPlan() {
        methodList.clearSelection()
        selectedGraph = null
        availableMethods = emptyList()
        currentResults = emptyMap()
        ownedQuantityPanel.clearGraph()
        skillDropdown.selectedItem = null
        calculatorUi.clear()
        updateSaveButtonState()
    }

    fun refreshSavedPlans(expandedPlanIndex: Int? = null) {
        savedPlansPanel.removeAll()
        savedPlanCards.clear()
        val indexedPlans = config.trainingPlans.plans.withIndex()
            .sortedWith(compareBy({ it.value.skill }, { it.value.startXp }))
        var expandedCard: TrainingPlanCard? = null

        if (indexedPlans.isEmpty()) {
            savedPlansPanel.add(JLabel("No plans yet").apply { alignmentX = CENTER_ALIGNMENT })
        } else {
            indexedPlans.forEach { indexed ->
                val card = TrainingPlanCard(
                    plan = indexed.value,
                    planIndex = indexed.index,
                    config = config,
                    itemManager = itemManager,
                    recipeRepository = recipeRepository,
                    planCalculator = planCalculator,
                    initialOwnedQuantities = currentOwnershipSnapshot.quantities,
                    initiallyExpanded = indexed.index == expandedPlanIndex,
                    onPlanChanged = ::onPlanChanged,
                ).apply { alignmentX = CENTER_ALIGNMENT }
                savedPlanCards[indexed.index] = card
                savedPlansPanel.add(card)
                if (indexed.index == expandedPlanIndex) expandedCard = card
            }
        }
        savedPlansPanel.revalidate()
        savedPlansPanel.repaint()
        expandedCard?.let { card ->
            SwingUtilities.invokeLater {
                card.scrollRectToVisible(java.awt.Rectangle(0, 0, card.width, card.height))
            }
        }
    }

    private fun onPlanChanged() {
        refreshSavedPlans()
    }

    private fun relevantOwnedQuantities(): Map<Int, Long> {
        val graph = selectedGraph ?: return emptyMap()
        return ownedQuantityPanel.getOwnedQuantities().filterKeys { itemId ->
            itemId != graph.rootItemId && graph.nodes.containsKey(itemId)
        }
    }

    private companion object {
        const val SAVED_PLANS_TAB_INDEX = 1
    }
}
