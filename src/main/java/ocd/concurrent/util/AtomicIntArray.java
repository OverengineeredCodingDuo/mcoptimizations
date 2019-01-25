package ocd.concurrent.util;

import ocd.concurrent.AccessMode;
import ocd.concurrent.AtomicCollectionAccessor;
import ocd.concurrent.MemoryOrder;
import ocd.concurrent.util.AtomicIntArray.Accessor.Getter;
import ocd.concurrent.util.AtomicIntArray.Accessor.Setter;

public interface AtomicIntArray
{
    int getVolatile(int index);

    int getAcquire(int index);

    int getOpaque(int index);

    int getPlain(int index);

    void setVolatile(int index, int value);

    void setRelease(int index, int value);

    void setOpaque(int index, int value);

    void setVolatileExclusive(int index, int value);

    void setReleaseExclusive(int index, int value);

    void setOpaqueExclusive(int index, int value);

    void setPlain(int index, int value);

    default int get(int index, AccessMode.Read accessMode)
    {
        switch (accessMode)
        {
        case PLAIN:
        case OPAQUE:
            return this.getOpaque(index);
        case ACQUIRE:
            return this.getAcquire(index);
        case VOLATILE:
        default:
            return this.getVolatile(index);
        }
    }

    default int get(int index, MemoryOrder memoryOrder)
    {
        return Accessor.INSTANCE.get(memoryOrder).get(this, index);
    }

    default void set(int index, int value, AccessMode.Write accessMode)
    {
        Accessor.INSTANCE.set(accessMode).set(this, index, value);
    }

    default void set(int index, int value, MemoryOrder memoryOrder)
    {
        Accessor.INSTANCE.set(memoryOrder).set(this, index, value);
    }

    default void setExclusive(int index, int value, MemoryOrder memoryOrder)
    {
        Accessor.INSTANCE.setExclusive(memoryOrder).set(this, index, value);
    }

    class Accessor implements AtomicCollectionAccessor<Getter, Setter>
    {
        public static final Accessor INSTANCE = new Accessor();

        public interface Getter
        {
            int get(AtomicIntArray map, int index);
        }

        public interface Setter
        {
            void set(AtomicIntArray map, int index, int value);
        }

        @Override
        public Getter getVolatile()
        {
            return AtomicIntArray::getVolatile;
        }

        @Override
        public Getter getAcquire()
        {
            return AtomicIntArray::getAcquire;
        }

        @Override
        public Getter getOpaque()
        {
            return AtomicIntArray::getOpaque;
        }

        @Override
        public Getter getPlain()
        {
            return AtomicIntArray::getPlain;
        }

        @Override
        public Setter setVolatile()
        {
            return AtomicIntArray::setVolatile;
        }

        @Override
        public Setter setRelease()
        {
            return AtomicIntArray::setRelease;
        }

        @Override
        public Setter setOpaque()
        {
            return AtomicIntArray::setOpaque;
        }

        @Override
        public Setter setVolatileExclusive()
        {
            return AtomicIntArray::setVolatileExclusive;
        }

        @Override
        public Setter setReleaseExclusive()
        {
            return AtomicIntArray::setReleaseExclusive;
        }

        @Override
        public Setter setOpaqueExclusive()
        {
            return AtomicIntArray::setOpaqueExclusive;
        }

        @Override
        public Setter setPlain()
        {
            return AtomicIntArray::setPlain;
        }
    }
}
