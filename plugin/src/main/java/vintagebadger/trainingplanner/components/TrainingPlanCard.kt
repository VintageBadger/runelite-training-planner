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
import vintagebadger.trainingplanner.data.ResolvedRecipeStep
import vintagebadger.trainingplanner.data.TrainingRecipeRepository
import vintagebadger.trainingplanner.models.Skill
import vintagebadger.trainingplanner.models.TrainingPlan
import vintagebadger.trainingplanner.models.TrainingPlanList
import vintagebadger.trainingplanner.util.totalXpBetween
import vintagebadger.trainingplanner.wiki.IngredientRef
import java.awt.BorderLayout
import java.awt.Color
import java.text.NumberFormat
import javax.swing.BorderFactory
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
    private val onPlanChanged: () -> Unit,
) : JPanel() {

    private var isExpanded = false
    private var isEditing = false

    private val headerPanel = JPanel()
    private val contentPanel = JPanel()

    private val skillDisplayName: String
        get() = Skill.entries.find { it.name == plan.skill }?.displayName ?: plan.skill

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        background = ColorScheme.DARK_GRAY_COLOR
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR),
            BorderFactory.createEmptyBorder(4, 0, 4, 0)
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

        val leftPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            background = ColorScheme.DARKER_GRAY_COLOR
        }

        val toggleButton = IconToggleButton(
            initialSelected = isExpanded,
            selectedIcon = Icon.ChevronDown.imageIcon,
            unselectedIcon = Icon.ChevronRight.imageIcon,
        ) { expanded ->
            isExpanded = expanded
            contentPanel.isVisible = expanded
            revalidate()
            repaint()
        }

        removeButtonDecorations(toggleButton)

        val skillLabel = JLabel(skillDisplayName).apply {
            font = FontManager.getRunescapeBoldFont()
            foreground = Color.WHITE
            border = EmptyBorder(0, 4, 0, 12)
        }

        val levelLabel = JLabel("${plan.startLevel} -> ${plan.endLevel}").apply {
            font = FontManager.getRunescapeFont()
            foreground = ColorScheme.LIGHT_GRAY_COLOR
        }

        leftPanel.add(toggleButton)
        leftPanel.add(skillLabel)
        leftPanel.add(levelLabel)

        val rightPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            background = ColorScheme.DARKER_GRAY_COLOR
        }

        if (isEditing) {
            rightPanel.add(buildCancelButton())
        } else {
            rightPanel.add(buildEditButton())
            rightPanel.add(buildDeleteButton())
        }

        headerPanel.add(leftPanel, BorderLayout.CENTER)
        headerPanel.add(rightPanel, BorderLayout.EAST)
    }

    // todo: icon button here
    private fun buildCancelButton() = JButton("✕").apply {
        font = FontManager.getRunescapeBoldFont()
        foreground = ColorScheme.LIGHT_GRAY_COLOR
        background = ColorScheme.DARK_GRAY_COLOR
        border = EmptyBorder(4, 6, 4, 6)
        toolTipText = "Cancel"
        addActionListener {
            isEditing = false
            buildHeader()
            buildContent()
            revalidate()
            repaint()
        }
    }

    // todo: fix toggled state when this button is pressed
    // todo: plan out how edit works
    // todo: fix edit functionality
    private fun buildEditButton() = Icon.Edit.button().apply {
        background = ColorScheme.DARK_GRAY_COLOR
        border = EmptyBorder(4, 6, 4, 6)
        toolTipText = "Edit"
        addActionListener {
            isEditing = true
            buildHeader()
            buildContent()
            revalidate()
            repaint()
        }
    }

    private fun buildDeleteButton() = Icon.Delete.button().apply {
        background = ColorScheme.DARK_GRAY_COLOR
        border = EmptyBorder(4, 6, 4, 6)
        toolTipText = "Delete"
        addActionListener { deletePlan() }
    }

    private fun buildContent() {
        contentPanel.removeAll()
        contentPanel.layout = DynamicGridLayout(0, 1, 0, 3)
        contentPanel.border = EmptyBorder(2, 0,0,0)
        contentPanel.background = ColorScheme.DARK_GRAY_COLOR
        contentPanel.isVisible = isExpanded

        if (isEditing) {
            buildEditContent()
        } else {
            buildDisplayContent()
        }
    }

    private fun buildDisplayContent() {
        val trainingMethod = plan.trainingMethod

        // We do firstOrNull because a component can have multiple ways to craft it.
        // For now, we assume the wiki returned first method is the correct one.
        // In the future it could be user-chosen
        val action = plan.trainingMethod.methods.firstOrNull()
        val selectedSkill = Skill.entries.find { it.name == plan.skill }
        val skillRequirement = selectedSkill?.let { skill ->
            action?.skills?.firstOrNull { it.skill.equals(skill.displayName, ignoreCase = true) }
        }

        if (skillRequirement != null && skillRequirement.xp > 0) {
            val totalXp = totalXpBetween(plan.startLevel, plan.endLevel)
            addInfoRow("Total XP:", NumberFormat.getNumberInstance().format(totalXp))
        }

        if (skillRequirement != null && skillRequirement.level > 0) {
            addInfoRow("Level required:", skillRequirement.level.toString())
        }

        val outputRow = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            background = ColorScheme.DARK_GRAY_COLOR
            isOpaque = false
        }
        val outputLabel = JLabel("Output:").apply {
            font = FontManager.getRunescapeBoldFont()
            foreground = Color.WHITE
        }
        outputRow.add(outputLabel)

        if (trainingMethod.id > 0) {
            val itemIcon = getItemIcon(trainingMethod.id, itemManager, leftPadding = 4)
            outputRow.add(itemIcon)
        }
        outputRow.add(JLabel(trainingMethod.name).apply {
            font = FontManager.getRunescapeFont()
            foreground = ColorScheme.LIGHT_GRAY_COLOR
        })
        contentPanel.add(outputRow)

        //TODO: update requiredQuantity based on xp calculation like in new plan panel
        val rootStep = selectedSkill?.let {
            recipeRepository.resolveSteps(trainingMethod, it, 1)
        }

        if (rootStep != null) {
            val stepsLabel = JLabel("Steps:").apply {
                font = FontManager.getRunescapeBoldFont()
                foreground = Color.WHITE
                border = EmptyBorder(8, 0, 4, 0)
            }
            contentPanel.add(stepsLabel)

            contentPanel.add(buildStepNode(rootStep, depth = 0))
        } else if (action != null && action.requires.isNotEmpty()) {
            addRequirements(action.requires, depth = 0)
        }
    }

    private fun buildStepNode(step: ResolvedRecipeStep, depth: Int): JPanel {
        val hasContent = step.children.isNotEmpty() || step.requires.isNotEmpty()

        val stepText = buildString {
            append("${step.outputName} x${step.outputQuantity}")
            if (step.recipeMethod.isNotBlank()) {
                append(" (${step.recipeMethod})")
            }
        }

        val bodyPanel = JPanel(BorderLayout()).apply {
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
        }
        if (depth > 0) {
            headerRow.border = EmptyBorder(0, depth * 16, 0, 0)
        }

        val toggle = if (hasContent) {
            IconToggleButton(
                initialSelected = true,
                selectedIcon = Icon.ChevronDown.imageIcon,
                unselectedIcon = Icon.ChevronRight.imageIcon,
            ) { expanded ->
                bodyPanel.isVisible = expanded
                this@TrainingPlanCard.revalidate()
                this@TrainingPlanCard.repaint()
            }.apply { removeButtonDecorations(this) }
        } else null

        if (toggle != null) {
            headerRow.add(toggle)
        }
        if (step.outputId > 0) {
            val itemIcon = getItemIcon(step.outputId, itemManager, leftPadding = 4)
            headerRow.add(itemIcon)
        }
        headerRow.add(JLabel(stepText).apply {
            font = FontManager.getRunescapeSmallFont()
            foreground = ColorScheme.LIGHT_GRAY_COLOR
            alignmentX = LEFT_ALIGNMENT
        })

        if (step.requires.isNotEmpty()) {
            addRequirements(step.requires, depth = depth + 1 , target = bodyPanel)
        }
        step.children.forEach { child ->
            bodyPanel.add(buildStepNode(child, depth + 1))
        }

        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = ColorScheme.DARK_GRAY_COLOR
            isOpaque = false
            add(headerRow)
            add(bodyPanel)
        }
    }

    private fun addRequirements(requirements: List<IngredientRef>, depth: Int, target: JPanel = contentPanel) {
        requirements.forEach { ingredient ->
            val row = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                background = ColorScheme.DARK_GRAY_COLOR
                border = EmptyBorder(0, (depth * 16) + ICON_SIZE, 0, 0)
                isOpaque = false
                alignmentX = LEFT_ALIGNMENT
            }
            if (ingredient.id > 0) {
                val itemIcon = getItemIcon(ingredient.id, itemManager, leftPadding = 4)
                row.add(itemIcon)
            }
            row.add(JLabel("${ingredient.name} x${ingredient.quantity}").apply {
                font = FontManager.getRunescapeSmallFont()
                foreground = ColorScheme.LIGHT_GRAY_COLOR
            })
            target.add(row)
        }
    }

    private fun buildEditContent() {
        val nameField = JTextField(plan.trainingMethod.name).apply {
            font = FontManager.getRunescapeFont()
            background = ColorScheme.DARK_GRAY_COLOR
            foreground = Color.WHITE
            caretColor = Color.WHITE
        }

        nameField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = autoSaveMethod(nameField.text.trim())
            override fun removeUpdate(e: DocumentEvent) = autoSaveMethod(nameField.text.trim())
            override fun changedUpdate(e: DocumentEvent) {}
        })

        val label = JLabel("Output Name:").apply {
            font = FontManager.getRunescapeFont()
            foreground = ColorScheme.LIGHT_GRAY_COLOR
        }

        contentPanel.add(label)
        contentPanel.add(nameField)
    }

    private fun autoSaveMethod(newName: String) {
        if (newName.isBlank()) return
        val updatedMethod = plan.trainingMethod.copy(name = newName)
        savePlan(plan.copy(trainingMethod = updatedMethod))
    }

    private fun addInfoRow(label: String, value: String) {
        val row = JLabel("$label $value").apply {
            font = FontManager.getRunescapeFont()
            foreground = ColorScheme.LIGHT_GRAY_COLOR
        }
        contentPanel.add(row)
    }

    private fun deletePlan() {
        val currentPlans = config.trainingPlans.plans.toMutableList()
        if (planIndex in currentPlans.indices) {
            currentPlans.removeAt(planIndex)
            config.setTrainingPlans(TrainingPlanList(currentPlans))
            onPlanChanged()
        }
    }

    private fun savePlan(updatedPlan: TrainingPlan = plan) {
        val currentPlans = config.trainingPlans.plans.toMutableList()
        if (planIndex in currentPlans.indices) {
            currentPlans[planIndex] = updatedPlan
            config.setTrainingPlans(TrainingPlanList(currentPlans))
            onPlanChanged()
        }
    }
}
