package ocd.concurrent;

public class AccessMode
{
    public enum Read
    {
        VOLATILE(MemoryOrder.VOLATILE, ShareMode.SHARED),
        ACQUIRE(MemoryOrder.ACQ_REL, ShareMode.SHARED),
        OPAQUE(MemoryOrder.OPAQUE, ShareMode.SHARED),
        PLAIN(MemoryOrder.NONE, ShareMode.EXCLUSIVE_WRITE);

        public final MemoryOrder memoryOrder;
        public final ShareMode shareMode;
        private static final Read modes[];

        static
        {
            modes = new Read[12];

            for (ShareMode shareMode : ShareMode.values())
            {
                modes[getIndex(MemoryOrder.NONE, shareMode)] = shareMode.allows(ShareMode.EXCLUSIVE_WRITE) ? PLAIN : OPAQUE;
                modes[getIndex(MemoryOrder.OPAQUE, shareMode)] = OPAQUE;
                modes[getIndex(MemoryOrder.ACQ_REL, shareMode)] = ACQUIRE;
                modes[getIndex(MemoryOrder.VOLATILE, shareMode)] = VOLATILE;
            }
        }

        Read(final MemoryOrder memoryOrder, final ShareMode shareMode)
        {
            this.memoryOrder = memoryOrder;
            this.shareMode = shareMode;
        }

        public <R> R getGetter(AtomicCollectionAccessor<R, ?> coll)
        {
            switch (this)
            {
            case PLAIN:
                return coll.getPlain();
            case OPAQUE:
                return coll.getOpaque();
            case ACQUIRE:
                return coll.getAcquire();
            default:
                return coll.getVolatile();
            }
        }

        public Read enfore(final MemoryOrder memoryOrder)
        {
            if (this.memoryOrder.implies(memoryOrder))
                return this;

            return getAccessMode(this.memoryOrder.enforce(memoryOrder), this.shareMode);
        }

        public Read restrict(final ShareMode shareMode)
        {
            if (shareMode.allows(this.shareMode))
                return this;

            return getAccessMode(this.memoryOrder, this.shareMode.restrict(shareMode));
        }

        private static int getIndex(final MemoryOrder memoryOrder, final ShareMode shareMode)
        {
            return (shareMode.ordinal() << 2) + memoryOrder.ordinal();
        }

        public static Read getAccessMode(final MemoryOrder memoryOrder, final ShareMode shareMode)
        {
            return modes[getIndex(memoryOrder, shareMode)];
        }
    }

    public enum Write
    {
        VOLATILE(MemoryOrder.VOLATILE, ShareMode.SHARED),
        RELEASE(MemoryOrder.ACQ_REL, ShareMode.SHARED),
        OPAQUE(MemoryOrder.OPAQUE, ShareMode.SHARED),
        VOLATILE_EXCLUSIVE(MemoryOrder.VOLATILE, ShareMode.EXCLUSIVE_WRITE),
        RELEASE_EXCLUSIVE(MemoryOrder.ACQ_REL, ShareMode.EXCLUSIVE_WRITE),
        OPAQUE_EXCLUSIVE(MemoryOrder.OPAQUE, ShareMode.EXCLUSIVE_WRITE),
        PLAIN(MemoryOrder.NONE, ShareMode.EXCLUSIVE_READ_WRITE);

        public final MemoryOrder memoryOrder;
        public final ShareMode shareMode;

        Write(final MemoryOrder memoryOrder, final ShareMode shareMode)
        {
            this.memoryOrder = memoryOrder;
            this.shareMode = shareMode;
        }

        public <W> W getSetter(AtomicCollectionAccessor<?, W> coll)
        {
            switch (this)
            {
            case PLAIN:
                return coll.setPlain();
            case OPAQUE_EXCLUSIVE:
                return coll.setOpaqueExclusive();
            case RELEASE_EXCLUSIVE:
                return coll.setReleaseExclusive();
            case VOLATILE_EXCLUSIVE:
                return coll.setVolatileExclusive();
            case OPAQUE:
                return coll.setOpaque();
            case RELEASE:
                return coll.setRelease();
            default:
                return coll.setVolatile();
            }
        }

        public Write enfore(final MemoryOrder memoryOrder)
        {
            if (this.memoryOrder.implies(memoryOrder))
                return this;

            return getAccessMode(this.memoryOrder.enforce(memoryOrder), this.shareMode);
        }

        public Write restrict(final ShareMode shareMode)
        {
            if (shareMode.allows(this.shareMode))
                return this;

            return getAccessMode(this.memoryOrder, this.shareMode.restrict(shareMode));
        }

        public static Write getAccessMode(final MemoryOrder memoryOrder, final ShareMode shareMode)
        {
            if (shareMode == ShareMode.EXCLUSIVE_READ_WRITE && memoryOrder == MemoryOrder.NONE)
                return PLAIN;

            if (shareMode == ShareMode.SHARED)
            {
                switch (memoryOrder)
                {
                case NONE:
                case OPAQUE:
                    return OPAQUE;
                case ACQ_REL:
                    return RELEASE;
                default:
                    return VOLATILE;
                }
            }

            switch (memoryOrder)
            {
            case NONE:
            case OPAQUE:
                return OPAQUE_EXCLUSIVE;
            case ACQ_REL:
                return RELEASE_EXCLUSIVE;
            default:
                return VOLATILE_EXCLUSIVE;
            }
        }
    }
}
