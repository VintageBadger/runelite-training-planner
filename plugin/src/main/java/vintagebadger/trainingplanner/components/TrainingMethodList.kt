package vintagebadger.trainingplanner.components

import net.runelite.client.game.ItemManager
import net.runelite.client.ui.ColorScheme
import net.runelite.client.ui.DynamicGridLayout
import net.runelite.client.ui.FontManager
import vintagebadger.trainingplanner.models.TrainingMethod
import java.text.NumberFormat
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.border.EmptyBorder
import kotlin.math.ceil

class TrainingMethodList(
    private val itemManager: ItemManager,
    private val onMethodSelected: () -> Unit,
) : JPanel() {

    private var selectedMethod: TrainingMethod? = null
    private val rowPanels = mutableMapOf<TrainingMethod, JPanel>()

    fun getSelectedMethod(): TrainingMethod? = selectedMethod

    fun setMethods(methods: List<TrainingMethod>, expRequired: Int?) {
        val previouslySelected = selectedMethod?.name
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
            val row = createRow(method, expRequired)
            rowPanels[method] = row
            add(row)
        }

        previouslySelected?.let { name ->
            methods.find { it.name == name }?.let { selectMethod(it, skipCallback = true) }
        }

        revalidate()
        repaint()
    }

    private fun createRow(method: TrainingMethod, expRequired: Int?): JPanel {
        val row = JPanel().apply {
            layout = javax.swing.BoxLayout(this, javax.swing.BoxLayout.X_AXIS)
            background = ColorScheme.DARKER_GRAY_COLOR
            border = EmptyBorder(4, 8, 4, 8)
            isOpaque = true

            val outputId = method.output.firstOrNull()?.id
            if (outputId != null && outputId > 0) {
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

            val itemName = method.output.firstOrNull()?.name ?: "Unknown"
            val craftCount = if (expRequired != null && expRequired > 0 && method.totalXp > 0) {
                ceil(expRequired / method.totalXp).toLong()
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

            val subLabel = JLabel("$itemName$craftText").apply {
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

    private fun selectMethod(method: TrainingMethod, skipCallback: Boolean = false) {
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
