package vintagebadger.trainingplanner

import net.runelite.api.Experience
import net.runelite.client.ui.DynamicGridLayout
import java.awt.GridLayout
import java.text.NumberFormat
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField

class LevelCalculatorUi : JPanel() {
    val inputPanel = InputPanel()
    val expRequiredLabel = JLabel()

    private var expRequired: Int? = null

    init {
        layout = DynamicGridLayout(0, 1, 0, 3)
        add(inputPanel)
        add(expRequiredLabel)
        setExpRequiredLabel(expRequired)

        val calculateButton = JButton("Calculate").apply {
            addActionListener { handleCalculate() }
        }
        add(calculateButton)
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

        val expRequired = endExperience - startExperience
        setExpRequiredLabel(expRequired)
    }

    class InputPanel : JPanel() {
        val startTextField = JTextField()
        val endTextField = JTextField()

        init {
            layout = GridLayout(2, 2, 7, 7)

            add(JLabel("Start Level"))
            add(startTextField)

            add(JLabel("End Level"))
            add(endTextField)
        }
    }
}