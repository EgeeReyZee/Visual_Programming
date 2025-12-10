package com.egeereyzee.multiapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import kotlin.random.Random
import android.widget.TextView
import android.widget.Button

class CalculatorActivity : AppCompatActivity() {
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
        setContentView(R.layout.activity_calculator)

        initializeViews()
        setupAllButtons()

    }

    private fun StringLastChar(string: String): Char {
        var char: Char = ' '
        for (i in string.indices) {
            char = string[i]
        }
        return char
    }

    private fun countOfCharInString(string: String, char: Char): Int {
        var counter: Int = 0
        for (i in string.indices) {
            if (string[i] == char) { counter++ }
        }
        return counter
    }

    private fun lengthOfString(string: String): Int {
        var counter: Int = 0
        for (i in string.indices) {
            counter++
        }
        return counter
    }

    private fun removeSymbolsFromStringFromStart(string: String, int: Int): String {
        var newString: String = ""
        for (i in string.indices) {
            if (i > int-1) {
                newString += string[i]
            }
        }
        return newString
    }

    private fun removeSymbolsFromStringFromEnd(string: String, int: Int): String {
        var newString: String = ""
        val LengthOfString: Int = lengthOfString(string)
        for (i in string.indices) {
            if (i < LengthOfString - int) {
                newString += string[i]
            }
            else {
                break
            }
        }
        return newString
    }

    private fun replaceFromStringOneStringToAnotherString(string: String, OneString: String, AnotherString: String, int: Int): String {
        var newString: String = ""
        var tempString: String = ""
        var counter: Int = 0
        var k: Int = 0
        val LengthOfOneString: Int = lengthOfString(OneString)
        for (i in string.indices) {
            if (k > 0) {
                k--
                continue
            }
            if (string[i] == OneString[0] && counter < int) {
                for (j in OneString.indices) {
                    if (string[i+j] != OneString[j]) {
                        newString += tempString
                        tempString = ""
                        break
                    }
                    tempString += string[i+j]
                    if (j == LengthOfOneString - 1) {
                        newString += AnotherString
                        k = j
                        counter++
                    }
                }
            }
            else {
                newString += string[i]
            }
        }
        return newString
    }

    private fun IndexOfCharInString(string: String, char: Char): Int {
        var int: Int = 0
        for (i in string.indices) {
            if (string[i] == char) {
                break
            }
            int++
        }
        return int
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
            if (lengthOfString(line) == 1 || lengthOfString(line) == 0) {line = "0"}
            else if (StringLastChar(line) == ' ') {line = removeSymbolsFromStringFromEnd(line, 3)}
            else if (StringLastChar(line) == 'y') {line = "0" + removeSymbolsFromStringFromEnd(line, 8)}
            else if (lengthOfString(line) > 1) {line = removeSymbolsFromStringFromEnd(line, 1)}

            updateDisplay() }
        buttonPlusMinus.setOnClickListener { toggleSign() }
    }

    private fun addNumber(number: String) {
        if (lengthOfString(line) == 1 && line[0] == '0') {
            line = number
        }
        else if (StringLastChar(line) != 'y'){
            line += number
        }
    }
    private fun replaceOperand(operand: String) {
        if (line.length > 2) {
            if (!("E-" in line) && (line[lengthOfString(line) - 2] == '+' || line[lengthOfString(line) - 2] == '÷' || line[lengthOfString(line) - 2] == '×' || line[lengthOfString(line) - 2] == '-')) {
                line = removeSymbolsFromStringFromEnd(line, 2) + operand + " "
            }
            else if (("E-" in line && countOfCharInString(line, ' ') < 2) || !("÷" in line || "×" in line || "+" in line || ("-" in line && line[0] != '-') || countOfCharInString(line, '-') == 2)) {
                line = line + " " + operand + " "
            }
        }
        if (lengthOfString(line) <= 2 && lengthOfString(line) > 0 && !(lengthOfString(line) == 1 && line[0] == '-')) {
            line = line + " " + operand + " "
        }
        updateDisplay()
    }
    private fun updateDisplay() {
        textViewMain.text = line
    }

    private fun addDecimalPoint() {
        if (countOfCharInString(line, '.') == 0 && StringLastChar(line) in numbers) {
            line += "."
        }
        else if (countOfCharInString(line, '.') == 1) {
            if (StringLastChar(line) == ' ') {
                line += "0."
            }
            else if ((("÷" in line || "×" in line || "+" in line || ("-" in line && line[0] != '-')) && StringLastChar(line) in numbers && IndexOfCharInString(line, '.') < IndexOfCharInString(line, ' '))) {
                line += "."
            }
        }
        updateDisplay()
    }

    private fun toggleSign() {
        if (!("÷" in line || "×" in line || "+" in line || ("-" in line && line[0] != '-'))) {
            if (lengthOfString(line) == 0) {
                line = "-0"
            }
            else if (line[0] == '-') {
                line = removeSymbolsFromStringFromStart(line, 1)
            }
            else {
                line = "-" + line
            }
        }
        else {
            if ("+" in line) {
                line = replaceFromStringOneStringToAnotherString(line, "+", "-", 1)
            }
            else if ("- " in line) {
                line = replaceFromStringOneStringToAnotherString(line, "-", "+", 1)
            }
            else if ("÷" in line) {
                if ("÷ -" in line) {
                    line = replaceFromStringOneStringToAnotherString(line, "÷ -", "÷ ", 1)
                }
                else if ("÷ " in line) {
                    line = replaceFromStringOneStringToAnotherString(line, "÷ ", "÷ -", 1)
                }
            }
            else if ("×" in line) {
                if ("× -" in line) {
                    line = replaceFromStringOneStringToAnotherString(line, "× -", "× ", 1)
                }
                else if ("× " in line) {
                    line = replaceFromStringOneStringToAnotherString(line, "× ", "× -", 1)
                }
            }
        }
        updateDisplay()
    }

    private fun calculateResult() {
        if (StringLastChar(line) == '-' || StringLastChar(line) == ' ' || lengthOfString(line) == 0) {
            return
        }
        else if (countOfCharInString(line, ' ') == 0) {
            if (line.toDouble() % 1 == 0.0 && line.toDouble() < 2147483648 && line.toDouble() > -2147483649) {
                textViewMini.text = line
                return
            }
            textViewMini.text = line
            return
        }
        else if ("E " in line || "E- " in line) {
            line = replaceFromStringOneStringToAnotherString(line, "E ", "E0 ", 1)
            line = replaceFromStringOneStringToAnotherString(line, "E- ", "E0 ", 1)
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