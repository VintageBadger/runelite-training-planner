package vintagebadger.trainingplanner.components.core

import net.runelite.client.util.SwingUtil.removeButtonDecorations
import java.awt.Dimension
import javax.swing.ImageIcon
import javax.swing.JButton

class IconButton(imageIcon: ImageIcon) : JButton() {
    init {
        icon = imageIcon
        preferredSize = Dimension(ICON_SIZE, ICON_SIZE)
        minimumSize = Dimension(ICON_SIZE, ICON_SIZE)
        maximumSize = Dimension(ICON_SIZE, ICON_SIZE)
    }
}

class IconToggleButton(
    initialSelected: Boolean,
    private val selectedIcon: ImageIcon,
    private val unselectedIcon: ImageIcon,
    private val onChanged: (Boolean) -> Unit,
) : JButton() {
    var selectedState: Boolean = initialSelected
        private set

    init {
        preferredSize = Dimension(ICON_SIZE, ICON_SIZE)
        minimumSize = Dimension(ICON_SIZE, ICON_SIZE)
        maximumSize = Dimension(ICON_SIZE, ICON_SIZE)
        icon = currentIcon()

        removeButtonDecorations(this)

        addActionListener {
            selectedState = !selectedState
            icon = currentIcon()
            onChanged(selectedState)
        }
    }

    private fun currentIcon(): ImageIcon =
        if (selectedState) selectedIcon else unselectedIcon
}

fun Icon.button() = IconButton(imageIcon)