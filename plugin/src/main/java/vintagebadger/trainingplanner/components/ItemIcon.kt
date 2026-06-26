package vintagebadger.trainingplanner.components

import net.runelite.client.game.ItemManager
import javax.swing.JLabel
import javax.swing.border.EmptyBorder


fun getItemIcon(itemId: Int,
                itemManager: ItemManager,
                topPadding: Int = 0,
                leftPadding: Int = 0,
                bottomPadding: Int = 0,
                rightPadding: Int = 0,): JLabel {
    val iconLabel = JLabel().apply {
        border = EmptyBorder(topPadding, leftPadding, bottomPadding, rightPadding)
    }
    val img = itemManager.getImage(itemId)
    img.addTo(iconLabel)
    return iconLabel
}