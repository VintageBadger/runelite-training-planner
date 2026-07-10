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
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JTabbedPane

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

    private lateinit var savedPlansPanel: JPanel
    private lateinit var skillDropdown: JComboBox<Skill?>
    private var availableMethods: List<OutputItemRecipes> = emptyList()
    private var selectedGraph: ResolvedRecipeGraph? = null
    private var currentResults: Map<Int, Result<PlanResult>> = emptyMap()
    private var lastSavedPlanIndex: Int? = null

    init {
        add(JLabel("Training Planner"))
        add(JTabbedPane().apply {
            addTab("New Plan", buildNewPlanTab())
            addTab("Saved Plans", buildSavedPlansTab())
        })
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
            return
        }

        availableMethods = recipeRepository.methodsFor(skill, target.startLevel)
        methodList.setMethods(availableMethods)
        updateSelectedGraph()
        refreshPlanResults()
        tryAutoSave()
    }

    private fun onMethodSelected() {
        updateSelectedGraph()
        refreshPlanResults()
        tryAutoSave()
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
        ownedQuantityPanel.setSnapshot(snapshot)
        refreshPlanResults()
        tryAutoSave()
    }

    fun shutDown() {
        snapshotService.clearListener()
    }

    private fun tryAutoSave() {
        val selectedSkill = skillDropdown.selectedItem as? Skill ?: return
        val target = calculatorUi.target ?: return
        if (target.targetXp <= target.startXp) return
        val selectedMethod = methodList.getSelectedMethod() ?: return
        if (currentResults[selectedMethod.id]?.isSuccess != true) return

        val currentPlans = config.getTrainingPlans().plans.toMutableList()
        val existing = lastSavedPlanIndex?.takeIf { it in currentPlans.indices }?.let(currentPlans::get)
        val plan = TrainingPlan(
            skill = selectedSkill.name,
            startLevel = target.startLevel,
            endLevel = target.endLevel,
            startXp = target.startXp,
            targetXp = target.targetXp,
            rootRecipeId = selectedMethod.id,
            displayNameOverride = existing?.displayNameOverride,
            methodSelections = emptyMap(),
            ownedQuantities = relevantOwnedQuantities(),
        )

        if (existing == plan) return
        if (lastSavedPlanIndex != null && lastSavedPlanIndex in currentPlans.indices) {
            currentPlans[lastSavedPlanIndex!!] = plan
        } else {
            currentPlans += plan
            lastSavedPlanIndex = currentPlans.lastIndex
        }
        config.setTrainingPlans(TrainingPlanList(currentPlans))
        refreshSavedPlans()
    }

    fun refreshSavedPlans() {
        savedPlansPanel.removeAll()
        val indexedPlans = config.trainingPlans.plans.withIndex()
            .sortedWith(compareBy({ it.value.skill }, { it.value.startXp }))

        if (indexedPlans.isEmpty()) {
            savedPlansPanel.add(JLabel("No plans yet").apply { alignmentX = CENTER_ALIGNMENT })
        } else {
            indexedPlans.forEach { indexed ->
                savedPlansPanel.add(
                    TrainingPlanCard(
                        plan = indexed.value,
                        planIndex = indexed.index,
                        config = config,
                        itemManager = itemManager,
                        recipeRepository = recipeRepository,
                        planCalculator = planCalculator,
                        onPlanChanged = ::onPlanChanged,
                    ).apply { alignmentX = CENTER_ALIGNMENT },
                )
            }
        }
        savedPlansPanel.revalidate()
        savedPlansPanel.repaint()
    }

    private fun onPlanChanged() {
        lastSavedPlanIndex = null
        refreshSavedPlans()
    }

    private fun relevantOwnedQuantities(): Map<Int, Long> {
        val graph = selectedGraph ?: return emptyMap()
        return ownedQuantityPanel.getOwnedQuantities().filterKeys { itemId ->
            itemId != graph.rootItemId && graph.nodes.containsKey(itemId)
        }
    }
}
