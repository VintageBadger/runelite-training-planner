package vintagebadger.trainingplanner.components.core

import net.runelite.client.util.ImageUtil
import java.awt.Color
import javax.swing.ImageIcon

private const val ICON_BASE_PATH = "/vintagebadger/trainingplanner/icons/"
const val ICON_SIZE = 24

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

    val imageIcon by lazy {
        var bufferedImage = image

        if (color != null) {
            bufferedImage = ImageUtil.recolorImage(bufferedImage, color)
        }

        if (rotation != null) {
            bufferedImage = ImageUtil.rotateImage(bufferedImage, Math.toRadians(rotation))
        }

        ImageIcon(bufferedImage)
    }
}