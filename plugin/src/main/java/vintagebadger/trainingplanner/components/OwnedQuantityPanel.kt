package vintagebadger.trainingplanner.components

import net.runelite.client.game.ItemManager
import net.runelite.client.ui.ColorScheme
import net.runelite.client.ui.DynamicGridLayout
import net.runelite.client.ui.FontManager
import vintagebadger.trainingplanner.data.OwnedQuantitySnapshot
import vintagebadger.trainingplanner.data.ResolvedRecipeGraph
import java.text.NumberFormat
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.border.EmptyBorder

class OwnedQuantityPanel(
    private val itemManager: ItemManager,
) : JPanel() {
    private val rowsPanel = JPanel()
    private val statusLabel = JLabel()
    private val quantityLabels = linkedMapOf<Int, JLabel>()
    private var displayedItemIds: List<Int> = emptyList()
    private var snapshot = OwnedQuantitySnapshot(emptyMap(), includesBank = false)

    init {
        layout = DynamicGridLayout(0, 1, 0, 3)
        background = ColorScheme.DARK_GRAY_COLOR
        add(JLabel("Owned Items").apply {
            font = FontManager.getRunescapeBoldFont()
        })
        statusLabel.font = FontManager.getRunescapeSmallFont()
        statusLabel.foreground = ColorScheme.LIGHT_GRAY_COLOR
        add(statusLabel)
        rowsPanel.layout = DynamicGridLayout(0, 1, 0, 2)
        rowsPanel.background = ColorScheme.DARK_GRAY_COLOR
        add(rowsPanel)
        updateStatus()
        isVisible = false
    }

    fun setGraph(graph: ResolvedRecipeGraph) {
        val itemIds = graph.topologicalOrder.filter { it != graph.rootItemId }
        if (itemIds == displayedItemIds) return

        displayedItemIds = itemIds
        rowsPanel.removeAll()
        quantityLabels.clear()
        itemIds.forEach { itemId ->
            val node = graph.nodes.getValue(itemId)
            val quantityLabel = JLabel().apply {
                font = FontManager.getRunescapeSmallFont()
                foreground = ColorScheme.TEXT_COLOR
            }
            quantityLabels[itemId] = quantityLabel
            rowsPanel.add(JPanel().apply {
                layout = BoxLayout(this, BoxLayout.X_AXIS)
                background = ColorScheme.DARK_GRAY_COLOR
                border = EmptyBorder(2, 0, 2, 0)
                if (itemId > 0) add(getItemIcon(itemId, itemManager, leftPadding = 0))
                add(JLabel(node.itemName).apply {
                    font = FontManager.getRunescapeSmallFont()
                    foreground = ColorScheme.LIGHT_GRAY_COLOR
                    border = EmptyBorder(0, 4, 0, 8)
                })
                add(Box.createHorizontalGlue())
                add(quantityLabel)
            })
        }
        updateVisibleQuantities()
        isVisible = itemIds.isNotEmpty()
        revalidate()
        repaint()
    }

    fun clearGraph() {
        displayedItemIds = emptyList()
        quantityLabels.clear()
        rowsPanel.removeAll()
        isVisible = false
        revalidate()
        repaint()
    }

    fun setSnapshot(snapshot: OwnedQuantitySnapshot) {
        this.snapshot = snapshot
        updateStatus()
        updateVisibleQuantities()
    }

    fun getOwnedQuantities(): Map<Int, Long> = snapshot.quantities

    private fun updateStatus() {
        statusLabel.text = if (snapshot.includesBank) {
            "Automatically using bank and inventory."
        } else {
            "Using inventory. Open your bank once to include bank items."
        }
    }

    private fun updateVisibleQuantities() {
        quantityLabels.forEach { (itemId, label) ->
            label.text = NumberFormat.getNumberInstance().format(snapshot.quantities[itemId] ?: 0L)
        }
        revalidate()
        repaint()
    }
}
