package vintagebadger.trainingplanner.components

import net.runelite.client.game.ItemManager
import net.runelite.client.ui.ColorScheme
import net.runelite.client.ui.DynamicGridLayout
import net.runelite.client.ui.FontManager
import net.runelite.client.util.SwingUtil.removeButtonDecorations
import vintagebadger.trainingplanner.TrainingPlannerConfig
import vintagebadger.trainingplanner.models.Skill
import vintagebadger.trainingplanner.models.TrainingPlan
import vintagebadger.trainingplanner.models.TrainingPlanList
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.text.NumberFormat
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.border.EmptyBorder
import javax.swing.border.MatteBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class TrainingPlanCard(
    private val plan: TrainingPlan,
    private val planIndex: Int,
    private val config: TrainingPlannerConfig,
    private val itemManager: ItemManager,
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
        border = MatteBorder(0, 0, 1, 0, ColorScheme.MEDIUM_GRAY_COLOR)

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

        val toggleButton = JButton(if (isExpanded) "▼" else "▶")
            .apply {
            font = FontManager.getRunescapeBoldFont()
            foreground = ColorScheme.LIGHT_GRAY_COLOR
            preferredSize = Dimension(20, 20)
            addActionListener {
                isExpanded = !isExpanded
                contentPanel.isVisible = isExpanded
                text = if (isExpanded) "▼" else "▶"
                revalidate()
                repaint()
            }
        }
        removeButtonDecorations(toggleButton)

        val skillLabel = JLabel(skillDisplayName).apply {
            font = FontManager.getRunescapeBoldFont()
            foreground = Color.WHITE
            border = EmptyBorder(0, 4, 0, 12)
        }

        val levelLabel = JLabel("${plan.startLevel} \u2192 ${plan.endLevel}").apply {
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

    private fun buildEditButton() = JButton("✎").apply {
        font = FontManager.getRunescapeBoldFont()
        foreground = ColorScheme.LIGHT_GRAY_COLOR
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

    private fun buildDeleteButton() = JButton("🗑").apply {
        font = FontManager.getRunescapeBoldFont()
        foreground = Color.RED
        background = ColorScheme.DARK_GRAY_COLOR
        border = EmptyBorder(4, 6, 4, 6)
        toolTipText = "Delete"
        addActionListener { deletePlan() }
    }

    private fun buildContent() {
        contentPanel.removeAll()
        contentPanel.layout = DynamicGridLayout(0, 1, 0, 3)
        contentPanel.background = ColorScheme.DARK_GRAY_COLOR
        contentPanel.isVisible = isExpanded

        if (isEditing) {
            buildEditContent()
        } else {
            buildDisplayContent()
        }
    }

    private fun buildDisplayContent() {
        val method = plan.trainingMethod

        addInfoRow("Method:", method.name)

        if (method.wikiPage.isNotBlank()) {
            addInfoRow("Wiki:", method.wikiPage)
        }

        if (method.totalXp > 0) {
            addInfoRow("Total XP:", NumberFormat.getNumberInstance().format(method.totalXp.toLong()))
        }

        if (method.maxLevel > 0) {
            addInfoRow("Max Level:", method.maxLevel.toString())
        }

        if (method.output.isNotEmpty()) {
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
            method.output.forEach { itemRef ->
                if (itemRef.id > 0) {
                    val iconLabel = JLabel().apply {
                        border = EmptyBorder(0, 4, 0, 0)
                    }
                    val img = itemManager.getImage(itemRef.id)
                    img.addTo(iconLabel)
                    outputRow.add(iconLabel)
                }
                outputRow.add(JLabel("${itemRef.name} x${itemRef.quantity}  ").apply {
                    font = FontManager.getRunescapeFont()
                    foreground = ColorScheme.LIGHT_GRAY_COLOR
                })
            }
            contentPanel.add(outputRow)
        }

        if (method.steps.isNotEmpty()) {
            val stepsLabel = JLabel("Steps:").apply {
                font = FontManager.getRunescapeBoldFont()
                foreground = Color.WHITE
                border = EmptyBorder(8, 0, 4, 0)
            }
            contentPanel.add(stepsLabel)

            method.steps.forEachIndexed { i, step ->
                var stepText = "  ${i + 1}. ${step.name.ifBlank { step.step }}"
                if (step.xp > 0) {
                    stepText += " (${NumberFormat.getNumberInstance().format(step.xp)} XP)"
                }
                if (step.level > 0) {
                    stepText += " [Lvl ${step.level}]"
                }
                contentPanel.add(JLabel(stepText).apply {
                    font = FontManager.getRunescapeSmallFont()
                    foreground = ColorScheme.LIGHT_GRAY_COLOR
                })
            }
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

        val label = JLabel("Method Name:").apply {
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
        val currentPlans = config.getTrainingPlans().plans.toMutableList()
        if (planIndex in currentPlans.indices) {
            currentPlans.removeAt(planIndex)
            config.setTrainingPlans(TrainingPlanList(currentPlans))
            onPlanChanged()
        }
    }

    private fun savePlan(updatedPlan: TrainingPlan = plan) {
        val currentPlans = config.getTrainingPlans().plans.toMutableList()
        if (planIndex in currentPlans.indices) {
            currentPlans[planIndex] = updatedPlan
            config.setTrainingPlans(TrainingPlanList(currentPlans))
            onPlanChanged()
        }
    }
}
