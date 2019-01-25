package ocd.concurrent;

public interface AtomicCollectionAccessor<R, W>
{
    default R get(AccessMode.Read accessMode)
    {
        return accessMode.getGetter(this);
    }

    default R get(MemoryOrder memoryOrder)
    {
        return memoryOrder.getGetter(this);
    }

    R getVolatile();

    R getAcquire();

    R getOpaque();

    R getPlain();

    default W set(AccessMode.Write accessMode)
    {
        return accessMode.getSetter(this);
    }

    default W set(MemoryOrder memoryOrder)
    {
        return memoryOrder.getSetter(this);
    }

    default W setExclusive(MemoryOrder memoryOrder)
    {
        return memoryOrder.getExclusiveSetter(this);
    }

    W setVolatile();

    W setRelease();

    W setOpaque();

    W setVolatileExclusive();

    W setReleaseExclusive();

    W setOpaqueExclusive();

    W setPlain();
}
