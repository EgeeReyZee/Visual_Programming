fun main() {
    val humans = arrayOf(
        Human("Иван", "Иванов", "Иванович", 25, 8.5),
        Human("Петр", "Петров", "Петрович", 30, 7.8),
        Human("Сергей", "Сергеев", "Сергеевич", 35, 9.2),
    )

    val Vasya: Driver = Driver("Василий", "Абрамович", "Абрамович", 28, 23.5, "Alfa Romeo")

    for (human in humans) {
        human.move(45)
        human.position()
    }
    Vasya.move(45)
    Vasya.position()
    Thread.sleep(1000)
}
