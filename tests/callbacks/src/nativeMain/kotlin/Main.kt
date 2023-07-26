import callbacks.meowAsync
import kotlinx.coroutines.runBlocking

// A toy executable that can be debugged externally after building it
fun main() {
    val answer = runBlocking { meowAsync(42u) }
    val secondAnswer = runBlocking { meowAsync(42u) }
}
