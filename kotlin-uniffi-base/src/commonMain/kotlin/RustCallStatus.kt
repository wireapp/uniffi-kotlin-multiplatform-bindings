// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect class RustCallStatus

// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect class RustCallStatusByValue

fun RustCallStatus.isSuccess(): Boolean = statusCode == 0.toByte()

fun RustCallStatus.isError(): Boolean = statusCode == 1.toByte()

fun RustCallStatus.isPanic(): Boolean = statusCode == 2.toByte()

expect val RustCallStatus.statusCode: Byte

expect val RustCallStatus.errorBuffer: RustBuffer

expect fun <T> withRustCallStatus(block: (RustCallStatus) -> T): T
