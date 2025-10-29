package com.egeereyzee.calculator

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Button

class MainActivity : AppCompatActivity() {
    private lateinit var textViewMain: TextView
    private lateinit var textViewMini: TextView

    private lateinit var buttonZero: Button
    private lateinit var buttonOne: Button
    private lateinit var buttonTwo: Button
    private lateinit var buttonThree: Button
    private lateinit var buttonFour: Button
    private lateinit var buttonFive: Button
    private lateinit var buttonSix: Button
    private lateinit var buttonSeven: Button
    private lateinit var buttonEight: Button
    private lateinit var buttonNine: Button

    private lateinit var buttonPlus: Button
    private lateinit var buttonMinus: Button
    private lateinit var buttonMultiply: Button
    private lateinit var buttonDivide: Button

    private lateinit var buttonEqual: Button
    private lateinit var buttonClear: Button
    private lateinit var buttonDot: Button
    private lateinit var buttonBackspace: Button
    private lateinit var buttonPlusMinus: Button
    private val numbers = arrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
    private var line: String = "0"
    private var num1: String = "0"
    private var num2: String = "0"
    private var oper: String = ""
    private var result: Double = 0.0
    private var nof: Int = 0
    private var numb1: Double = 0.0
    private var numb2: Double = 0.0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeViews()
        setupAllButtons()

    }

    private fun initializeViews() {
        textViewMain = findViewById(R.id.textViewMain)
        textViewMini = findViewById(R.id.textViewMini)

        buttonZero = findViewById(R.id.ButtonZero)
        buttonOne = findViewById(R.id.ButtonOne)
        buttonTwo = findViewById(R.id.ButtonTwo)
        buttonThree = findViewById(R.id.ButtonThree)
        buttonFour = findViewById(R.id.ButtonFour)
        buttonFive = findViewById(R.id.ButtonFive)
        buttonSix = findViewById(R.id.ButtonSix)
        buttonSeven = findViewById(R.id.ButtonSeven)
        buttonEight = findViewById(R.id.ButtonEight)
        buttonNine = findViewById(R.id.ButtonNine)

        buttonPlus = findViewById(R.id.ButtonPlus)
        buttonMinus = findViewById(R.id.ButtonMinus)
        buttonMultiply = findViewById(R.id.ButtonMultiply)
        buttonDivide = findViewById(R.id.ButtonDivide)

        buttonEqual = findViewById(R.id.ButtonEqual)
        buttonClear = findViewById(R.id.ButtonClear)
        buttonDot = findViewById(R.id.ButtonDot)
        buttonBackspace = findViewById(R.id.ButtonBackspace)
        buttonPlusMinus = findViewById(R.id.ButtonPlusMinus)
    }

    private fun setupAllButtons() {
        buttonZero.setOnClickListener { addNumber("0")
                                        updateDisplay() }
        buttonOne.setOnClickListener { addNumber("1")
                                        updateDisplay()}
        buttonTwo.setOnClickListener { addNumber("2")
                                        updateDisplay()}
        buttonThree.setOnClickListener { addNumber("3")
                                        updateDisplay()}
        buttonFour.setOnClickListener { addNumber("4")
                                        updateDisplay()}
        buttonFive.setOnClickListener { addNumber("5")
                                        updateDisplay()}
        buttonSix.setOnClickListener { addNumber("6")
                                        updateDisplay()}
        buttonSeven.setOnClickListener { addNumber("7")
                                        updateDisplay()}
        buttonEight.setOnClickListener { addNumber("8")
                                        updateDisplay()}
        buttonNine.setOnClickListener { addNumber("9")
                                        updateDisplay()}

        buttonPlus.setOnClickListener { replaceOperand("+")}
        buttonMinus.setOnClickListener { replaceOperand("-") }
        buttonMultiply.setOnClickListener { replaceOperand("×") }
        buttonDivide.setOnClickListener { replaceOperand("÷") }

        buttonEqual.setOnClickListener { calculateResult() }
        buttonClear.setOnClickListener { line = "0"
                                         updateDisplay()}
        buttonDot.setOnClickListener { addDecimalPoint() }
        buttonBackspace.setOnClickListener {
                                             if (line.length == 1 || line.length == 0) {line = "0"}
                                             else if (line.last() == ' ') {line = line.dropLast(3)}
                                             else if (line.last() == 'y') {line = "0" + line.dropLast(8)}
                                             else if (line.length > 1) {line = line.dropLast(1)}

                                             updateDisplay() }
        buttonPlusMinus.setOnClickListener { toggleSign() }
    }

    private fun addNumber(number: String) {
        if (line.length == 1 && line[0] == '0') {
            line = number
        }
        else if (line.last() != 'y'){
            line += number
        }
    }
    private fun replaceOperand(operand: String) {
        if (line.length > 2) {
            if (!("E-" in line) && (line[line.length - 2] == '+' || line[line.length - 2] == '÷' || line[line.length - 2] == '×' || line[line.length - 2] == '-')) {
                line = line.dropLast(2) + operand + " "
            }
            else if (("E-" in line && line.count { it == ' '} < 2) || !("÷" in line || "×" in line || "+" in line || ("-" in line && line[0] != '-') || line.count { it == '-'} == 2)) {
                line = line + " " + operand + " "
            }
        }
        if (line.length <= 2 && line.length > 0 && !(line.length == 1 && line[0] == '-')) {
            line = line + " " + operand + " "
        }
        updateDisplay()
    }
    private fun updateDisplay() {
        textViewMain.text = line
    }

    private fun addDecimalPoint() {
        if (line.count { it == '.' } == 0 && line.last() in numbers) {
            line += "."
        }
        else if (line.count { it == '.' } == 1) {
            if (line.last() == ' ') {
                line += "0."
            }
            else if (("÷" in line || "×" in line || "+" in line || ("-" in line && line[0] != '-') && line.last() in numbers)) {
                line += "."
            }
        }
        updateDisplay()
    }

    private fun toggleSign() {
        if (!("÷" in line || "×" in line || "+" in line || ("-" in line && line[0] != '-'))) {
            if (line.isEmpty()) {
                line = "-0"
            }
            else if (line[0] == '-') {
                line = line.drop(1)
            }
            else {
                line = "-" + line
            }
        }
        else {
            if ("+" in line) {
                line = line.replace('+', '-')
            }
            else if ("- " in line) {
                line = line.replace("- ", "+ ")
            }
            else if ("÷" in line) {
                if ("÷ -" in line) {
                    line = line.replace("÷ -", "÷ ")
                }
                else if ("÷ " in line) {
                    line = line.replace("÷ ", "÷ -")
                }
            }
            else if ("×" in line) {
                if ("× -" in line) {
                    line = line.replace("× -", "× ")
                }
                else if ("× " in line) {
                    line = line.replace("× ", "× -")
                }
            }
        }
        updateDisplay()
    }

    private fun calculateResult() {
        if (line.last() == '-' || line.last() == ' ' || line.isEmpty()) {
            return
        }
        else if (line.count({ it == ' ' }) == 0) {
            if (line.toDouble() % 1 == 0.0 && line.toDouble() < 2147483648 && line.toDouble() > -2147483649) {
                textViewMini.text = line
                return
            }
            textViewMini.text = line
            return
        }
        else if ("E " in line || "E- " in line) {
            line = line.replace("E ", "E0 ")
            line = line.replace("E- ", "E0 ")
        }
        num1 = ""
        num2 = ""
        oper = ""
        nof = 0
        for (i in 0 until line.length) {
            if (line[i] == ' ') {
                nof += 1
            }
            else {
                if (nof == 0) {
                    num1 += line[i]
                }
                else if (nof == 1) {
                    oper += line[i]
                }
                else if (nof == 2) {
                    num2 += line[i]
                }
            }
        }

        numb1 = num1.toDouble()
        numb2 = num2.toDouble()

        if (oper == "+") {
            result = numb1 + numb2
        }
        else if (oper == "-") {
            result = numb1 - numb2
        }
        else if (oper == "×") {
            result = numb1 * numb2
        }
        else if (oper == "÷") {
            if (num2.toDouble() != 0.0) {
                result = numb1 / numb2
            }
            else {
                textViewMain.text = "Div 0 error"
            }
        }
        if (result % 1 == 0.0 && result < 2147483648 && result > -2147483649) {
            textViewMini.text = (result.toInt()).toString()
            line = (result.toInt()).toString()
        }
        else {
            textViewMini.text = result.toString()
            line = result.toString()
        }
    }
}