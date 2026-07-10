package vintagebadger.trainingplanner.components

import net.runelite.client.game.ItemManager
import net.runelite.client.ui.ColorScheme
import net.runelite.client.ui.DynamicGridLayout
import net.runelite.client.ui.FontManager
import vintagebadger.trainingplanner.planning.PlanResult
import vintagebadger.trainingplanner.wiki.OutputItemRecipes
import java.text.NumberFormat
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.border.EmptyBorder

class TrainingMethodList(
    private val itemManager: ItemManager,
    private val onMethodSelected: () -> Unit,
) : JPanel() {
    private var selectedMethod: OutputItemRecipes? = null
    private var selectedMethodId: Int? = null
    private val rowPanels = mutableMapOf<Int, JPanel>()
    private val detailLabels = mutableMapOf<Int, JLabel>()
    private val methodNames = mutableMapOf<Int, String>()

    fun getSelectedMethod(): OutputItemRecipes? = selectedMethod

    fun setMethods(methods: List<OutputItemRecipes>) {
        val previouslySelected = selectedMethodId
        removeAll()
        rowPanels.clear()
        detailLabels.clear()
        methodNames.clear()
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
            val row = createRow(method)
            rowPanels[method.id] = row
            add(row)
        }
        previouslySelected?.let { id ->
            methods.find { it.id == id }?.let { selectMethod(it, skipCallback = true) }
        }
        revalidate()
        repaint()
    }

    fun setPlanResults(results: Map<Int, Result<PlanResult>>) {
        detailLabels.forEach { (itemId, label) ->
            val actionText = results[itemId]
                ?.getOrNull()
                ?.let { result ->
                    "${NumberFormat.getNumberInstance().format(result.rootActions)} actions"
                }
                ?: "Unavailable"
            label.text = "${methodNames[itemId] ?: "Unknown method"} - $actionText"
        }
        revalidate()
        repaint()
    }

    private fun createRow(output: OutputItemRecipes): JPanel {
        val methodName = output.methods.firstOrNull()?.method ?: "Unknown method"
        methodNames[output.id] = methodName
        val detailLabel = JLabel(methodName).apply {
            font = FontManager.getRunescapeSmallFont()
            foreground = ColorScheme.LIGHT_GRAY_COLOR
        }
        detailLabels[output.id] = detailLabel

        return JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            background = ColorScheme.DARKER_GRAY_COLOR
            border = EmptyBorder(4, 8, 4, 8)
            isOpaque = true

            if (output.id > 0) {
                val iconLabel = JLabel()
                itemManager.getImage(output.id).addTo(iconLabel)
                add(iconLabel)
            }
            add(JPanel().apply {
                layout = DynamicGridLayout(0, 1)
                background = ColorScheme.DARKER_GRAY_COLOR
                isOpaque = false
                add(JLabel(output.name).apply {
                    font = FontManager.getRunescapeBoldFont()
                    foreground = ColorScheme.TEXT_COLOR
                })
                add(detailLabel)
            })
            addMouseListener(object : java.awt.event.MouseAdapter() {
                override fun mouseClicked(e: java.awt.event.MouseEvent) = selectMethod(output)
            })
        }
    }

    private fun selectMethod(method: OutputItemRecipes, skipCallback: Boolean = false) {
        selectedMethod = method
        selectedMethodId = method.id
        rowPanels.forEach { (itemId, panel) ->
            panel.background = if (itemId == method.id) {
                ColorScheme.DARKER_GRAY_HOVER_COLOR
            } else {
                ColorScheme.DARKER_GRAY_COLOR
            }
        }
        revalidate()
        repaint()
        if (!skipCallback) onMethodSelected()
    }
}
