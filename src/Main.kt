import kotlin.system.exitProcess

fun main() {
    val humans = arrayOf(
        Human("Иван", "Иванов", "Иванович", 25, 8.5),
        Human("Петр", "Петров", "Петрович", 30, 7.8),
        Human("Сергей", "Сергеев", "Сергеевич", 35, 9.2),
    )

    for (human in humans) {
        val row = index / 5
        val col = index % 5
        human.x = col * 30.0
        human.y = row * 25.0
        println("${human.name} ${human.surname} - Стартовая позиция: (${"%.2f".format(human.x)}, ${"%.2f".format(human.y)})")
    }

    val simulationTimeSeconds = 45
    println("\nstarts on  $simulationTimeSeconds seconds")
    println("number of members: ${humans.size}")
    for (human in humans) {
        human.freeWalking(45)
        human.position()
    }
}
