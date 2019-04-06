package ocd.concurrent.util;

/**
 * This class provides helper methods for implementing version-stamp based atomic composite objects.
 * This is used to make sure that consistent versions of multiple data fields are fetched.
 * The payload data is guarded by a version-stamp that is altered every time the data is changed.
 * The general workflow for a reader looks as follows:
 * <ul>
 *     <li>Fetch the version-stamp</li>
 *     <li>Fetch the payload data</li>
 *     <li>Fetch the version-stamp again and compare it with the initial stamp</li>
 *     <li>If they match (and they indicate that no modification is in progress), we have successfully fetched a consistent version of the payload data</li>
 *     <li>Otherwise, try again</li>
 * </ul>
 * There are other approaches to achieve this, like
 * <ul>
 *     <li>ReadWriteLocks, that make sure that the data is not changed while it is being read</li>
 *     <li>using immutable data containers, so that we can replace a single reference to the container atomically</li>
 * </ul>
 *
 *
 * While this approach uses more memory accesses, CPU instructions and an additional branch compared to the immutable data container, it saves one level of indirection.
 * That is because the data can be accessed directly without the indirection over the data container.
 * In many cases, memory latency is the primary bottleneck, so that the reduced indirection often outweighs the additional operations.
 * The additional memory accesses can usually be parallelized and the branch can be well predicted by the CPU.
 * Furthermore, this approach achieves better cache locality because the payload data resides directly in the owner object rather than in a separate data container.
 *
 * If the data can be prefetched by the CPU, the cost of the indirection is hidden, so this approach will be less relevant in those cases.
 * As a rule of thumb, if the access of the composite data is one of the first operations on an object, this approach should be faster than an immutable data container.
 *
 *
 * The payload data should be processed on the fly, rather than first fetching all data at once and process it afterwards.
 * Otherwise, the processing might be delayed too much which nullifies the performance gains from the avoided indirection.
 * Also, because CPU registers are limited, this might cause more data to be moved around, which causes additional overhead.
 *
 * Ideally, the code should be written as if there was no concurrency (eg. like it would be written using immutable data containers).
 * Afterwards the two reads to the version field should be inserted just before the first access of the payload data and just after the last access.
 * The rest of the processing (after fetching the last payload data) can then continue after the {@link #isConsistent(long, long) consistency check}.
 *
 * However, one should keep in mind that the data encountered within on-the-fly processing might be inconsistent.
 * Consistency is only known after the call to {@link #isConsistent(long, long)}.
 * One should be especially cautious to make sure that inconsistencies won't cause problems.
 * That may limit the applicability of this approach.
 *
 *
 * Writers must use {@link #startModification(long)} to adapt the version number before modifying the data and {@link #endModification(long)} after the modifications.
 * They should use CAS operations to update the version number or make sure that they have exclusive write access.
 * Also, they should check if {@link #isModificationInProgress(long) a modification is in progress} by another thread before starting a modification (unless we have exclusive write access).
 *
 * Both readers and writers must make sure that the two accesses to the version number are visible before, respectively after, the accecss to the payload data.
 * For example, this can be achieved by
 * <ul>
 *     <li>placing a loadFence after reading the version number for the first time and one before reading it for the second time. (Respectively storeFences for writers)</li>
 *     <li>or making all the payload data and the version field <code>volatile</code></li>
 * </ul>
 *
 *
 * For performance reasons, this class only provides static helper methods rather than a Version object encapsulating the version-stamp.
 * Although the accesses to the version stamp don't count towards the dependency chain of the payload data, the overhead of such an object compared to a simple long could still be measurable.
 *
 * Using longs for the version-stamps can cause aliasing problems, meaning that two versions-stamps are considered {@link #isConsistent(long, long) consistent} although they are not.
 * However, that requires exactly a multiple of 2^63 modifications while the payload data is read.
 * It would take several 100 years to increase the version-stamp that often, even when updating it every CPU cycle. Hence this should not be a problem in practice.
 *
 *
 * This class is mainly intended for documenting the approach. The implementation of the helper methods is actually quite simple.
 */
public class VersionUtils
{
    /*
     * The least significant bit of the version number indicates if a modification is currently in progress.
     * Just comparing the version number before and after reading the payload data for equality is not sufficient.
     * We also need to make sure that the version indicates that no modification is in progress.
     * Otherwise we won't know if we fetched the new or old version of each payload data field when a modification is in progress.
     */

    /**
     * Initializes a new version number that indicates that {@link #isModificationInProgress(long) no modification is in progress}.
     */
    public static long initVersion()
    {
        return 0;
    }

    /**
     * Returns whether the specified version number indicates that a modification is in progress.
     */
    public static boolean isModificationInProgress(final long version)
    {
        return (version & 1) != 0;
    }

    /**
     * Updates the specified version number to indicate that {@link #isModificationInProgress(long) a modification is in progress}.
     * Writers must use this to modify the version number before they start modifying the payload data.
     * They should use CAS operations to update the version field or make sure that they have exclusive write access.
     * The specified version must indicate that {@link #isModificationInProgress(long) no modification is in progress}.
     *
     * Writers need to make sure that the update to the version number is visible before modifications to the payload data, eg. by making the version field <code>volatile</code>.
     */
    public static long startModification(final long version)
    {
        return version + 1;
    }

    /**
     * Returns an updated version number that is distinguishable from the previous iterations of the specified version number.
     * The returned version indicates that {@link #isModificationInProgress(long) no modification is in progress}.
     *
     * Writers need to make sure that modifications to the payload data are visible before the update to the version number, eg. by making the payload data <code>volatile</code>.
     *
     * @param prevVersion The version number before {@link #startModification(long) starting the modification}
     */
    public static long endModification(final long prevVersion)
    {
        return prevVersion + 2;
    }

    /**
     * Checks if a consistent version of the payload data was fetched.
     * <code>versionStart</code> is fetched before the payload data and <code>versionEnd</code> afterwards.
     * Readers need to make sure that the access to <code>versionStart</code> is visible before the access to the payload data and that this is visible before the access to <code>versionEnd</code>.
     * This can be achieved, for example, by making the payload data and the version field <code>volatile</code>.
     */
    public static boolean isConsistent(final long versionStart, final long versionEnd)
    {
        // !isModificationInProgress(versionStart) && versionStart == versionEnd
        return versionStart == (versionEnd & -2);
    }
}
