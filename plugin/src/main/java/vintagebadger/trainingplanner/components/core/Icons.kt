package vintagebadger.trainingplanner.components.core

import net.runelite.client.util.ImageUtil
import java.awt.Color
import java.awt.Dimension
import javax.swing.ImageIcon
import javax.swing.JButton

private const val ICON_SIZE = 24
private const val ICON_BASE_PATH = "/vintagebadger/trainingplanner/icons/"

/**
 * @param iconRes icon name
 * @param color optional icon tint
 * @param rotation optional icon rotation in degrees
 */
enum class Icon(val iconRes: String, color: Color? = null, rotation: Double? = null) {
    Edit("edit.png"),
    ChevronRight("chevron.png"),
    ChevronDown("chevron.png", rotation = 90.0),
    Delete("delete.png", color = Color.RED);

    private val image by lazy {
        ImageUtil.loadImageResource(Icon::class.java, "$ICON_BASE_PATH$iconRes")
    }

    private val imageIcon by lazy {
        var bufferedImage = image

        if (color != null) {
            bufferedImage = ImageUtil.recolorImage(bufferedImage, color)
        }

        if (rotation != null) {
            bufferedImage = ImageUtil.rotateImage(bufferedImage, Math.toRadians(rotation))
        }

        ImageIcon(bufferedImage)
    }

    fun button() = JButton(imageIcon).apply {
        preferredSize = Dimension(ICON_SIZE, ICON_SIZE)
    }
}