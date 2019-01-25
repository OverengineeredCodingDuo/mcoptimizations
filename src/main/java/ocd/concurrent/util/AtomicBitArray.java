package ocd.concurrent.util;

import java.util.concurrent.atomic.AtomicLongArray;

import org.apache.commons.lang3.Validate;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.BitArray;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Thread-safe version of {@link BitArray}.
 * This uses a different packing format than {@link BitArray} to make sure that each entry is contained inside a single long.
 * This allows accessing the entry atomically.
 * Reads and writes have the memory effects of volatile reads and writes respectively.
 * In particular, all accesses together with all accesses of volatile variables have a coherent ordering among all threads.
 */
public class AtomicBitArray implements AtomicIntArray
{
    private final AtomicLongArray data;
    private final int bitsPerEntry;
    private final int entriesPerLong;
    private final long maxEntryValue;
    private final int arraySize;

    public AtomicBitArray(int bitsPerEntryIn, int arraySizeIn)
    {
        this.bitsPerEntry = bitsPerEntryIn;
        this.entriesPerLong = 64 / bitsPerEntryIn;
        this.arraySize = arraySizeIn;
        this.maxEntryValue = (1L << bitsPerEntryIn) - 1;
        this.data = new AtomicLongArray(MathHelper.roundUp(arraySizeIn, this.entriesPerLong) / this.entriesPerLong);
    }

    public AtomicBitArray(int bitsPerEntryIn, int arraySizeIn, long[] data)
    {
        this(bitsPerEntryIn, arraySizeIn);
        this.read(data);
    }

    private interface CASOperation
    {
        boolean compareAndSet(AtomicLongArray data, int index, long expectedValue, long newValue);
    }

    /**
     * Sets the given value at the given index atomically.
     * Has the memory effect of a volatile write.
     */
    public void setOpaque(int index, int value)
    {
        this.set(index, value, AtomicLongArray::weakCompareAndSet);
    }

    /**
     * Sets the given value at the given index atomically.
     * Has the memory effect of a volatile write.
     */
    public void setRelease(int index, int value)
    {
        this.set(index, value, AtomicLongArray::compareAndSet);
    }

    /**
     * Sets the given value at the given index atomically.
     * Has the memory effect of a volatile write.
     */
    public void setVolatile(int index, int value)
    {
        this.set(index, value, AtomicLongArray::compareAndSet);
    }

    private void set(final int index, final int value, final CASOperation operation)
    {
        Validate.inclusiveBetween(0, this.arraySize - 1, index);
        Validate.inclusiveBetween(0, this.maxEntryValue, value);

        final int i = index / this.entriesPerLong;
        final int shift = (index % this.entriesPerLong) * this.bitsPerEntry;

        // Use CAS to set the entry atomically.
        long oldVal;
        do
        {
            oldVal = this.data.get(i);
        } while (!operation.compareAndSet(this.data, i, oldVal, (oldVal & ~(this.maxEntryValue << shift)) | (((long) value) << shift)));
    }

    private interface SetOperation
    {
        void set(AtomicLongArray data, int index, long val);
    }

    /**
     * Sets the given value at the given index atomically.
     * Has the memory effect of a volatile write.
     */
    public void setPlain(int index, int value)
    {
        this.set(index, value, AtomicLongArray::lazySet);
    }

    /**
     * Sets the given value at the given index atomically.
     * Has the memory effect of a volatile write.
     */
    public void setOpaqueExclusive(int index, int value)
    {
        this.set(index, value, AtomicLongArray::lazySet);
    }

    /**
     * Sets the given value at the given index atomically.
     * Has the memory effect of a volatile write.
     */
    public void setReleaseExclusive(int index, int value)
    {
        this.set(index, value, AtomicLongArray::lazySet);
    }

    /**
     * Sets the given value at the given index atomically.
     * Has the memory effect of a volatile write.
     */
    public void setVolatileExclusive(int index, int value)
    {
        this.set(index, value, AtomicLongArray::set);
    }

    private void set(final int index, final int value, final SetOperation operation)
    {
        Validate.inclusiveBetween(0, this.arraySize - 1, index);
        Validate.inclusiveBetween(0, this.maxEntryValue, value);

        final int i = index / this.entriesPerLong;
        final int shift = (index % this.entriesPerLong) * this.bitsPerEntry;

        long oldVal = this.data.get(i);
        operation.set(this.data, i, (oldVal & ~(this.maxEntryValue << shift)) | (((long) value) << shift));
    }

    private interface GetOperation
    {
        long get(AtomicLongArray data, int index);
    }

    /**
     * Reads the value at the given index atomically.
     * Has the memory effect of a volatile read.
     */
    public int getPlain(int index)
    {
        return this.get(index, AtomicLongArray::get);
    }

    /**
     * Reads the value at the given index atomically.
     * Has the memory effect of a volatile read.
     */
    public int getOpaque(int index)
    {
        return this.get(index, AtomicLongArray::get);
    }

    /**
     * Reads the value at the given index atomically.
     * Has the memory effect of a volatile read.
     */
    public int getAcquire(int index)
    {
        return this.get(index, AtomicLongArray::get);
    }

    /**
     * Reads the value at the given index atomically.
     * Has the memory effect of a volatile read.
     */
    public int getVolatile(int index)
    {
        return this.get(index, AtomicLongArray::get);
    }

    private int get(final int index, final GetOperation operation)
    {
        Validate.inclusiveBetween(0, this.arraySize - 1, index);
        final int i = index / this.entriesPerLong;
        final int shift = (index % this.entriesPerLong) * this.bitsPerEntry;
        return (int) ((operation.get(this.data, i) >>> shift) & this.maxEntryValue);
    }

    /**
     * Get a serialized long array for the data in the format specified by {@link BitArray}.
     * The returned array is not a snapshot of the data, ie. if there are concurrent modifications, it is unspecified which of those are reflected in the returned array.
     */
    public long[] getSerializedLongArray()
    {
        final BitArray bitArray = new BitArray(this.bitsPerEntry, this.arraySize);

        for (int i = 0; i < this.arraySize; ++i)
            bitArray.setAt(i, this.getOpaque(i));

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

    public int bitsPerEntry()
    {
        return this.bitsPerEntry;
    }

    public void read(final BitArray bitArray)
    {
        if (this.arraySize != bitArray.size())
            throw new IllegalArgumentException(String.format("Trying to read array of size %s into array of incompatible size %s", bitArray.size(), this.arraySize));

        for (int i = 0; i < this.arraySize; ++i)
            this.setPlain(i, bitArray.getAt(i));
    }

    /**
     * Reads the data in the format specified by {@link BitArray}.
     */
    public void read(final long[] data)
    {
        this.read(new BitArray(this.bitsPerEntry, this.arraySize, data));
    }

    /**
     * Reads the data in the format specified by {@link BitArray}.
     */
    @OnlyIn(Dist.CLIENT)
    public synchronized void read(PacketBuffer buf)
    {
        final BitArray bitArray = new BitArray(this.bitsPerEntry, this.arraySize);
        buf.readLongArray(bitArray.getBackingLongArray());
        this.read(bitArray);
    }
}
