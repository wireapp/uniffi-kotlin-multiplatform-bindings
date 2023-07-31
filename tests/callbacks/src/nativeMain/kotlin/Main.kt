import callbacks.calcAsync
import kotlinx.coroutines.runBlocking

// A toy executable that can be debugged externally after building it
fun main() {
    val answer = runBlocking { calcAsync(42u) }
    val secondAnswer = runBlocking { calcAsync(42u) }
}
