import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

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
    open fun move() {
        Thread {
            val curX = x
            val curY = y

            val movedX = Random.Default.nextDouble(speed)
            val movedY = sqrt(speed.pow(2) - movedX.pow(2))
            if (Random.Default.nextBoolean() == true) {
                x += movedX
            } else {
                x -= movedX
            }
            if (Random.Default.nextBoolean() == true) {
                y += movedY
            } else {
                y -= movedY
            }
        }.start()
    }
}
