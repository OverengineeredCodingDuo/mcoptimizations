package ocd.concurrent;

/**
 * Defines a set of share modes used for general atomic operations.
 * Those are guarantees regarding exclusive ownership that a caller can pass to atomic operations.
 * Stronger guarantees allow for more efficient implementations of atomic operations, eg. by eliding locks or using normal writes instead of CAS operations.
 * Callers need to make sure that the specified share mode is indeed guaranteed, otherwise the results are undefined.
 * They should aim for the strongest share mode that they can guarantee.
 *
 * For performance reasons, this uses static ints instead of enums.
 */
public class ShareMode
{
    /**
     * This mode does not give any exclusivity guarantees.
     * This means that there might be other concurrent writers, as well as concurrent readers.
     * Callers don't need to assert anything for this mode.
     */
    public static final int SHARED = 0;
    /**
     * This mode guarantees exclusive write access.
     * This means that there are no concurrent writers, but there might be concurrent readers.
     * Callers need to make sure that all writes prior to acquiring exclusive write access are visible and that all write operations prior to releasing exclusive ownership are visible to subsequent writers.
     * These guarantees are usually automatically achieved by external synchronization methods (eg. locks).
     *
     * For example, this mode allows to elide (shared) write locks and replace CAS operations with ordinary writes.
     */
    public static final int EXCLUSIVE_WRITE = 1;
    /**
     * This mode guarantees fully exclusive access.
     * This means there are no other concurrent writers ore readers.
     * Callers need to make sure that all writes prior to acquiring exclusive access are visible and that all write operations prior to releasing exclusive ownership are visible to subsequent writers and readers.
     * These guarantees are usually automatically achieved by external synchronization methods (eg. locks).
     *
     * In addition to the optimizations allowed by {@link #EXCLUSIVE_WRITE}, this also allows to elide ReadWriteLocks, for example.
     */
    public static final int EXCLUSIVE_READ_WRITE = 2;

    /**
     * Returns whether operations can be performed with <code>shareMode2</code> when a caller guarantees <code>shareMode1</code>.
     */
    public static boolean allows(final int shareMode1, final int shareMode2)
    {
        return shareMode1 >= shareMode2;
    }

    /**
     * Returns a share mode that is {@link #allows(int, int) allowed} by both specified share modes.
     */
    public static int restrict(final int shareMode1, final int shareMode2)
    {
        return shareMode1 <= shareMode2 ? shareMode1 : shareMode2;
    }
}
