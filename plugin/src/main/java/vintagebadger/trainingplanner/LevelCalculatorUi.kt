package vintagebadger.trainingplanner

import net.runelite.api.Experience
import net.runelite.client.ui.DynamicGridLayout
import java.awt.CardLayout
import java.awt.GridLayout
import java.text.NumberFormat
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.AbstractDocument
import javax.swing.text.AttributeSet
import javax.swing.text.DocumentFilter

data class XpTarget(
    val startLevel: Int,
    val endLevel: Int,
    val startXp: Long,
    val targetXp: Long,
) {
    val xpRequired: Long
        get() = maxOf(0L, targetXp - startXp)
}

private enum class XpInputMode(private val label: String) {
    LEVELS("Levels"),
    EXPERIENCE("Experience"),
    ;

    override fun toString(): String = label
}

class LevelCalculatorUi : JPanel() {
    private val modeDropdown = JComboBox(XpInputMode.entries.toTypedArray())
    private val cardLayout = CardLayout()
    private val inputCards = JPanel(cardLayout)
    private val levelInputs = LevelInputPanel()
    private val xpInputs = XpInputPanel()
    private val expRequiredLabel = JLabel()
    private var updatingFields = false

    var onTargetChanged: ((XpTarget?) -> Unit)? = null

    var target: XpTarget? = null
        private set(value) {
            if (field == value) return
            field = value
            updateRequiredLabel(value?.xpRequired)
            onTargetChanged?.invoke(value)
        }

    val expRequired: Long?
        get() = target?.xpRequired

    init {
        layout = DynamicGridLayout(0, 1, 0, 3)
        inputCards.add(levelInputs, XpInputMode.LEVELS.name)
        inputCards.add(xpInputs, XpInputMode.EXPERIENCE.name)
        add(modeDropdown)
        add(inputCards)
        add(expRequiredLabel)
        updateRequiredLabel(null)

        val listener = OnChangeListener {
            if (!updatingFields) calculateTarget()
        }
        levelInputs.startField.document.addDocumentListener(listener)
        levelInputs.endField.document.addDocumentListener(listener)
        xpInputs.startField.document.addDocumentListener(listener)
        xpInputs.endField.document.addDocumentListener(listener)
        modeDropdown.addActionListener { switchMode() }
    }

    fun getStartLevel(): Int? = target?.startLevel

    fun getEndLevel(): Int? = target?.endLevel

    fun getStartXp(): Long? = target?.startXp

    fun getTargetXp(): Long? = target?.targetXp

    fun clear() {
        updatingFields = true
        modeDropdown.selectedItem = XpInputMode.LEVELS
        levelInputs.startField.text = ""
        levelInputs.endField.text = ""
        xpInputs.startField.text = ""
        xpInputs.endField.text = ""
        cardLayout.show(inputCards, XpInputMode.LEVELS.name)
        updatingFields = false
        target = null
    }

    private fun switchMode() {
        if (updatingFields) return
        val mode = modeDropdown.selectedItem as XpInputMode
        updatingFields = true
        target?.let { current ->
            when (mode) {
                XpInputMode.LEVELS -> {
                    levelInputs.startField.text = current.startLevel.toString()
                    levelInputs.endField.text = current.endLevel.toString()
                }
                XpInputMode.EXPERIENCE -> {
                    xpInputs.startField.text = current.startXp.toString()
                    xpInputs.endField.text = current.targetXp.toString()
                }
            }
        }
        cardLayout.show(inputCards, mode.name)
        updatingFields = false
        calculateTarget()
    }

    private fun calculateTarget() {
        target = when (modeDropdown.selectedItem as XpInputMode) {
            XpInputMode.LEVELS -> targetFromLevels()
            XpInputMode.EXPERIENCE -> targetFromExperience()
        }
    }

    private fun targetFromLevels(): XpTarget? {
        val start = levelInputs.startField.text.toIntOrNull()
            ?.takeIf { it in 1..Experience.MAX_VIRT_LEVEL }
            ?: return null
        val end = levelInputs.endField.text.toIntOrNull()
            ?.takeIf { it in 1..Experience.MAX_VIRT_LEVEL }
            ?: return null
        return XpTarget(
            startLevel = start,
            endLevel = end,
            startXp = Experience.getXpForLevel(start).toLong(),
            targetXp = Experience.getXpForLevel(end).toLong(),
        )
    }

    private fun targetFromExperience(): XpTarget? {
        val start = xpInputs.startField.text.toLongOrNull() ?: return null
        val end = xpInputs.endField.text.toLongOrNull() ?: return null
        return XpTarget(
            startLevel = Experience.getLevelForXp(start.toInt()),
            endLevel = Experience.getLevelForXp(end.toInt()),
            startXp = start,
            targetXp = end,
        )
    }

    private fun updateRequiredLabel(expRequired: Long?) {
        val formatted = expRequired?.let(NumberFormat.getNumberInstance()::format) ?: "-"
        expRequiredLabel.text = "XP Required: $formatted"
    }

    private class LevelInputPanel : JPanel() {
        val startField = numberField(Experience.MAX_VIRT_LEVEL.toLong())
        val endField = numberField(Experience.MAX_VIRT_LEVEL.toLong())

        init {
            layout = GridLayout(2, 2, 7, 7)
            add(JLabel("Start Level"))
            add(startField)
            add(JLabel("Target Level"))
            add(endField)
        }
    }

    private class XpInputPanel : JPanel() {
        val startField = numberField(Experience.MAX_SKILL_XP.toLong())
        val endField = numberField(Experience.MAX_SKILL_XP.toLong())

        init {
            layout = GridLayout(2, 2, 7, 7)
            add(JLabel("Start XP"))
            add(startField)
            add(JLabel("Target XP"))
            add(endField)
        }
    }

    private class OnChangeListener(private val onChange: () -> Unit) : DocumentListener {
        override fun insertUpdate(e: DocumentEvent) = onChange()
        override fun removeUpdate(e: DocumentEvent) = onChange()
        override fun changedUpdate(e: DocumentEvent) = onChange()
    }

    private class BoundedNumberFilter(private val maximum: Long) : DocumentFilter() {
        override fun replace(fb: FilterBypass, offset: Int, length: Int, text: String, attrs: AttributeSet?) {
            val current = fb.document.getText(0, fb.document.length)
            val replacement = current.substring(0, offset) + text + current.substring(offset + length)
            if (replacement.isEmpty()) {
                super.replace(fb, offset, length, text, attrs)
                return
            }
            val value = replacement.toLongOrNull() ?: return
            val accepted = minOf(value, maximum).toString()
            super.replace(fb, 0, fb.document.length, accepted, attrs)
        }

        override fun insertString(fb: FilterBypass, offset: Int, string: String, attrs: AttributeSet?) {
            replace(fb, offset, 0, string, attrs)
        }
    }

    private companion object {
        fun numberField(maximum: Long) = JTextField().apply {
            (document as AbstractDocument).documentFilter = BoundedNumberFilter(maximum)
        }
    }
}
