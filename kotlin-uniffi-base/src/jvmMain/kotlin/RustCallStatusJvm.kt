import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.Structure.ByValue

@Structure.FieldOrder("code", "error_buf")
actual open class RustCallStatus : Structure() {
    @JvmField
    var code: Byte = 0

    @JvmField
    var error_buf: RustBuffer = RustBuffer()
}

@Suppress("NO_ACTUAL_FOR_EXPECT")
actual open class RustCallStatusByValue : RustCallStatus(), ByValue

actual val RustCallStatus.statusCode: Byte
    get() = code
actual val RustCallStatus.errorBuffer: RustBuffer
    get() = error_buf

actual fun <T> withRustCallStatus(block: (RustCallStatus) -> T): T {
    val rustCallStatus = RustCallStatus()
    return block(rustCallStatus)
}
