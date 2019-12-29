package ocd.concurrent.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * This class provides a mechanism for implementing atomic composite objects.
 * This is used to make sure that consistent versions of multiple data fields are fetched.
 * The payload data is guarded by a version-stamp that is altered every time the data is changed.
 * The general workflow for a reader looks as follows:
 * <ul>
 *     <li>Fetch the version-stamp via {@link #getVersion()}</li>
 *     <li>Fetch the payload data</li>
 *     <li>Fetch the version-stamp again and compare it with the initial stamp using {@link VersionStamp#isConsistent()}</li>
 *     <li>If they match (and they indicate that no modification is in progress), we have successfully fetched a consistent version of the payload data</li>
 *     <li>Otherwise, try again or use some other fallback method like immutable data containers</li>
 * </ul>
 *
 * The payload data must be accessed through a single VersionGuard.
 * Read and write operations to the payload data as a whole have memory consistency guarantees as defined by {@link ocd.concurrent.MemoryOrder#ACQ_REL}.
 * However, the single operations comprising the compound operations might not have any consistency guarantees.
 *
 *
 * There are other approaches to achieve this, like
 * <ul>
 *     <li>ReadWriteLocks, that make sure that the data is not changed while it is being read</li>
 *     <li>using immutable data containers, so that we can replace a single reference to the container atomically</li>
 * </ul>
 *
 * While this approach uses more memory accesses, CPU instructions and an additional branch compared to the immutable data container approach, it saves one level of indirection.
 * That is because the data can be accessed directly without the indirection over the data container.
 * In many cases, memory latency is the primary bottleneck, so that the reduced indirection often outweighs the additional operations.
 * The additional memory accesses can usually be parallelized and the branch can be well predicted by the CPU.
 * Furthermore, this approach achieves better cache locality because the payload data resides directly in the owner object rather than in a separate data container.
 *
 * Although the access to {@link #version this.version} requires a few levels of indirection, those accesses don't count towards the dependency chain of the payload data, as they only carry a dependency into the branch, which can be well predicted and hence optimized away.
 * Hence it can still be more efficient than an immutable data container, which carries a true dependency into the payload data.
 *
 * However, the additional cost of an encapsulated VersionGuard object rather than a collection of helper methods could still be measurable.
 * But that is a general weakness of java and could be solved in general by manually inlining all fields.
 * In contrast, the indirection caused by an immutable data container approach is fundamental and language independent.
 *
 *
 * If the data can be prefetched by the CPU, the cost of the indirection of an immutable data container is hidden, so the approach given here will be less relevant in those cases.
 * As a rule of thumb, if the access of the composite data is one of the first operations on an object, this approach should be faster than an immutable data container.
 *
 * Also, the given approach should be mainly used when modifications to the payload data are rare, as otherwise concurrent modifications might neglect the performance gain.
 *
 *
 * For large amounts of payload data, the data should be processed on the fly, rather than first fetching all data at once and processing it afterwards. This can save some unnecessary copy instructions.
 * However, on-the-fly processing might increase the risk of encountering a concurrent modification which forces to fetch the data again.
 * For large amounts of data, other approaches like immutable data containers might be more suitable, as the additional overhead becomes small compared to the large amount of data, but fetching an immutable container always succeeds and is not susceptible to concurrent modifications.
 *
 * For small amounts of payload data, on the other hand, it is recommended to first fetch all the data using the above version-stamp approach and only process it afterwards.
 * This minimizes the chances of encountering concurrent modifications and hence the number of retries needed.
 * Out-of-order execution will then start processing the data on the fly on CPU level.
 *
 * Furthermore, on-the-fly processing is susceptible to the problem that data encountered during processing might be inconsistent.
 * Consistency is only known after the call to {@link VersionStamp#isConsistent()}.
 * Processing the data only after the consistency check avoids this issue.
 *
 *
 * Writers must use {@link #startModification()} (or {@link #startModificationExclusive()}) to adapt the version number before modifying the payload data and {@link #endModification()} after the modifications.
 *
 * Readers must make sure that the access to the payload data is visible before the call to {@link VersionStamp#isConsistent()}.
 * Writers must make sure that the call to {@link #startModification()} (respectively {@link #startModificationExclusive()}) is visible before the access to the payload data.
 * Both can be achieved for example by making all the payload data <code>volatile</code>.
 *
 * <b>Note:</b> In Java 9+ this requirement can be eliminated by using memory fences provided by {@link java.lang.invoke.VarHandle}.
 *
 *
 * Using longs for the version-stamps can cause aliasing problems, meaning that two versions-stamps are considered {@link VersionStamp#isConsistent() consistent} although they are not.
 * However, that requires exactly a multiple of 2^63 modifications while the payload data is being read.
 * It would take several 100 years to increase the version-stamp that often, even when updating it every CPU cycle. Hence this should not be a problem in practice.
 *
 *
 * This class is mainly intended for documenting the approach. The implementation is actually quite simple.
 *
 * <b>Note:</b> The functionality of this class is already provided by {@link java.util.concurrent.locks.StampedLock} (except for a slightly optimized {@link #startModificationExclusive()}).
 * The given implementation is just slightly more lightweight and optimized and contains additional documentation.
 */
public class VersionGuard
{
    /**
     * The least significant bit of the version number indicates if a modification is currently in progress.
     * Just comparing the version number before and after reading the payload data for equality is not sufficient.
     * We also need to make sure that the version indicates that no modification is in progress.
     * Otherwise we won't know if we fetched the new or old version of each payload data field when a modification is in progress.
     */
    private final AtomicLong version = new AtomicLong(0);

    /**
     * Returns whether the specified version number indicates that a modification is in progress.
     */
    protected static boolean isModificationInProgress(final long version)
    {
        return (version & 1) != 0;
    }

    /**
     * Returns whether a modification is currently in progress.
     */
    public boolean isModificationInProgress()
    {
        return isModificationInProgress(this.version.get());
    }

    /**
     * Updates the version number to indicate that {@link #isModificationInProgress() a modification is in progress} and obtains exclusive write access to the payload data.
     * Writers must use this to modify the version number before they start modifying the payload data.
     *
     * This has the memory consistency guarantees as defined by {@link ocd.concurrent.AccessMode.Read#ACQUIRE}, ie. it synchronizes with the corresponding {@link #endModification()}.
     *
     * Writers need to make sure that the update to the version number is visible before modifications to the payload data, eg. by making the payload data <code>volatile</code>.
     * <b>Note:</b> In Java 9+ this requirement can be eliminated by using a memory fence provided by {@link java.lang.invoke.VarHandle}.
     *
     * This implementation simply retries to acquire the lock until success.
     * Since the version-stamp approach should only be used when modifications are rare, more sophisticated methods should not be needed, as concurrent writes should be sufficiently rare in practice.
     */
    public void startModification()
    {
        while (true)
        {
            final long version = this.version.get();

            if (!isModificationInProgress(version) && this.version.compareAndSet(version, version + 1))
            {
                // java.lang.invoke.VarHandle.storeStoreFence();
                return;
            }
        }
    }

    /**
     * Updates the version number to indicate that {@link #isModificationInProgress() a modification is in progress}.
     * Writers must use this to modify the version number before they start modifying the payload data.
     *
     * This has the memory consistency guarantees as defined by {@link ocd.concurrent.AccessMode.Read#PLAIN}, ie. it effectively synchronizes with the corresponding {@link #endModification()}.
     *
     * Writers must have exclusive access to the payload data and this VersionGuard, with the requirements specified by {@link ocd.concurrent.ShareMode#EXCLUSIVE_WRITE}
     * There must be no modification currently {@link #isModificationInProgress() in progress}.
     *
     * Writers need to make sure that the update to the version number is visible before modifications to the payload data, eg. by making the payload data <code>volatile</code>.
     * <b>Note:</b> In Java 9+ this requirement can be eliminated by using a memory fence provided by {@link java.lang.invoke.VarHandle}.
     */
    public void startModificationExclusive()
    {
        this.version.lazySet(this.version.get() + 1);
        // java.lang.invoke.VarHandle.storeStoreFence();
    }

    /**
     * Updates the version number to indicate that {@link #isModificationInProgress() no modification is in progress} such that it is {@link VersionStamp#isConsistent() inconsistent} with previous version numbers.
     * Writers must use this to modify the version number after modifying the payload data.
     *
     * This has the memory consistency guarantees defined by {@link ocd.concurrent.AccessMode.Write#RELEASE} with respect to {@link #startModification()} and {@link #startModificationExclusive()}.
     */
    public void endModification()
    {
        this.version.lazySet(this.version.get() + 1);
    }

    /**
     * Retrieves a {@link VersionStamp} corresponding to the current version number that can later be used to check if the version number is still {@link VersionStamp#isConsistent() consistent} with the current one.
     * Readers must use this before reading the payload data.
     *
     * This has the memory consistency guarantees as defined by {@link ocd.concurrent.AccessMode.Read#ACQUIRE}, ie. it synchronizes with the {@link #endModification()} corresponding to the current version number, IF the VersionStamp is later SUCCESSFULLY {@link VersionStamp#isConsistent() checked for consistency}.
     */
    public VersionStamp getVersion()
    {
        return new VersionStamp();
    }

    /**
     * Captures the state of the associated {@link VersionGuard} at the point of creation.
     * Later on, {@link #isConsistent()} can be used to check whether any modification via the associated VersionGuard occurred in the meantime.
     *
     * Escape analysis can usually eliminate these short-lived objects.
     */
    public class VersionStamp
    {
        private final long version;

        VersionStamp()
        {
            this.version = VersionGuard.this.version.get();
        }

        /**
         * Checks if a consistent version of the payload data was fetched, ie. the version number has not changed since the creation of this VersionStamp and {@link #isModificationInProgress() no modification is in progress}.
         * Readers must call this after fetching the payload data.
         *
         * Readers need to make sure that the access to the payload data is visible before the call to this method, eg. by making the payload data <code>volatile</code>..
         * <b>Note:</b> In Java 9+ this requirement can be eliminated by using a memory fence provided by {@link java.lang.invoke.VarHandle}.
         */
        public boolean isConsistent()
        {
            // java.lang.invoke.VarHandle.loadLoadFence();

            // !isModificationInProgress(versionStart) && versionStart == versionEnd
            return this.version == (VersionGuard.this.version.get() & -2);
        }
    }
}
