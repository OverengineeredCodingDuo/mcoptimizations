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
 * When only a single memory ordering is implemented, {@link #ACQ_REL} should be used.
 * <ul>
 *     <li>While {@link #OPAQUE} is sufficient in some cases, there are situations where {@link #ACQ_REL} is needed. Since this fallback is usually quite cheap/free, this should be a fair compromise.</li>
 *     <li>A common example where {@link #OPAQUE} is not sufficient, but {@link #ACQ_REL} is, is given by data containers for non-scalar objects.
 *     There it is desirable that not only the reference to the object is read atomically, but also the data contained in the object is up-to-date, ie. at least as recent as at the time of writing the object to the data container.
 *     This is achieved with {@link #ACQ_REL} memory ordering, but not with {@link #OPAQUE} memory ordering.</li>
 *     <li>{@link #VOLATILE} semantics are not really ever needed in practice for data containers. Since this does usually incur a performance hit, this should not be chosen as the only implemented memory ordering.</li>
 * </ul>
 *
 * A concurrent reader should not assume any memory consistency for unrelated data.
 * A task that requires any sort of global memory consistency should be synchronized by other means. Otherwise chances are high that there are errors even with sequential consistency.
 * The only guarantee that should be used is atomicity, ie. reads always return meaningful values.
 *
 * For performance reasons, this uses static ints instead of enums.
 */
public class MemoryOrder
{
    /**
     * Does not give any memory consistency guarantee other than atomicity.
     * In particular, this does not guarantee monotonic reads.
     *
     * Operations with this memory ordering behave like ordinary non-volatile variable accesses.
     * This means that they are well-behaved when used under exclusive access.
     * Using shared access, one won't get any consistency guarantees.
     */
    public static final int PLAIN = 0;
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
     * Additionally, reading operations synchronize with the corresponding write operation that produced the observed value (but not with write operations that produced earlier values in the coherent modification history).
     * That means that all operations of any kind prior to a release write happen before all operations of any kind subsequent to the corresponding acquire read observing the written value.
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
     * Returns an {@link AccessMode.ReadModifyWrite access mode} used for read-modify-write operations that guarantees the specified memory order.
     */
    public static int getRMWAccessMode(final int memoryOrder)
    {
        return AccessMode.ReadModifyWrite.getAccessMode(memoryOrder, ShareMode.SHARED);
    }

    /**
     * Returns an {@link AccessMode.ReadModifyWrite access mode} used for exclusive read-modify-write operations that guarantees the specified memory order.
     */
    public static int getExclusiveRMWAccessMode(final int memoryOrder)
    {
        return AccessMode.ReadModifyWrite.getAccessMode(memoryOrder, ShareMode.EXCLUSIVE_WRITE);
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
