import okio.Buffer

// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect class Pointer

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect class UBytePointer

expect fun Long.toPointer(): Pointer

expect fun Pointer.toLong(): Long

expect fun UBytePointer.copyToBuffer(len: Long): Buffer
