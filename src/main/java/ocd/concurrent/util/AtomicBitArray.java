package ocd.concurrent.util;

import java.util.concurrent.atomic.AtomicLongArray;

import org.apache.commons.lang3.Validate;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.BitArray;
import net.minecraft.util.math.MathHelper;

public class AtomicBitArray extends BitArray
{
    private final AtomicLongArray data;
    private final int entriesPerLong;

    public AtomicBitArray(int bitsPerEntryIn, int arraySizeIn)
    {
        super(bitsPerEntryIn, arraySizeIn);
        this.entriesPerLong = 64 / bitsPerEntryIn;
        this.data = new AtomicLongArray(MathHelper.roundUp(arraySizeIn, this.entriesPerLong) / this.entriesPerLong);
    }

    public AtomicBitArray(int bitsPerEntryIn, int arraySizeIn, long[] data)
    {
        super(bitsPerEntryIn, arraySizeIn, data);
        this.entriesPerLong = 64 / bitsPerEntryIn;
        this.data = new AtomicLongArray(MathHelper.roundUp(arraySizeIn, this.entriesPerLong) / this.entriesPerLong);
        this.sync();
    }

    public synchronized void setAt(int index, int value)
    {
        super.setAt(index, value);
        this.setAt_(index, value);
    }

    private void setAt_(int index, int value)
    {
        Validate.inclusiveBetween(0, this.size() - 1, index);
        Validate.inclusiveBetween(0, this.maxEntryValue, value);
        final int i = index / this.entriesPerLong;
        final int shift = (index - i * this.entriesPerLong) * this.bitsPerEntry();
        this.data.lazySet(i, (this.data.get(i) & ~(this.maxEntryValue << shift)) | ((value & this.maxEntryValue) << shift));
    }

    public int getAt(int index)
    {
        Validate.inclusiveBetween(0, this.size() - 1, index);
        final int i = index / this.entriesPerLong;
        final int shift = (index - i * this.entriesPerLong) * this.bitsPerEntry();
        return (int) ((this.data.get(i) >>> shift) & this.maxEntryValue);
    }

    public synchronized void sync()
    {
        for (int i = 0; i < this.size(); ++i)
            this.setAt_(i, super.getAt(i));
    }

    public synchronized void read(PacketBuffer buf)
    {
        buf.readLongArray(this.getBackingLongArray());
        this.sync();
    }

    public synchronized void read(final long[] data)
    {
        System.arraycopy(data, 0, this.getBackingLongArray(), 0, data.length);
        this.sync();
    }
}
