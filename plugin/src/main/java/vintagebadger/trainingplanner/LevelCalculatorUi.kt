package vintagebadger.trainingplanner

import net.runelite.api.Experience
import net.runelite.client.ui.DynamicGridLayout
import java.awt.GridLayout
import java.text.NumberFormat
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.AbstractDocument
import javax.swing.text.AttributeSet
import javax.swing.text.DocumentFilter

class LevelCalculatorUi : JPanel() {
    val inputPanel = InputPanel()
    val expRequiredLabel = JLabel()

    var onExpRequiredChanged: ((Int?) -> Unit)? = null

    var expRequired: Int? = null
        private set(value) {
            if (field == value) return
            field = value
            setExpRequiredLabel(value)
            onExpRequiredChanged?.invoke(value)
        }

    init {
        layout = DynamicGridLayout(0, 1, 0, 3)
        add(inputPanel)
        add(expRequiredLabel)
        setExpRequiredLabel(expRequired)

        val listener = OnChangeListener { handleCalculate() }
        inputPanel.startTextField.document.addDocumentListener(listener)
        inputPanel.endTextField.document.addDocumentListener(listener)
    }

    fun setExpRequiredLabel(expRequired: Int?) {
        val formattedExperience = if (expRequired == null) {
            "Lots!"
        } else {
            NumberFormat.getNumberInstance().format(expRequired)
        }

        expRequiredLabel.text = "Exp Required: $formattedExperience"
    }

    fun getStartLevel() = inputPanel.startTextField.text.toIntOrNull()

    fun getEndLevel() = inputPanel.endTextField.text.toIntOrNull()

    private fun handleCalculate() {
        val startLevel = getStartLevel() ?: return
        val endLevel = getEndLevel() ?: return

        val startExperience = Experience.getXpForLevel(startLevel)
        val endExperience = Experience.getXpForLevel(endLevel)

        expRequired = endExperience - startExperience
    }

    class InputPanel : JPanel() {
        val startTextField = JTextField()
        val endTextField = JTextField()

        init {
            layout = GridLayout(2, 2, 7, 7)

            val filter = LevelCapFilter()
            (startTextField.document as AbstractDocument).documentFilter = filter
            (endTextField.document as AbstractDocument).documentFilter = filter

            add(JLabel("Start Level"))
            add(startTextField)

            add(JLabel("End Level"))
            add(endTextField)
        }
    }

    private class OnChangeListener(private val onChange: () -> Unit) : DocumentListener {
        override fun insertUpdate(e: DocumentEvent) = onChange()
        override fun removeUpdate(e: DocumentEvent) = onChange()
        override fun changedUpdate(e: DocumentEvent) = onChange()
    }

    private class LevelCapFilter : DocumentFilter() {
        override fun replace(fb: FilterBypass, offset: Int, length: Int, text: String, attrs: AttributeSet?) {
            val current = fb.document.getText(0, fb.document.length)
            val newValue = current.substring(0, offset) + text + current.substring(offset + length)

            if (newValue.isEmpty()) {
                super.replace(fb, offset, length, text, attrs)
                return
            }

            val parsed = newValue.toIntOrNull()
            if (parsed != null && parsed > 126) {
                super.replace(fb, 0, fb.document.length, "126", attrs)
            } else {
                super.replace(fb, offset, length, text, attrs)
            }
        }

        override fun insertString(fb: FilterBypass, offset: Int, string: String, attrs: AttributeSet?) {
            replace(fb, offset, 0, string, attrs)
        }
    }
}
