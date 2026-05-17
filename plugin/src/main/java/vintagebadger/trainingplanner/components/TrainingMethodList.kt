package vintagebadger.trainingplanner.components

import net.runelite.client.game.ItemManager
import net.runelite.client.ui.ColorScheme
import net.runelite.client.ui.DynamicGridLayout
import net.runelite.client.ui.FontManager
import vintagebadger.trainingplanner.models.Skill
import vintagebadger.trainingplanner.wiki2.OutputItemRecipes
import java.text.NumberFormat
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.border.EmptyBorder
import kotlin.math.ceil

class TrainingMethodList(
    private val itemManager: ItemManager,
    private val onMethodSelected: () -> Unit,
) : JPanel() {

    private var selectedMethod: OutputItemRecipes? = null
    private val rowPanels = mutableMapOf<OutputItemRecipes, JPanel>()

    fun getSelectedMethod(): OutputItemRecipes? = selectedMethod

    fun setMethods(methods: List<OutputItemRecipes>, skill: Skill, expRequired: Int?) {
        val previouslySelected = selectedMethod?.id
        removeAll()
        rowPanels.clear()
        selectedMethod = null

        layout = DynamicGridLayout(0, 1, 0, 3)
        background = ColorScheme.DARK_GRAY_COLOR

        if (methods.isEmpty()) {
            add(JLabel("No methods available").apply {
                font = FontManager.getRunescapeFont()
                foreground = ColorScheme.LIGHT_GRAY_COLOR
            })
            return
        }

        methods.forEach { method ->
            val row = createRow(method, skill, expRequired)
            rowPanels[method] = row
            add(row)
        }

        previouslySelected?.let { id ->
            methods.find { it.id == id }?.let { selectMethod(it, skipCallback = true) }
        }

        revalidate()
        repaint()
    }

    private fun createRow(method: OutputItemRecipes, skill: Skill, expRequired: Int?): JPanel {
        val row = JPanel().apply {
            layout = javax.swing.BoxLayout(this, javax.swing.BoxLayout.X_AXIS)
            background = ColorScheme.DARKER_GRAY_COLOR
            border = EmptyBorder(4, 8, 4, 8)
            isOpaque = true

            val outputId = method.id
            if (outputId > 0) {
                val iconLabel = JLabel().apply {
                    border = EmptyBorder(0, 0, 0, 0)
                }
                val img = itemManager.getImage(outputId)
                img.addTo(iconLabel)
                add(iconLabel)
            }

            val textPanel = JPanel().apply {
                layout = DynamicGridLayout(0, 1)
                background = ColorScheme.DARKER_GRAY_COLOR
                isOpaque = false
            }

            val recipe = method.methods.firstOrNull()
            val skillRequirement = recipe?.skills?.firstOrNull { it.skill.equals(skill.displayName, ignoreCase = true) }
            val totalXp = skillRequirement?.xp ?: 0.0
            val craftCount = if (expRequired != null && expRequired > 0 && totalXp > 0) {
                ceil(expRequired / totalXp).toLong()
            } else {
                null
            }

            val craftText = if (craftCount != null) {
                " — ${NumberFormat.getNumberInstance().format(craftCount)} crafts"
            } else {
                ""
            }

            val topLabel = JLabel(method.name).apply {
                font = FontManager.getRunescapeBoldFont()
                foreground = ColorScheme.TEXT_COLOR
            }

            val methodText = recipe?.method?.takeIf { it.isNotBlank() } ?: "Unknown method"
            val subLabel = JLabel("$methodText$craftText").apply {
                font = FontManager.getRunescapeSmallFont()
                foreground = ColorScheme.LIGHT_GRAY_COLOR
            }

            textPanel.add(topLabel)
            textPanel.add(subLabel)
            add(textPanel)

            addMouseListener(object : java.awt.event.MouseAdapter() {
                override fun mouseClicked(e: java.awt.event.MouseEvent) {
                    selectMethod(method)
                }
            })
        }

        return row
    }

    private fun selectMethod(method: OutputItemRecipes, skipCallback: Boolean = false) {
        selectedMethod = method
        rowPanels.forEach { (m, panel) ->
            panel.background = if (m == method) {
                ColorScheme.DARKER_GRAY_HOVER_COLOR
            } else {
                ColorScheme.DARKER_GRAY_COLOR
            }
        }
        revalidate()
        repaint()
        if (!skipCallback) {
            onMethodSelected()
        }
    }
}
