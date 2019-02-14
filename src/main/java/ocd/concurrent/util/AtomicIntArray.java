package ocd.concurrent.util;

import ocd.concurrent.AccessMode;
import ocd.concurrent.MemoryOrder;

/**
 * Defines access methods for thread-safe array-like data structures using int values.
 * The provided access modes correspond to the modes defined in {@link AccessMode}.
 * "location" in the definition of access modes and memory orderings correspond to values at a fixed index.
 * This defines direct convenience overloads, as well as generic versions, accepting variable access modes and memory orderings.
 */
public interface AtomicIntArray
{
    /**
     * Alias for {@link #getOpaque(int)}.
     */
    default int get(int index)
    {
        return this.getOpaque(index);
    }

    /**
     * Atomically gets the value at the specified index.
     * This has the memory consistency guarantees defined by {@link AccessMode.Read#VOLATILE}.
     * This does not require any exclusivity guarantees from the caller.
     */
    int getVolatile(int index);

    /**
     * Atomically gets the value at the specified index.
     * This has the memory consistency guarantees defined by {@link AccessMode.Read#ACQUIRE}.
     * This does not require any exclusivity guarantees from the caller.
     */
    default int getAcquire(int index)
    {
        return this.getVolatile(index);
    }

    /**
     * Atomically gets the value at the specified index.
     * This has the memory consistency guarantees defined by {@link AccessMode.Read#OPAQUE}.
     * This does not require any exclusivity guarantees from the caller.
     */
    default int getOpaque(int index)
    {
        return this.getAcquire(index);
    }

    /**
     * Atomically gets the value at the specified index.
     * This has the memory consistency guarantees and exclusivity requirements as defined by {@link AccessMode.Read#PLAIN}.
     */
    default int getPlain(int index)
    {
        return this.getOpaque(index);
    }

    /**
     * Alias for {@link #setOpaque(int, int)}.
     */
    default void set(int index, int value)
    {
        this.setOpaque(index, value);
    }

    /**
     * Atomically sets the given value at the specified index.
     * This has the memory consistency guarantees defined by {@link AccessMode.Write#VOLATILE}.
     * This does not require any exclusivity guarantees from the caller.
     */
    void setVolatile(int index, int value);

    /**
     * Atomically sets the given value at the specified index.
     * This has the memory consistency guarantees defined by {@link AccessMode.Write#RELEASE}.
     * This does not require any exclusivity guarantees from the caller.
     */
    void setRelease(int index, int value);

    /**
     * Atomically sets the given value at the specified index.
     * This has the memory consistency guarantees defined by {@link AccessMode.Write#OPAQUE}.
     * This does not require any exclusivity guarantees from the caller.
     */
    default void setOpaque(int index, int value)
    {
        this.setRelease(index, value);
    }

    /**
     * Alias for {@link #setOpaqueExclusive(int, int)}.
     */
    default void setExclusive(int index, int value)
    {
        this.setOpaqueExclusive(index, value);
    }

    /**
     * Atomically sets the given value at the specified index.
     * This has the memory consistency guarantees and exclusivity requirements as defined by {@link AccessMode.Write#VOLATILE_EXCLUSIVE}.
     */
    void setVolatileExclusive(int index, int value);

    /**
     * Atomically sets the given value at the specified index.
     * This has the memory consistency guarantees and exclusivity requirements as defined by {@link AccessMode.Write#RELEASE_EXCLUSIVE}.
     */
    void setReleaseExclusive(int index, int value);

    /**
     * Atomically sets the given value at the specified index.
     * This has the memory consistency guarantees and exclusivity requirements as defined by {@link AccessMode.Write#OPAQUE_EXCLUSIVE}.
     */
    default void setOpaqueExclusive(int index, int value)
    {
        this.setReleaseExclusive(index, value);
    }

    /**
     * Atomically sets the given value at the specified index.
     * This has the memory consistency guarantees and exclusivity requirements as defined by {@link AccessMode.Write#PLAIN}.
     */
    void setPlain(int index, int value);

    /**
     * Atomically gets the value at the specified index.
     * This has the memory consistency guarantees and exclusivity requirements as defined by the specified <code>accessMode</code>.
     */
    default int get(int index, int accessMode)
    {
        switch (accessMode)
        {
        case AccessMode.Read.PLAIN:
            return this.getPlain(index);
        case AccessMode.Read.OPAQUE:
            return this.getOpaque(index);
        case AccessMode.Read.ACQUIRE:
            return this.getAcquire(index);
        case AccessMode.Read.VOLATILE:
        default:
            return this.getVolatile(index);
        }
    }

    /**
     * Atomically gets the value at the specified index.
     * This has the memory consistency guarantees defined by the specified <code>memoryOrder</code>.
     * This does not require any exclusivity guarantees from the caller.
     */
    default int getShared(int index, int memoryOrder)
    {
        switch (memoryOrder)
        {
        case MemoryOrder.PLAIN:
        case MemoryOrder.OPAQUE:
            return this.getOpaque(index);
        case MemoryOrder.ACQ_REL:
            return this.getAcquire(index);
        case MemoryOrder.VOLATILE:
        default:
            return this.getVolatile(index);
        }
    }

    /**
     * Atomically sets the given value at the specified index.
     * This has the memory consistency guarantees and exclusivity requirements as defined by the specified <code>accessMode</code>.
     */
    default void set(int index, int value, int accessMode)
    {
        switch (accessMode)
        {
        case AccessMode.Write.PLAIN:
            this.setPlain(index, value);
            return;
        case AccessMode.Write.OPAQUE_EXCLUSIVE:
            this.setOpaqueExclusive(index, value);
            return;
        case AccessMode.Write.RELEASE_EXCLUSIVE:
            this.setReleaseExclusive(index, value);
            return;
        case AccessMode.Write.VOLATILE_EXCLUSIVE:
            this.setVolatileExclusive(index, value);
            return;
        case AccessMode.Write.OPAQUE:
            this.setOpaque(index, value);
            return;
        case AccessMode.Write.RELEASE:
            this.setRelease(index, value);
            return;
        case AccessMode.Write.VOLATILE:
        default:
            this.setVolatile(index, value);
        }
    }

    /**
     * Atomically sets the given value at the specified index.
     * This has the memory consistency guarantees defined by the specified <code>memoryOrder</code>.
     * This does not require any exclusivity guarantees from the caller.
     */
    default void setShared(int index, int value, int memoryOrder)
    {
        switch (memoryOrder)
        {
        case MemoryOrder.PLAIN:
        case MemoryOrder.OPAQUE:
            this.setOpaque(index, value);
            return;
        case MemoryOrder.ACQ_REL:
            this.setRelease(index, value);
            return;
        case MemoryOrder.VOLATILE:
        default:
            this.setVolatile(index, value);
        }
    }

    /**
     * Atomically sets the given value at the specified index.
     * This has the memory consistency guarantees defined by the specified <code>memoryOrder</code>.
     * This requires exclusivity guarantees as defined by {@link ocd.concurrent.ShareMode#EXCLUSIVE_WRITE} from the caller.
     */
    default void setExclusive(int index, int value, int memoryOrder)
    {
        switch (memoryOrder)
        {
        case MemoryOrder.PLAIN:
        case MemoryOrder.OPAQUE:
            this.setOpaqueExclusive(index, value);
            return;
        case MemoryOrder.ACQ_REL:
            this.setReleaseExclusive(index, value);
            return;
        case MemoryOrder.VOLATILE:
        default:
            this.setVolatileExclusive(index, value);
        }
    }

    interface Getter
    {
        int get(AtomicIntArray map, int index);
    }

    interface Setter
    {
        void set(AtomicIntArray map, int index, int value);
    }
}
