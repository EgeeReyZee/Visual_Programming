import kotlin.random.Random

class Driver(name: String, surname: String, second_name: String, age: Int, speed: Double, val car_model: String) :
    Human(name, surname,  second_name, age, speed) {
    override fun move() {
        Thread {

            if (Random.Default.nextBoolean() == true) {
                if (Random.Default.nextBoolean() == true) {
                    x += speed
                } else {
                    x -= speed
                }
            } else {
                if (Random.Default.nextBoolean() == true) {
                    y += speed
                } else {
                    y -= speed
                }
            }
        }.start()
    }
}