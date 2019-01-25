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
 * Convenience overloads are provided for the access modes defined in {@link ocd.concurrent.AccessMode}.
 *
 * Most memory orderings for AtomicLongArray are only implemented since Java 9.
 * Hence, most methods below actually fallback to stronger memory orderings.
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

    private interface CASOperation
    {
        boolean compareAndSet(AtomicLongArray data, int index, long expectedValue, long newValue);
    }

    @Override
    public void setOpaque(int index, int value)
    {
        this.setShared(index, value, AtomicLongArray::weakCompareAndSet);
    }

    @Override
    public void setRelease(int index, int value)
    {
        this.setShared(index, value, AtomicLongArray::compareAndSet);
    }

    @Override
    public void setVolatile(int index, int value)
    {
        this.setShared(index, value, AtomicLongArray::compareAndSet);
    }

    private void setShared(final int index, final int value, final CASOperation operation)
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

    @Override
    public void setPlain(int index, int value)
    {
        this.setExclusive(index, value, AtomicLongArray::lazySet);
    }

    @Override
    public void setOpaqueExclusive(int index, int value)
    {
        this.setExclusive(index, value, AtomicLongArray::lazySet);
    }

    @Override
    public void setReleaseExclusive(int index, int value)
    {
        this.setExclusive(index, value, AtomicLongArray::lazySet);
    }

    @Override
    public void setVolatileExclusive(int index, int value)
    {
        this.setExclusive(index, value, AtomicLongArray::set);
    }

    private void setExclusive(final int index, final int value, final SetOperation operation)
    {
        Validate.inclusiveBetween(0, this.arraySize - 1, index);
        Validate.inclusiveBetween(0, this.maxEntryValue, value);

        final int i = index / this.entriesPerLong;
        final int shift = (index % this.entriesPerLong) * this.bitsPerEntry;

        // We are guaranteed exclusive write access. Hence we can use ordinary writes instead of CAS.
        long oldVal = this.data.get(i);
        operation.set(this.data, i, (oldVal & ~(this.maxEntryValue << shift)) | (((long) value) << shift));
    }

    // Placeholder for Java 9, which actually provides weaker access modes.
    private interface GetOperation
    {
        long get(AtomicLongArray data, int index);
    }

    @Override
    public int getPlain(int index)
    {
        return this.get(index, AtomicLongArray::get);
    }

    @Override
    public int getOpaque(int index)
    {
        return this.get(index, AtomicLongArray::get);
    }

    @Override
    public int getAcquire(int index)
    {
        return this.get(index, AtomicLongArray::get);
    }

    @Override
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

    /**
     * Copies the data from the specified bit array.
     * This method is NOT thread-safe and requires exclusivity guarantees as specified by {@link ocd.concurrent.ShareMode#EXCLUSIVE_READ_WRITE} from the caller.
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
     * This method is NOT thread-safe and requires exclusivity guarantees as specified by {@link ocd.concurrent.ShareMode#EXCLUSIVE_READ_WRITE} from the caller.
     */
    public void read(final long[] data)
    {
        this.read(new BitArray(this.bitsPerEntry, this.arraySize, data));
    }

    /**
     * Copies the data in the format specified by {@link BitArray}.
     * This method is NOT thread-safe and requires exclusivity guarantees as specified by {@link ocd.concurrent.ShareMode#EXCLUSIVE_READ_WRITE} from the caller.
     */
    @OnlyIn(Dist.CLIENT)
    public synchronized void read(PacketBuffer buf)
    {
        final BitArray bitArray = new BitArray(this.bitsPerEntry, this.arraySize);
        buf.readLongArray(bitArray.getBackingLongArray());
        this.read(bitArray);
    }
}
