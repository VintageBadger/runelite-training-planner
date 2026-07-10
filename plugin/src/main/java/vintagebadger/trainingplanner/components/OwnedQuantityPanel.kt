package vintagebadger.trainingplanner.components

import net.runelite.client.game.ItemManager
import net.runelite.client.ui.ColorScheme
import net.runelite.client.ui.DynamicGridLayout
import net.runelite.client.ui.FontManager
import vintagebadger.trainingplanner.data.ResolvedRecipeGraph
import java.awt.Dimension
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.border.EmptyBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.AbstractDocument
import javax.swing.text.AttributeSet
import javax.swing.text.DocumentFilter

class OwnedQuantityPanel(
    private val itemManager: ItemManager,
    private val onSnapshotRequested: () -> Unit,
    private val onChanged: () -> Unit,
) : JPanel() {
    private val rowsPanel = JPanel()
    private val snapshotButton = JButton("Use Bank + Inventory")
    private val statusLabel = JLabel()
    private val fields = linkedMapOf<Int, JTextField>()
    private val knownQuantities = mutableMapOf<Int, Long>()
    private var displayedItemIds: List<Int> = emptyList()
    private var updating = false

    init {
        layout = DynamicGridLayout(0, 1, 0, 3)
        background = ColorScheme.DARK_GRAY_COLOR
        add(JLabel("Owned Items").apply {
            font = FontManager.getRunescapeBoldFont()
        })
        snapshotButton.addActionListener { onSnapshotRequested() }
        add(snapshotButton)
        statusLabel.font = FontManager.getRunescapeSmallFont()
        statusLabel.foreground = ColorScheme.LIGHT_GRAY_COLOR
        add(statusLabel)
        rowsPanel.layout = DynamicGridLayout(0, 1, 0, 2)
        rowsPanel.background = ColorScheme.DARK_GRAY_COLOR
        add(rowsPanel)
        isVisible = false
    }

    fun setGraph(graph: ResolvedRecipeGraph, initialQuantities: Map<Int, Long> = emptyMap()) {
        val itemIds = graph.topologicalOrder.filter { it != graph.rootItemId }
        if (itemIds == displayedItemIds) return

        syncVisibleQuantities()
        initialQuantities.forEach { (itemId, quantity) ->
            if (quantity > 0L) knownQuantities[itemId] = quantity
        }
        displayedItemIds = itemIds
        rowsPanel.removeAll()
        fields.clear()
        itemIds.forEach { itemId ->
            val node = graph.nodes.getValue(itemId)
            val field = JTextField(8).apply {
                maximumSize = Dimension(90, preferredSize.height)
                (document as AbstractDocument).documentFilter = QuantityFilter()
                document.addDocumentListener(object : DocumentListener {
                    override fun insertUpdate(e: DocumentEvent) = notifyChanged()
                    override fun removeUpdate(e: DocumentEvent) = notifyChanged()
                    override fun changedUpdate(e: DocumentEvent) = notifyChanged()
                })
            }
            fields[itemId] = field
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
                add(field)
            })
        }
        updateVisibleFields()
        isVisible = itemIds.isNotEmpty()
        revalidate()
        repaint()
    }

    fun clearGraph() {
        syncVisibleQuantities()
        displayedItemIds = emptyList()
        fields.clear()
        rowsPanel.removeAll()
        statusLabel.text = ""
        isVisible = false
        revalidate()
        repaint()
    }

    fun getOwnedQuantities(): Map<Int, Long> {
        syncVisibleQuantities()
        return knownQuantities.toMap()
    }

    fun setOwnedQuantities(quantities: Map<Int, Long>) {
        displayedItemIds.forEach { itemId ->
            val quantity = quantities[itemId]?.coerceAtLeast(0L) ?: 0L
            if (quantity > 0L) {
                knownQuantities[itemId] = quantity
            } else {
                knownQuantities.remove(itemId)
            }
        }
        updateVisibleFields()
    }

    fun showBankUnavailable() {
        statusLabel.text = "Open your bank before taking a snapshot."
    }

    fun showSnapshotCaptured() {
        statusLabel.text = "Bank and inventory quantities loaded."
    }

    private fun notifyChanged() {
        if (!updating) {
            syncVisibleQuantities()
            onChanged()
        }
    }

    private fun syncVisibleQuantities() {
        fields.forEach { (itemId, field) ->
            val quantity = field.text.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
            if (quantity > 0L) {
                knownQuantities[itemId] = quantity
            } else {
                knownQuantities.remove(itemId)
            }
        }
    }

    private fun updateVisibleFields() {
        updating = true
        fields.forEach { (itemId, field) ->
            field.text = knownQuantities[itemId]?.toString().orEmpty()
        }
        updating = false
    }

    private class QuantityFilter : DocumentFilter() {
        override fun replace(fb: FilterBypass, offset: Int, length: Int, text: String, attrs: AttributeSet?) {
            val current = fb.document.getText(0, fb.document.length)
            val replacement = current.substring(0, offset) + text + current.substring(offset + length)
            if (replacement.isEmpty() || replacement.toLongOrNull() != null) {
                super.replace(fb, offset, length, text, attrs)
            }
        }

        override fun insertString(fb: FilterBypass, offset: Int, string: String, attrs: AttributeSet?) {
            replace(fb, offset, 0, string, attrs)
        }
    }
}
