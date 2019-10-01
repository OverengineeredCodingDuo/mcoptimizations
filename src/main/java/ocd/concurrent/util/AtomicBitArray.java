package ocd.concurrent.util;

import java.util.concurrent.atomic.AtomicLongArray;

import org.apache.commons.lang3.Validate;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.BitArray;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import ocd.concurrent.AccessMode;
import ocd.concurrent.ShareMode;

/**
 * Thread-safe version of {@link BitArray}.
 * This uses a different packing format than {@link BitArray} to make sure that each entry is contained inside a single long.
 * This allows accessing the entry atomically.
 *
 * Operations have {@link ocd.concurrent.MemoryOrder#ACQ_REL} semantics, where "location" in the definition of access modes and memory orderings correspond to values at a fixed index.
 * {@link ocd.concurrent.ShareMode#SHARED}, {@link ocd.concurrent.ShareMode#EXCLUSIVE_WRITE} and {@link ocd.concurrent.ShareMode#EXCLUSIVE_READ_WRITE} access modes are provided.
 */
public class AtomicBitArray
{
    private final AtomicLongArray data;
    protected final int bitsPerEntry;
    protected final int entriesPerLong;
    protected final long maxEntryValue;
    private final int arraySize;

    public AtomicBitArray(int bitsPerEntryIn, int arraySizeIn)
    {
        this.bitsPerEntry = bitsPerEntryIn;
        this.entriesPerLong = 64 / bitsPerEntryIn;
        this.arraySize = arraySizeIn;
        this.maxEntryValue = (1L << bitsPerEntryIn) - 1;
        this.data = new AtomicLongArray(MathHelper.roundUp(arraySizeIn, this.entriesPerLong) / this.entriesPerLong);
    }

    /**
     * Returns the index with respect to {@link #data} where the entry at the given <code>index</code> is located.
     * This can be overridden by subclasses to give more efficient implementations.
     */
    protected int getIndex(final int index)
    {
        return getIndex(index, this.entriesPerLong);
    }

    /**
     * Standard implementation for {@link #getIndex(int)}
     */
    protected static int getIndex(final int index, final int entriesPerLong)
    {
        return index / entriesPerLong;
    }

    /**
     * Returns the number of bits which the entry at the given <code>index</code> is shifted with respect to the containing long inside {@link #data}.
     * This can be overridden by subclasses to give more efficient implementations.
     */
    protected int getShift(final int index)
    {
        return getShift(index, this.bitsPerEntry, this.entriesPerLong);
    }

    /**
     * Standard implementation for {@link #getShift(int)}}
     */
    protected static int getShift(final int index, final int bitsPerEntry, final int entriesPerLong)
    {
        return (index % entriesPerLong) * bitsPerEntry;
    }

    /**
     * Atomically sets the given value at the specified index and returns the previous value.
     * This has the memory consistency guarantees defined by {@link AccessMode.ReadModifyWrite#ACQ_REL}.
     * This does not require any exclusivity guarantees from the caller.
     */
    public int getAndSet(int index, int value)
    {
        Validate.inclusiveBetween(0, this.arraySize - 1, index);
        Validate.inclusiveBetween(0, this.maxEntryValue, value);

        final int i = this.getIndex(index);
        final int shift = this.getShift(index);

        // Use CAS to set the entry atomically.
        while (true)
        {
            final long oldVal = this.data.get(i);

            if (this.data.compareAndSet(i, oldVal, (oldVal & ~(this.maxEntryValue << shift)) | (((long) value) << shift)))
                return (int) ((oldVal >>> shift) & this.maxEntryValue);
        }
    }

    /**
     * Atomically sets the given value at the specified index and returns the previous value.
     * This has the memory consistency guarantees and exclusivity requirements as defined by {@link AccessMode.ReadModifyWrite#ACQ_REL_EXCLUSIVE}.
     */
    public int getAndSetExclusive(int index, int value)
    {
        Validate.inclusiveBetween(0, this.arraySize - 1, index);
        Validate.inclusiveBetween(0, this.maxEntryValue, value);

        final int i = this.getIndex(index);
        final int shift = this.getShift(index);

        // We are guaranteed exclusive write access. Hence we can use ordinary writes instead of CAS.
        long oldVal = this.data.get(i);
        this.data.lazySet(i, (oldVal & ~(this.maxEntryValue << shift)) | (((long) value) << shift));

        return (int) ((oldVal >>> shift) & this.maxEntryValue);
    }

    /**
     * Atomically sets the given value at the specified index and returns the previous value.
     * This has the memory consistency guarantees and exclusivity requirements as defined by {@link AccessMode.ReadModifyWrite#PLAIN}.
     */
    public int getAndSetPlain(int index, int value)
    {
        return this.getAndSetExclusive(index, value);
    }

    /**
     * Atomically sets the given value at the specified index and returns the previous value.
     * This has the memory consistency guarantees defined by {@link ocd.concurrent.MemoryOrder#ACQ_REL} and exclusivity requirements as defined by the specified <code>shareMode</code>.
     */
    public int getAndSet(int index, int value, int shareMode)
    {
        return ShareMode.allows(shareMode, ShareMode.EXCLUSIVE_WRITE) ? this.getAndSetExclusive(index, value) : this.getAndSet(index, value);
    }

    /**
     * Atomically sets the given value at the specified index.
     * This has the memory consistency guarantees defined by {@link AccessMode.Write#RELEASE}.
     * This does not require any exclusivity guarantees from the caller.
     */
    public void set(int index, int value)
    {
        this.getAndSet(index, value);
    }

    /**
     * Atomically sets the given value at the specified index.
     * This has the memory consistency guarantees and exclusivity requirements as defined by {@link AccessMode.Write#RELEASE_EXCLUSIVE}.
     */
    public void setExclusive(int index, int value)
    {
        this.getAndSetExclusive(index, value);
    }

    /**
     * Atomically sets the given value at the specified index.
     * This has the memory consistency guarantees and exclusivity requirements as defined by {@link AccessMode.Write#PLAIN}.
     */
    public void setPlain(int index, int value)
    {
        this.setExclusive(index, value);
    }

    /**
     * Atomically sets the given value at the specified index.
     * This has the memory consistency guarantees defined by {@link ocd.concurrent.MemoryOrder#ACQ_REL} and exclusivity requirements as defined by the specified <code>shareMode</code>.
     */
    public void set(int index, int value, int shareMode)
    {
        if (ShareMode.allows(shareMode, ShareMode.EXCLUSIVE_WRITE))
            this.setExclusive(index, value);
        else
            this.set(index, value);
    }

    /**
     * Atomically gets the value at the specified index.
     * This has the memory consistency guarantees defined by {@link AccessMode.Read#ACQUIRE}.
     * This does not require any exclusivity guarantees from the caller.
     */
    public int get(int index)
    {
        Validate.inclusiveBetween(0, this.arraySize - 1, index);
        return (int) ((this.data.get(this.getIndex(index)) >>> this.getShift(index)) & this.maxEntryValue);
    }

    /**
     * Atomically gets the value at the specified index.
     * This has the memory consistency guarantees and exclusivity requirements as defined by {@link AccessMode.Read#PLAIN}.
     */
    public int getPlain(int index)
    {
        return this.get(index);
    }

    /**
     * Atomically gets the value at the specified index.
     * This has the memory consistency guarantees defined by {@link ocd.concurrent.MemoryOrder#ACQ_REL} and exclusivity requirements as defined by the specified <code>shareMode</code>.
     */
    public int get(int index, int shareMode)
    {
        return ShareMode.allows(shareMode, ShareMode.EXCLUSIVE_WRITE) ? this.getPlain(index) : this.get(index);
    }

    /**
     * Get a serialized long array for the data in the format specified by {@link BitArray}.
     * The returned array is not a snapshot of the data, ie. if there are concurrent modifications, it is unspecified which of those are reflected in the returned array.
     */
    public long[] getSerializedLongArray()
    {
        final BitArray bitArray = new BitArray(this.bitsPerEntry, this.arraySize);

        for (int i = 0; i < this.arraySize; ++i)
            bitArray.setAt(i, this.get(i));

        return bitArray.getBackingLongArray();
    }

    /**
     * Get the size of a serialized long array for the data in the format specified by {@link BitArray}.
     */
    public int serializedSize()
    {
        return MathHelper.roundUp(this.arraySize * this.bitsPerEntry, 64) / 64;
    }

    public int size()
    {
        return this.arraySize;
    }

    /**
     * Copies the data from the specified bit array.
     * This method is NOT thread-safe and requires exclusivity guarantees as specified by {@link ShareMode#EXCLUSIVE_READ_WRITE} from the caller.
     */
    public void read(final BitArray bitArray)
    {
        if (this.arraySize != bitArray.size())
            throw new IllegalArgumentException(String.format("Trying to read array of size %s into array of incompatible size %s", bitArray.size(), this.arraySize));

        for (int i = 0; i < this.arraySize; ++i)
            this.setPlain(i, bitArray.getAt(i));
    }

    /**
     * Copies the data in the format specified by {@link BitArray}.
     * This method is NOT thread-safe and requires exclusivity guarantees as specified by {@link ShareMode#EXCLUSIVE_READ_WRITE} from the caller.
     */
    public void read(final long[] data)
    {
        this.read(new BitArray(this.bitsPerEntry, this.arraySize, data));
    }

    /**
     * Copies the data in the format specified by {@link BitArray}.
     * This method is NOT thread-safe and requires exclusivity guarantees as specified by {@link ShareMode#EXCLUSIVE_READ_WRITE} from the caller.
     */
    @OnlyIn(Dist.CLIENT)
    public void read(PacketBuffer buf)
    {
        final BitArray bitArray = new BitArray(this.bitsPerEntry, this.arraySize);
        buf.readLongArray(bitArray.getBackingLongArray());
        this.read(bitArray);
    }
}
