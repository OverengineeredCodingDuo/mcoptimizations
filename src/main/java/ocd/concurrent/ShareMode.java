package ocd.concurrent;

public enum ShareMode
{
    SHARED(0),
    EXCLUSIVE_WRITE(1),
    EXCLUSIVE_READ_WRITE(2);

    private final int level;

    public AccessMode.Read getReadAccessMode(MemoryOrder memoryOrder)
    {
        return AccessMode.Read.getAccessMode(memoryOrder, this);
    }

    public AccessMode.Write getWritAccessMode(MemoryOrder memoryOrder)
    {
        return AccessMode.Write.getAccessMode(memoryOrder, this);
    }

    ShareMode(final int level)
    {
        this.level = level;
    }

    public boolean allows(final ShareMode shareMode)
    {
        return this.level >= shareMode.level;
    }

    public ShareMode restrict(final ShareMode shareMode)
    {
        return shareMode.allows(this) ? this : shareMode;
    }
}
