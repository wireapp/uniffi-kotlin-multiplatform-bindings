import kotlinx.atomicfu.getAndUpdate

// This is actually common kotlin but inefficient because of the coarse granular locking...
// TODO either create some real implementation or at least measure if protecting the counter
//      with the lock and using a plain Int wouldn't be faster
actual class UniFfiHandleMap<T : Any> {
    private val mapLock = kotlinx.atomicfu.locks.ReentrantLock()
    private val map = HashMap<ULong, T>()

    // Use AtomicInteger for our counter, since we may be on a 32-bit system.  4 billion possible
    // values seems like enough. If somehow we generate 4 billion handles, then this will wrap
    // around back to zero and we can assume the first handle generated will have been dropped by
    // then.
    private val counter = kotlinx.atomicfu.atomic<Int>(0)

    actual val size: Int
        get() = map.size

    actual fun insert(obj: T): ULong {
        val handle = counter.getAndUpdate { it + 1 }.toULong()
        synchronizedMapAccess { map.put(handle, obj) }
        return handle
    }

    actual fun get(handle: ULong): T? {
        return synchronizedMapAccess { map.get(handle) }
    }

    actual fun remove(handle: ULong) {
        synchronizedMapAccess { map.remove(handle) }
    }

    fun <T> synchronizedMapAccess(block: () -> T): T {
        mapLock.lock()
        try {
            return block()
        } finally {
            mapLock.unlock()
        }
    }
}
