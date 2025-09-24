import kotlin.random.Random
import kotlin.math.sqrt
import kotlin.math.pow
import java.util.Timer
import java.util.TimerTask

class Human
{
    var name: String = ""
    var surname: String = ""
    var second_name: String = ""

    var x: Double = 0.0
    var y: Double = 0.0

    var age: Int = -1
    var speed: Double = -1.0

    constructor(_name: String, _surname: String, _second: String, _age: Int, _speed: Double){
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
    fun freeWalking(_sec: Int) {
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

fun main() {
    val humans = arrayOf(
        Human("Иван", "Иванов", "Иванович", 25, 8.5),
        Human("Петр", "Петров", "Петрович", 30, 7.8),
        Human("Сергей", "Сидоров", "Сергеевич", 35, 9.2),
        Human("Алексей", "Смирнов", "Алексеевич", 28, 8.1),
        Human("Дмитрий", "Кузнецов", "Дмитриевич", 32, 7.5),
        Human("Андрей", "Попов", "Андреевич", 27, 8.9),
        Human("Максим", "Васильев", "Максимович", 29, 7.2),
        Human("Владимир", "Соколов", "Владимирович", 31, 8.3),
        Human("Артем", "Михайлов", "Артемович", 26, 9.0)
    )

    humans.forEachIndexed { index, human ->
        val row = index / 5
        val col = index % 5
        human.x = col * 30.0
        human.y = row * 25.0
        println("${human.name} ${human.surname} - Стартовая позиция: (${"%.2f".format(human.x)}, ${"%.2f".format(human.y)})")
    }

    val simulationTimeSeconds = 45
    println("\nstarts on  $simulationTimeSeconds seconds")
    println("number of members: ${humans.size}")
    humans.forEachIndexed { index, human ->
        human.freeWalking(45)
        human.position()
    }
}