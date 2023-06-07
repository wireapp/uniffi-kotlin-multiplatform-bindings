import callbacks.meowAsync
import kotlinx.coroutines.runBlocking

fun main() {
    val answer = runBlocking { meowAsync(42u) }
    val secondAnswer = runBlocking { meowAsync(42u) }
}
