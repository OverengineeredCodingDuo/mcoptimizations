package ocd.concurrent;

public enum MemoryOrder
{
    VOLATILE(3),
    ACQ_REL(2),
    OPAQUE(1),
    NONE(0);

    private final int level;

    public AccessMode.Read getReadAccessMode()
    {
        return AccessMode.Read.getAccessMode(this, ShareMode.SHARED);
    }

    public AccessMode.Write getWriteAccessMode()
    {
        return AccessMode.Write.getAccessMode(this, ShareMode.SHARED);
    }

    public AccessMode.Write getExclusiveWriteAccessMode()
    {
        return AccessMode.Write.getAccessMode(this, ShareMode.EXCLUSIVE_WRITE);
    }

    public <R> R getGetter(final AtomicCollectionAccessor<R, ?> coll)
    {
        switch (this)
        {
        case NONE:
        case OPAQUE:
            return coll.getOpaque();
        case ACQ_REL:
            return coll.getAcquire();
        default:
            return coll.getVolatile();
        }
    }

    public <W> W getSetter(final AtomicCollectionAccessor<?, W> coll)
    {
        switch (this)
        {
        case NONE:
        case OPAQUE:
            return coll.setOpaque();
        case ACQ_REL:
            return coll.setRelease();
        default:
            return coll.setVolatile();
        }
    }

    public <W> W getExclusiveSetter(final AtomicCollectionAccessor<?, W> coll)
    {
        switch (this)
        {
        case NONE:
        case OPAQUE:
            return coll.setOpaqueExclusive();
        case ACQ_REL:
            return coll.setReleaseExclusive();
        default:
            return coll.setVolatileExclusive();
        }
    }

    MemoryOrder(final int level)
    {
        this.level = level;
    }

    public boolean implies(final MemoryOrder memoryOrder)
    {
        return this.level >= memoryOrder.level;
    }

    public MemoryOrder enforce(final MemoryOrder memoryOrder)
    {
        return this.implies(memoryOrder) ? this : memoryOrder;
    }
}
