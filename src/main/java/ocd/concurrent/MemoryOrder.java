package ocd.concurrent;

/**
 * Defines a set of memory consistency modes used for general atomic operations.
 * These memory orders are important to make sure that concurrent threads see a coherent history of data.
 * In order to achieve the desired memory consistency guarantee, both writer and reader must use it (or a stronger one).
 * Weaker memory orderings might be more efficient, depending on the CPU and the atomic data structure in question.
 *
 * There are two primary effects that can lead to weaker memory orderings being more efficient:
 * <ul>
 *     <li>
 *         CPUs might implement weaker memory orderings more efficiently. However, many modern processors guarantee volatile reads and release stores for free.
 *         Volatile stores however are usually more expensive.
 *     </li>
 *     <li>
 *         Weaker memory orderings can allow compilers/JIT to reorder memory accesses around the method call.
 *         However, for complex data structures it can be expected that such optimizations won't be employed.
 *         Furthermore, implementations for complex data structures might enforce acquire-release semantics anyway.
 *     </li>
 * </ul>
 *
 * In summary, the following fallbacks can usually be expected to be cheap/free:
 * <ul>
 *     <li>Acquire reads can fallback to volatile reads.</li>
 *     <li>For complex data structures, {@link #OPAQUE} can fallback to {@link #ACQ_REL}.</li>
 * </ul>
 *
 * Callers should aim for the weakest possible memory ordering.
 * These memory orders correspond to the modes provided with VarHandle, introduced in Java 9.
 *
 * For performance reasons, this uses static ints instead of enums.
 */
public class MemoryOrder
{
    /**
     * Does not give any memory consistency guarantee other than atomicity.
     * In particular, this does not guarantee monotonic reads.
     */
    public static final int NONE = 0;
    /**
     * In the following, the meaning of "location" depends on the underlying atomic data structure.
     * Usually, it is clear what "location" means, eg. for an array it describes a value corresponding to a fixed index, for a map it describes a mapping for a fixed key.
     *
     * This mode guarantees that operations on a fixed location have a coherent history among all threads.
     * That means that for a fixed location, all threads will observe the same history of modifications.
     * However, there is no guarantee with respect to other locations.
     */
    public static final int OPAQUE = 1;
    /**
     * This mode gives {@link #OPAQUE} consistency guarantees.
     * Additionally, reading operations synchronize with the corresponding write operation that produced the observed value.
     * That means that any writing operations (irrespective of their memory ordering) prior to this write are visible to all reading operations (irrespective of their memory ordering) after this read.
     */
    public static final int ACQ_REL = 2;
    /**
     * This mode gives {@link #ACQ_REL} consistency guarantees.
     * Additionally, all operations with this memory ordering together with all volatile variable accesses have a coherent history among all threads.
     * This means that all VOLATILE operations will appear in the same order on all threads.
     */
    public static final int VOLATILE = 3;

    /**
     * Returns an {@link AccessMode.Read access mode} used for read operations that guarantees the specified memory order.
     */
    public static int getReadAccessMode(final int memoryOrder)
    {
        return AccessMode.Read.getAccessMode(memoryOrder, ShareMode.SHARED);
    }

    /**
     * Returns an {@link AccessMode.Write access mode} used for write operations that guarantees the specified memory order.
     */
    public static int getWriteAccessMode(final int memoryOrder)
    {
        return AccessMode.Write.getAccessMode(memoryOrder, ShareMode.SHARED);
    }

    /**
     * Returns an {@link AccessMode.Write access mode} used for exclusive write operations that guarantees the specified memory order.
     */
    public static int getExclusiveWriteAccessMode(final int memoryOrder)
    {
        return AccessMode.Write.getAccessMode(memoryOrder, ShareMode.EXCLUSIVE_WRITE);
    }

    /**
     * Returns whether operations performed with <code>memoryOrder1</code> order also give the guarantees of <code>memoryOrder2</code>.
     */
    public static boolean implies(final int memoryOrder1, final int memoryOrder2)
    {
        return memoryOrder1 >= memoryOrder2;
    }

    /**
     * Returns a memory order that {@link #implies(int, int) implies} both of the given memory orders.
     */
    public static int enforce(final int memoryOrder1, final int memoryOrder2)
    {
        return memoryOrder1 >= memoryOrder2 ? memoryOrder1 : memoryOrder2;
    }
}
