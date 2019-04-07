package ocd.concurrent;

/**
 * Defines a set of access modes used for general atomic operations.
 * These are pairs of a {@link MemoryOrder memory order} guaranteed by the atomic operation and a {@link ShareMode share mode} that is guaranteed by the caller.
 * Callers should aim for the weakest memory order possible and for the strongest share mode that they can guarantee.
 * This can allow for more efficient implementations.
 * Access modes used for read operations are listed in {@link Read} and access modes used for write operations are listed in {@link Write}.
 * The memory orders correspond to the modes provided with VarHandle, introduced in Java 9.
 *
 * For performance reasons, this uses static ints instead of enums.
 */
public class AccessMode
{
    /**
     * Returns the {@link MemoryOrder memory order} corresponding to the specified access mode.
     */
    public static int getMemoryOrder(final int accessMode)
    {
        return accessMode & 3;
    }

    /**
     * Returns the {@link ShareMode share mode} corresponding to the specified access mode.
     */
    public static int getShareMode(final int accessMode)
    {
        return accessMode >>> 2;
    }

    /**
     * Encodes the access mode consisting of the specified memory order and share mode.
     */
    private static int getAccessMode(final int memoryOrder, final int shareMode)
    {
        return (shareMode << 2) + memoryOrder;
    }

    /**
     * Defines the access modes used for read operations.
     */
    public static class Read
    {
        /**
         * This mode has the memory consistency guarantees specified by {@link MemoryOrder#VOLATILE}.
         * This does not require any exclusivity guarantees from the caller.
         */
        public static final int VOLATILE = 3; // getAccessMode(MemoryOrder.VOLATILE, ShareMode.SHARED)
        /**
         * This mode has the memory consistency guarantees specified by {@link MemoryOrder#ACQ_REL}.
         * This does not require any exclusivity guarantees from the caller.
         */
        public static final int ACQUIRE = 2; // getAccessMode(MemoryOrder.ACQ_REL, ShareMode.SHARED)
        /**
         * This mode has the memory consistency guarantees specified by {@link MemoryOrder#OPAQUE}.
         * This does not require any exclusivity guarantees from the caller.
         */
        public static final int OPAQUE = 1; // getAccessMode(MemoryOrder.OPAQUE, ShareMode.SHARED)
        /**
         * This mode has the memory consistency guarantees specified by {@link MemoryOrder#PLAIN} (effectively {@link MemoryOrder#ACQ_REL}).
         * This requires exclusivity guarantees as specified by {@link ShareMode#EXCLUSIVE_WRITE}.
         * Operations with this access mode behave like ordinary non-atomic operations.
         *
         * While {@link MemoryOrder#PLAIN} does not give any consistency guarantees by itself, the external synchronization requirements imposed by {@link ShareMode#EXCLUSIVE_WRITE} do give effectively {@link MemoryOrder#ACQ_REL} semantics for these operations.
         */
        public static final int PLAIN = 4; // getAccessMode(MemoryOrder.PLAIN, ShareMode.EXCLUSIVE_WRITE)

        /**
         * Returns a read access mode that {@link MemoryOrder#implies(int, int) implies} both the provided memory order and the memory order guaranteed by the specified access mode
         * and that is {@link ShareMode#allows(int, int) allowed} by the share mode of the specified access mode.
         */
        public static int enfore(final int accessMode, final int memoryOrder)
        {
            return getAccessMode(MemoryOrder.enforce(getMemoryOrder(accessMode), memoryOrder), getShareMode(accessMode));
        }

        /**
         * Returns a read access mode that {@link MemoryOrder#implies(int, int) implies} the memory consistency guarantees of the provided access mode
         * and that is {@link ShareMode#allows(int, int) allowed} by both the specified share mode and the share mode of the specified access mode.
         */
        public static int restrict(final int accessMode, final int shareMode)
        {
            return getAccessMode(getMemoryOrder(accessMode), ShareMode.restrict(getShareMode(accessMode), shareMode));
        }

        /**
         * Returns a read access mode that {@link MemoryOrder#implies(int, int) implies} the specified memory order and that is {@link ShareMode#allows(int, int) allowed} by the specified share mode.
         */
        public static int getAccessMode(final int memoryOrder, final int shareMode)
        {
            final int memoryOrder_ = MemoryOrder.enforce(memoryOrder, shareMode == ShareMode.SHARED ? MemoryOrder.OPAQUE : MemoryOrder.PLAIN);
            final int shareMode_ = ShareMode.restrict(shareMode, memoryOrder == MemoryOrder.PLAIN ? ShareMode.EXCLUSIVE_WRITE : ShareMode.SHARED);

            return AccessMode.getAccessMode(memoryOrder_, shareMode_);
        }
    }

    /**
     * Defines the access modes used for write operations.
     */
    public static class Write
    {
        /**
         * This mode has the memory consistency guarantees specified by {@link MemoryOrder#VOLATILE}.
         * This does not require any exclusivity guarantees from the caller.
         */
        public static final int VOLATILE = 3; // getAccessMode(MemoryOrder.VOLATILE, ShareMode.SHARED)
        /**
         * This mode has the memory consistency guarantees specified by {@link MemoryOrder#ACQ_REL}.
         * This does not require any exclusivity guarantees from the caller.
         */
        public static final int RELEASE = 2; // getAccessMode(MemoryOrder.RELEASE, ShareMode.SHARED)
        /**
         * This mode has the memory consistency guarantees specified by {@link MemoryOrder#OPAQUE}.
         * This does not require any exclusivity guarantees from the caller.
         */
        public static final int OPAQUE = 1; // getAccessMode(MemoryOrder.OPAQUE, ShareMode.SHARED)
        /**
         * This mode has the memory consistency guarantees specified by {@link MemoryOrder#VOLATILE}.
         * This requires exclusivity guarantees as specified by {@link ShareMode#EXCLUSIVE_WRITE}.
         */
        public static final int VOLATILE_EXCLUSIVE = 7; // getAccessMode(MemoryOrder.VOLATILE, ShareMode.EXCLUSIVE_WRITE)
        /**
         * This mode has the memory consistency guarantees specified by {@link MemoryOrder#ACQ_REL}.
         * This requires exclusivity guarantees as specified by {@link ShareMode#EXCLUSIVE_WRITE}.
         */
        public static final int RELEASE_EXCLUSIVE = 6; // getAccessMode(MemoryOrder.RELEASE, ShareMode.EXCLUSIVE_WRITE)
        /**
         * This mode has the memory consistency guarantees specified by {@link MemoryOrder#OPAQUE}.
         * This requires exclusivity guarantees as specified by {@link ShareMode#EXCLUSIVE_WRITE}.
         */
        public static final int OPAQUE_EXCLUSIVE = 5; // getAccessMode(MemoryOrder.OPAQUE, ShareMode.EXCLUSIVE_WRITE)
        /**
         * This mode has the memory consistency guarantees specified by {@link MemoryOrder#PLAIN} (effectively {@link MemoryOrder#ACQ_REL}).
         * This requires exclusivity guarantees as specified by {@link ShareMode#EXCLUSIVE_READ_WRITE}.
         * Operations with this access mode behave like ordinary non-atomic operations.
         *
         * While {@link MemoryOrder#PLAIN} does not give any consistency guarantees by itself, the external synchronization requirements imposed by {@link ShareMode#EXCLUSIVE_READ_WRITE} do give effectively {@link MemoryOrder#ACQ_REL} semantics for these operations.
         */
        public static final int PLAIN = 8; // getAccessMode(MemoryOrder.PLAIN, ShareMode.EXCLUSIVE_READ_WRITE)

        /**
         * Returns a write access mode that {@link MemoryOrder#implies(int, int) implies} both the provided memory order and the memory order guaranteed by the specified access mode
         * and that is {@link ShareMode#allows(int, int) allowed} by the share mode of the specified access mode.
         */
        public static int enfore(final int accessMode, final int memoryOrder)
        {
            return getAccessMode(MemoryOrder.enforce(getMemoryOrder(accessMode), memoryOrder), getShareMode(accessMode));
        }

        /**
         * Returns a write access mode that {@link MemoryOrder#implies(int, int) implies} the memory consistency guarantees of the provided access mode
         * and that is {@link ShareMode#allows(int, int) allowed} by both the specified share mode and the share mode of the specified access mode.
         */
        public static int restrict(final int accessMode, final int shareMode)
        {
            return getAccessMode(getMemoryOrder(accessMode), ShareMode.restrict(getShareMode(accessMode), shareMode));
        }

        /**
         * Returns a write access mode that {@link MemoryOrder#implies(int, int) implies} the specified memory order and that is {@link ShareMode#allows(int, int) allowed} by the specified share mode.
         */
        public static int getAccessMode(final int memoryOrder, final int shareMode)
        {
            final int memoryOrder_ = MemoryOrder.enforce(memoryOrder, shareMode == ShareMode.EXCLUSIVE_READ_WRITE ? MemoryOrder.PLAIN : MemoryOrder.OPAQUE);
            final int shareMode_ = ShareMode.restrict(shareMode, memoryOrder == MemoryOrder.PLAIN ? ShareMode.EXCLUSIVE_READ_WRITE : ShareMode.EXCLUSIVE_WRITE);

            return AccessMode.getAccessMode(memoryOrder_, shareMode_);
        }
    }

    /**
     * Defines the access modes used for read-modify-write operations.
     * This is a class of operations that atomically read a value and update it based on the obtained value.
     * This means that the new value will directly follow the obtained value in the coherent modification order for the respective location, as in the definition of {@link MemoryOrder#OPAQUE}.
     * Examples of such operations are <code>compareAndSet</code> or <code>getAndSet</code>.
     *
     * The {@link MemoryOrder memory ordering} {@link #getMemoryOrder(int)} corresponding to} a read-modify-write operation holds for both the read and the write sub-operation.
     * In the case of exclusive access this is only true effectively, ie. implementations can use read operations that do not directly give the appropriate memory ordering. {@link MemoryOrder#ACQ_REL} memory ordering is then provided through the synchronization requirements on the caller for exclusive access modes.
     *
     * While these operations are mainly intended to be used in {@link ShareMode#SHARED} access mode as means of creating consensus, it can be useful to provide {@link ShareMode#EXCLUSIVE_WRITE} and {@link ShareMode#EXCLUSIVE_READ_WRITE} versions.
     * In exclusive mode, these operations can still avoid duplicate lookups.
     */
    public static class ReadModifyWrite
    {
        /**
         * With this mode, both the read and write operation have the memory consistency guarantees specified by {@link MemoryOrder#VOLATILE}.
         * This does not require any exclusivity guarantees from the caller.
         */
        public static final int VOLATILE = 3; // getAccessMode(MemoryOrder.VOLATILE, ShareMode.SHARED)
        /**
         * With this mode, both the read and write operation have the memory consistency guarantees specified by {@link MemoryOrder#ACQ_REL}.
         * This does not require any exclusivity guarantees from the caller.
         */
        public static final int ACQ_REL = 2; // getAccessMode(MemoryOrder.RELEASE, ShareMode.SHARED)
        /**
         * With this mode, both the read and write operation have the memory consistency guarantees specified by {@link MemoryOrder#OPAQUE}.
         * This does not require any exclusivity guarantees from the caller.
         */
        public static final int OPAQUE = 1; // getAccessMode(MemoryOrder.OPAQUE, ShareMode.SHARED)
        /**
         * With this mode, both the read and write operation have the memory consistency guarantees specified by {@link MemoryOrder#VOLATILE}.
         * This requires exclusivity guarantees as specified by {@link ShareMode#EXCLUSIVE_WRITE}.
         */
        public static final int VOLATILE_EXCLUSIVE = 7; // getAccessMode(MemoryOrder.VOLATILE, ShareMode.EXCLUSIVE_WRITE)
        /**
         * With this mode, the read operation has the memory consistency guarantees specified by {@link MemoryOrder#PLAIN} (effectively {@link MemoryOrder#ACQ_REL}).
         * The write operation has the memory consistency guarantees specified by {@link MemoryOrder#ACQ_REL}.
         * This requires exclusivity guarantees as specified by {@link ShareMode#EXCLUSIVE_WRITE}.
         *
         * While the {@link MemoryOrder#PLAIN} memory ordering for the read operation does not give any consistency guarantees by itself, the external synchronization requirements imposed by {@link ShareMode#EXCLUSIVE_WRITE} do give effectively {@link MemoryOrder#ACQ_REL} semantics for the read operation.
         */
        public static final int ACQ_REL_EXCLUSIVE = 6; // getAccessMode(MemoryOrder.RELEASE, ShareMode.EXCLUSIVE_WRITE)
        /**
         * With this mode, the read operation has the memory consistency guarantees specified by {@link MemoryOrder#PLAIN} (effectively {@link MemoryOrder#OPAQUE}).
         * The write operation has the memory consistency guarantees specified by {@link MemoryOrder#OPAQUE}.
         * This requires exclusivity guarantees as specified by {@link ShareMode#EXCLUSIVE_WRITE}.
         *
         * While the {@link MemoryOrder#PLAIN} memory ordering for the read operation does not give any consistency guarantees by itself, the external synchronization requirements imposed by {@link ShareMode#EXCLUSIVE_WRITE} do give effectively {@link MemoryOrder#ACQ_REL} semantics for the read operation.
         */
        public static final int OPAQUE_EXCLUSIVE = 5; // getAccessMode(MemoryOrder.OPAQUE, ShareMode.EXCLUSIVE_WRITE)
        /**
         * With this mode, both the read and write operation have the memory consistency guarantees specified by {@link MemoryOrder#PLAIN} (effectively {@link MemoryOrder#ACQ_REL}).
         * This requires exclusivity guarantees as specified by {@link ShareMode#EXCLUSIVE_READ_WRITE}.
         * Operations with this access mode behave like ordinary non-atomic operations.
         *
         * While {@link MemoryOrder#PLAIN} does not give any consistency guarantees by itself, the external synchronization requirements imposed by {@link ShareMode#EXCLUSIVE_READ_WRITE} do give effectively {@link MemoryOrder#ACQ_REL} semantics for these operations.
         */
        public static final int PLAIN = 8; // getAccessMode(MemoryOrder.PLAIN, ShareMode.EXCLUSIVE_READ_WRITE)

        /**
         * Returns a read-modify-write access mode that {@link MemoryOrder#implies(int, int) implies} both the provided memory order and the memory order guaranteed by the specified access mode
         * and that is {@link ShareMode#allows(int, int) allowed} by the share mode of the specified access mode.
         */
        public static int enfore(final int accessMode, final int memoryOrder)
        {
            return getAccessMode(MemoryOrder.enforce(getMemoryOrder(accessMode), memoryOrder), getShareMode(accessMode));
        }

        /**
         * Returns a read-modify-write access mode that {@link MemoryOrder#implies(int, int) implies} the memory consistency guarantees of the provided access mode
         * and that is {@link ShareMode#allows(int, int) allowed} by both the specified share mode and the share mode of the specified access mode.
         */
        public static int restrict(final int accessMode, final int shareMode)
        {
            return getAccessMode(getMemoryOrder(accessMode), ShareMode.restrict(getShareMode(accessMode), shareMode));
        }

        /**
         * Returns a read-modify-write access mode that {@link MemoryOrder#implies(int, int) implies} the specified memory order and that is {@link ShareMode#allows(int, int) allowed} by the specified share mode.
         */
        public static int getAccessMode(final int memoryOrder, final int shareMode)
        {
            final int memoryOrder_ = MemoryOrder.enforce(memoryOrder, shareMode == ShareMode.EXCLUSIVE_READ_WRITE ? MemoryOrder.PLAIN : MemoryOrder.OPAQUE);
            final int shareMode_ = ShareMode.restrict(shareMode, memoryOrder == MemoryOrder.PLAIN ? ShareMode.EXCLUSIVE_READ_WRITE : ShareMode.EXCLUSIVE_WRITE);

            return AccessMode.getAccessMode(memoryOrder_, shareMode_);
        }
    }
}
