import okio.Buffer
import java.nio.ByteOrder

actual typealias Pointer = com.sun.jna.Pointer

actual typealias UBytePointer = com.sun.jna.Pointer

actual fun Long.toPointer() = com.sun.jna.Pointer(this)

actual fun Pointer.toLong(): Long = com.sun.jna.Pointer.nativeValue(this)

actual fun UBytePointer.copyToBuffer(len: Long): Buffer =
    // FIXME cannot use JVM-only getByteBuffer here
    // FIXME this would mean copying the data...
    getByteBuffer(0, len.toLong()).also {
        it.order(ByteOrder.BIG_ENDIAN)
    }.let { byteBuffer ->
        Buffer().also { it.write(byteBuffer) }
    }
