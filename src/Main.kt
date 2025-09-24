import kotlin.random.Random
import kotlin.math.sqrt
import kotlin.math.pow
import java.util.Timer
import java.util.TimerTask

open class Human {
    var name: String = ""
    var surname: String = ""
    var second_name: String = ""

    var x: Double = 0.0
    var y: Double = 0.0

    var age: Int = -1
    var speed: Double = -1.0

    constructor(_name: String, _surname: String, _second: String, _age: Int, _speed: Double) {
        name = _name
        surname = _surname
        second_name = _second
        age = _age
        speed = _speed
        println("Мы создали человека с именем: $name")
    }

    fun position()
    {
        println("$name $surname на позиции (${"%.2f".format(x)}; ${"%.2f".format(y)})")
    }

    fun moveTo(_toX: Double, _toY: Double)
    {
        x = _toX
        y = _toY
        println("$name пошел на позицию: (${"%.2f".format(x)}; ${"%.2f".format(y)})")
    }
    open fun move(_sec: Int) {
        for (n in 1 .. _sec) {
            val curX = x
            val curY = y

            val movedX = Random.nextDouble(speed)
            val movedY = sqrt(speed.pow(2) - movedX.pow(2))
            if (Random.nextBoolean() == true) {
                x += movedX
            } else {
                x -= movedX
            }
            if (Random.nextBoolean() == true) {
                y += movedY
            } else {
                y -= movedY
            }
        }
    }
}
class Driver(name: String, surname: String, second_name: String, age: Int, speed: Double, val car_model: String) :
    Human(name, surname,  second_name, age, speed) {
    override fun move(_sec: Int) {
        Thread {

            if (Random.nextBoolean() == true) {
                if (Random.nextBoolean() == true) {
                    x += speed
                } else {
                    x -= speed
                }
            } else {
                if (Random.nextBoolean() == true) {
                    y += speed
                } else {
                    y -= speed
                }
            }
        }
    }
}

fun main() {
    val humans = arrayOf(
        Human("Иван", "Иванов", "Иванович", 25, 8.5),
        Human("Петр", "Петров", "Петрович", 30, 7.8),
        Human("Сергей", "Сергеев", "Сергеевич", 35, 9.2),
    )

    val Vasya: Driver = Driver("Василий", "Абрамович", "Абрамович", 28, 23.5, "Alfa Romeo")

    for (human in humans) {
        human.move(45)
    }
    Vasya.move(45)
    Thread.sleep(2000)
}
