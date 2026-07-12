package vintagebadger.trainingplanner.components

import net.runelite.client.game.ItemManager
import net.runelite.client.ui.ColorScheme
import net.runelite.client.ui.DynamicGridLayout
import net.runelite.client.ui.FontManager
import net.runelite.client.util.SwingUtil.removeButtonDecorations
import vintagebadger.trainingplanner.TrainingPlannerConfig
import vintagebadger.trainingplanner.components.core.ICON_SIZE
import vintagebadger.trainingplanner.components.core.Icon
import vintagebadger.trainingplanner.components.core.IconToggleButton
import vintagebadger.trainingplanner.components.core.button
import vintagebadger.trainingplanner.data.RecipeRequirementEdge
import vintagebadger.trainingplanner.data.ResolvedRecipeGraph
import vintagebadger.trainingplanner.data.TrainingRecipeRepository
import vintagebadger.trainingplanner.models.Skill
import vintagebadger.trainingplanner.models.TrainingPlan
import vintagebadger.trainingplanner.models.TrainingPlanList
import vintagebadger.trainingplanner.planning.PlanNodeResult
import vintagebadger.trainingplanner.planning.RecipePlanCalculator
import java.awt.BorderLayout
import java.awt.Color
import java.text.NumberFormat
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.border.EmptyBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class TrainingPlanCard(
    private val plan: TrainingPlan,
    private val planIndex: Int,
    private val config: TrainingPlannerConfig,
    private val itemManager: ItemManager,
    private val recipeRepository: TrainingRecipeRepository,
    private val planCalculator: RecipePlanCalculator,
    initialOwnedQuantities: Map<Int, Long> = plan.ownedQuantities,
    initiallyExpanded: Boolean = false,
    private val onPlanChanged: () -> Unit,
) : JPanel() {
    private var isExpanded = initiallyExpanded
    private var isEditing = false
    private var ownedQuantities = initialOwnedQuantities
    private val expandedRecipeSteps = mutableMapOf<Int, Boolean>()
    private val headerPanel = JPanel()
    private val contentPanel = JPanel()
    private val skillDisplayName: String
        get() = Skill.entries.find { it.name == plan.skill }?.displayName ?: plan.skill

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = ColorScheme.DARK_GRAY_COLOR
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
            BorderFactory.createEmptyBorder(4, 0, 4, 0),
        )
        add(headerPanel)
        add(contentPanel)
        buildHeader()
        buildContent()
    }

    private fun buildHeader() {
        headerPanel.removeAll()
        headerPanel.layout = BorderLayout()
        headerPanel.background = ColorScheme.DARKER_GRAY_COLOR
        headerPanel.border = EmptyBorder(6, 8, 6, 8)

        headerPanel.add(JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            background = ColorScheme.DARKER_GRAY_COLOR
            val toggle = IconToggleButton(
                initialSelected = isExpanded,
                selectedIcon = Icon.ChevronDown.imageIcon,
                unselectedIcon = Icon.ChevronRight.imageIcon,
            ) { expanded ->
                isExpanded = expanded
                contentPanel.isVisible = expanded
                revalidate()
                repaint()
            }
            removeButtonDecorations(toggle)
            add(toggle)
            add(JLabel(skillDisplayName).apply {
                font = FontManager.getRunescapeBoldFont()
                foreground = Color.WHITE
                border = EmptyBorder(0, 4, 0, 12)
            })
            add(JLabel("${plan.startLevel} -> ${plan.endLevel}").apply {
                font = FontManager.getRunescapeFont()
                foreground = ColorScheme.LIGHT_GRAY_COLOR
            })
        }, BorderLayout.CENTER)

        headerPanel.add(JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            background = ColorScheme.DARKER_GRAY_COLOR
            if (isEditing) {
                add(JButton("X").apply {
                    toolTipText = "Cancel"
                    addActionListener {
                        isEditing = false
                        rebuild()
                    }
                })
            } else {
                add(Icon.Edit.button().apply {
                    background = ColorScheme.DARK_GRAY_COLOR
                    border = EmptyBorder(4, 6, 4, 6)
                    toolTipText = "Edit"
                    addActionListener {
                        isEditing = true
                        rebuild()
                    }
                })
                add(Icon.Delete.button().apply {
                    background = ColorScheme.DARK_GRAY_COLOR
                    border = EmptyBorder(4, 6, 4, 6)
                    toolTipText = "Delete"
                    addActionListener { deletePlan() }
                })
            }
        }, BorderLayout.EAST)
    }

    private fun buildContent() {
        contentPanel.removeAll()
        contentPanel.layout = DynamicGridLayout(0, 1, 0, 3)
        contentPanel.border = EmptyBorder(2, 0, 0, 0)
        contentPanel.background = ColorScheme.DARK_GRAY_COLOR
        contentPanel.isVisible = isExpanded
        if (isEditing) buildEditContent() else buildDisplayContent()
    }

    private fun buildDisplayContent() {
        val output = recipeRepository.recipeById(plan.rootRecipeId)
        val selectedSkill = Skill.entries.find { it.name == plan.skill }
        if (output == null || selectedSkill == null) {
            addInfoRow("Unavailable:", "The saved recipe or skill was not found.")
            return
        }

        val calculation = runCatching {
            val graph = recipeRepository.resolveGraph(output, selectedSkill, plan.methodSelections)
            graph to planCalculator.solve(
                graph = graph,
                targetXpTenths = Math.multiplyExact(plan.xpRequired, 10L),
                ownedQuantities = ownedQuantities,
            )
        }.getOrElse { error ->
            addInfoRow("Unavailable:", error.message ?: "Could not calculate this plan.")
            return
        }
        val (graph, result) = calculation

        addInfoRow("XP required:", NumberFormat.getNumberInstance().format(plan.xpRequired))
        addInfoRow("Planned XP:", formatXpTenths(result.totalXpTenths))
        addInfoRow("Extra XP:", formatXpTenths(result.extraXpTenths))
        graph.nodes[graph.rootItemId]?.levelRequirement?.takeIf { it > 0 }?.let {
            addInfoRow("Level required:", it.toString())
        }

        contentPanel.add(JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            background = ColorScheme.DARK_GRAY_COLOR
            isOpaque = false
            add(JLabel("Output:").apply {
                font = FontManager.getRunescapeBoldFont()
                foreground = Color.WHITE
            })
            add(getItemIcon(output.id, itemManager, leftPadding = 4))
            add(JLabel(plan.displayNameOverride ?: output.name).apply {
                font = FontManager.getRunescapeFont()
                foreground = ColorScheme.LIGHT_GRAY_COLOR
            })
        })

        addSectionLabel("Steps:")
        contentPanel.add(buildRecipeTree(graph, result.nodes))
    }

    private fun buildRecipeTree(
        graph: ResolvedRecipeGraph,
        steps: List<PlanNodeResult>,
    ): JPanel {
        val stepsById = steps.associateBy(PlanNodeResult::itemId)
        val outgoing = graph.edges.groupBy(RecipeRequirementEdge::parentItemId)
        return buildStepNode(
            itemId = graph.rootItemId,
            branchDemand = stepsById.getValue(graph.rootItemId).demandUnits,
            depth = 0,
            graph = graph,
            stepsById = stepsById,
            outgoing = outgoing,
            renderedItems = mutableSetOf(),
        )
    }

    private fun buildStepNode(
        itemId: Int,
        branchDemand: Long,
        depth: Int,
        graph: ResolvedRecipeGraph,
        stepsById: Map<Int, PlanNodeResult>,
        outgoing: Map<Int, List<RecipeRequirementEdge>>,
        renderedItems: MutableSet<Int>,
    ): JPanel {
        val node = graph.nodes.getValue(itemId)
        val step = stepsById.getValue(itemId)
        val isFirstAppearance = renderedItems.add(itemId)
        val children = if (isFirstAppearance && step.craftActions > 0L) {
            outgoing[itemId].orEmpty()
        } else {
            emptyList()
        }

        val bodyPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = ColorScheme.DARK_GRAY_COLOR
            isOpaque = false
            alignmentX = LEFT_ALIGNMENT
        }
        val headerRow = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            background = ColorScheme.DARK_GRAY_COLOR
            isOpaque = false
            alignmentX = LEFT_ALIGNMENT
            if (depth > 0) border = EmptyBorder(0, depth * 16, 0, 0)
        }

        val isStepExpanded = expandedRecipeSteps[itemId] ?: true
        bodyPanel.isVisible = isStepExpanded
        val toggle = if (children.isNotEmpty()) {
            IconToggleButton(
                initialSelected = isStepExpanded,
                selectedIcon = Icon.ChevronDown.imageIcon,
                unselectedIcon = Icon.ChevronRight.imageIcon,
            ) { expanded ->
                expandedRecipeSteps[itemId] = expanded
                bodyPanel.isVisible = expanded
                this@TrainingPlanCard.revalidate()
                this@TrainingPlanCard.repaint()
            }.apply { removeButtonDecorations(this) }
        } else {
            null
        }
        if (toggle != null) headerRow.add(toggle) else headerRow.add(Box.createHorizontalStrut(ICON_SIZE))
        if (itemId > 0) headerRow.add(getItemIcon(itemId, itemManager, leftPadding = 4))

        headerRow.add(JLabel(
            if (isFirstAppearance) buildStepText(node.itemName, node.methodName, step, itemId == graph.rootItemId)
            else "${node.itemName} (shared step above)",
        ).apply {
            font = FontManager.getRunescapeSmallFont()
            foreground = ColorScheme.LIGHT_GRAY_COLOR
            alignmentX = LEFT_ALIGNMENT
            toolTipText = buildStepTooltip(step, branchDemand, isFirstAppearance)
        })

        children.forEach { edge ->
            val childDemand = Math.multiplyExact(step.craftActions, edge.quantityPerAction)
            bodyPanel.add(
                buildStepNode(
                    itemId = edge.ingredientItemId,
                    branchDemand = childDemand,
                    depth = depth + 1,
                    graph = graph,
                    stepsById = stepsById,
                    outgoing = outgoing,
                    renderedItems = renderedItems,
                ),
            )
        }

        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = ColorScheme.DARK_GRAY_COLOR
            isOpaque = false
            alignmentX = LEFT_ALIGNMENT
            add(headerRow)
            add(bodyPanel)
        }
    }

    private fun buildStepText(
        itemName: String,
        methodName: String,
        step: PlanNodeResult,
        isRoot: Boolean,
    ): String {
        val displayName = if (isRoot) plan.displayNameOverride ?: itemName else itemName
        val quantity = if (step.craftable) step.craftActions else step.unitsToAcquire
        val details = buildList {
            if (methodName.isNotBlank()) add(methodName)
            if (!isRoot && step.ownedUsed > 0L) {
                add("${NumberFormat.getNumberInstance().format(step.ownedUsed)} owned")
            }
            if (step.outputQuantity > 1L) {
                add("${NumberFormat.getNumberInstance().format(step.producedUnits)} produced")
            }
        }
        return buildString {
            append(displayName)
            append(" x")
            append(NumberFormat.getNumberInstance().format(quantity))
            if (details.isNotEmpty()) append(" (${details.joinToString(", ")})")
        }
    }

    private fun buildStepTooltip(
        step: PlanNodeResult,
        branchDemand: Long,
        isFirstAppearance: Boolean,
    ): String {
        if (!isFirstAppearance) {
            return "Needed on this branch: ${NumberFormat.getNumberInstance().format(branchDemand)}; totals are shown above."
        }
        return buildList {
            add("Total needed: ${NumberFormat.getNumberInstance().format(step.demandUnits)}")
            if (step.ownedUsed > 0L) add("owned used: ${NumberFormat.getNumberInstance().format(step.ownedUsed)}")
            if (step.craftable) add("actions: ${NumberFormat.getNumberInstance().format(step.craftActions)}")
            if (!step.craftable) add("to acquire: ${NumberFormat.getNumberInstance().format(step.unitsToAcquire)}")
            if (step.surplusUnits > 0L) add("surplus: ${NumberFormat.getNumberInstance().format(step.surplusUnits)}")
        }.joinToString("; ")
    }

    fun updateOwnedQuantities(quantities: Map<Int, Long>) {
        if (ownedQuantities == quantities) return

        ownedQuantities = quantities
        if (!isEditing) {
            buildContent()
            revalidate()
            repaint()
        }
    }

    private fun addSectionLabel(text: String) {
        contentPanel.add(JLabel(text).apply {
            font = FontManager.getRunescapeBoldFont()
            foreground = Color.WHITE
            border = EmptyBorder(8, 0, 2, 0)
        })
    }

    private fun buildEditContent() {
        val outputName = plan.displayNameOverride
            ?: recipeRepository.recipeById(plan.rootRecipeId)?.name
            ?: "Unknown recipe"
        val nameField = JTextField(outputName).apply {
            font = FontManager.getRunescapeFont()
            background = ColorScheme.DARK_GRAY_COLOR
            foreground = Color.WHITE
            caretColor = Color.WHITE
        }
        nameField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = saveDisplayName(nameField.text.trim())
            override fun removeUpdate(e: DocumentEvent) = saveDisplayName(nameField.text.trim())
            override fun changedUpdate(e: DocumentEvent) = Unit
        })
        contentPanel.add(JLabel("Output Name:").apply {
            font = FontManager.getRunescapeFont()
            foreground = ColorScheme.LIGHT_GRAY_COLOR
        })
        contentPanel.add(nameField)
    }

    private fun addInfoRow(label: String, value: String) {
        contentPanel.add(JLabel("$label $value").apply {
            font = FontManager.getRunescapeFont()
            foreground = ColorScheme.LIGHT_GRAY_COLOR
        })
    }

    private fun saveDisplayName(newName: String) {
        savePlan(plan.copy(displayNameOverride = newName.takeIf { it.isNotBlank() }))
    }

    private fun deletePlan() {
        val currentPlans = config.trainingPlans.plans.toMutableList()
        if (planIndex in currentPlans.indices) {
            currentPlans.removeAt(planIndex)
            config.setTrainingPlans(TrainingPlanList(currentPlans))
            onPlanChanged()
        }
    }

    private fun savePlan(updatedPlan: TrainingPlan) {
        val currentPlans = config.trainingPlans.plans.toMutableList()
        if (planIndex in currentPlans.indices && currentPlans[planIndex] != updatedPlan) {
            currentPlans[planIndex] = updatedPlan
            config.setTrainingPlans(TrainingPlanList(currentPlans))
            onPlanChanged()
        }
    }

    private fun rebuild() {
        buildHeader()
        buildContent()
        revalidate()
        repaint()
    }

    private fun formatXpTenths(value: Long): String {
        val whole = value / 10L
        val remainder = value % 10L
        val formattedWhole = NumberFormat.getNumberInstance().format(whole)
        return if (remainder == 0L) formattedWhole else "$formattedWhole.$remainder"
    }
}
