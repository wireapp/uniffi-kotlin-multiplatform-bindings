import kotlinx.cinterop.*

import okio.Buffer
import okio.Source
import okio.Timeout

// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("ACTUAL_WITHOUT_EXPECT", "ACTUAL_TYPE_ALIAS_WITH_USE_SITE_VARIANCE")
actual typealias Pointer = CPointer<out CPointed>

@Suppress("ACTUAL_WITHOUT_EXPECT", "ACTUAL_TYPE_ALIAS_WITH_USE_SITE_VARIANCE", "ACTUAL_TYPE_ALIAS_WITH_COMPLEX_SUBSTITUTION")
actual typealias UBytePointer = CPointer<UByteVar>

actual fun Long.toPointer(): Pointer = requireNotNull(this.toCPointer())

actual fun Pointer.toLong(): Long = this.rawValue.toLong()

// FIXME
actual fun UBytePointer.copyToBuffer(len: Long): Buffer = Buffer().also { it.write(source(), len) }

fun UBytePointer.source(): Source =
    object : Source {
        var index = 0L

        override fun close() {
            // nothing to do, we do not own the memory
        }

        override fun read(sink: Buffer, byteCount: Long): Long {
            for (index in 0L until byteCount) {
                sink.writeByte(get(this.index++).toInt())
            }
            return byteCount
        }

        override fun timeout(): Timeout = Timeout.NONE

    }
