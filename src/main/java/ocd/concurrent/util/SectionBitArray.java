package ocd.concurrent.util;

/**
 * Specialization of {@link AtomicBitArray} for a fixed array size of 4096.
 * This implements {@link #getIndex(int)} and {@link #getShift(int)} using lookup tables, instead of integer division.
 */
public class SectionBitArray extends AtomicBitArray
{
    private static final int ARRAY_SIZE = 4096;

    // Use small data types, so we don't pollute the cache too much.
    // Most entries are actually not needed in practice. However, they won't end up in the cache, so they don't bother us.
    private static final short[] indices = new short[ARRAY_SIZE * 64];
    private static final byte[] shifts = new byte[ARRAY_SIZE * 64];

    static
    {
        // Most entries won't occur in practice. However, since they don't pollute RAM or cache, they don't bother us.
        for (int bitsPerEntry = 1; bitsPerEntry <= 64; ++bitsPerEntry)
        {
            final int entriesPerLong = 64 / bitsPerEntry;

            for (int index = 0; index < ARRAY_SIZE; ++index)
            {
                indices[getCacheIndex(index, bitsPerEntry)] = (short) getIndex(index, entriesPerLong);
                shifts[getCacheIndex(index, bitsPerEntry)] = (byte) getShift(index, bitsPerEntry, entriesPerLong);
            }
        }
    }

    public SectionBitArray(final int bitsPerEntryIn)
    {
        super(bitsPerEntryIn, ARRAY_SIZE);
    }

    private static int getCacheIndex(final int index, final int bitsPerEntry)
    {
        return (bitsPerEntry - 1) * ARRAY_SIZE + index;
    }

    @Override
    protected int getIndex(final int index)
    {
        return indices[getCacheIndex(index, this.bitsPerEntry)];
    }

    @Override
    protected int getShift(final int index)
    {
        return shifts[getCacheIndex(index, this.bitsPerEntry)];
    }
}
