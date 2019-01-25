package ocd.concurrent.util;

import ocd.concurrent.AccessMode;
import ocd.concurrent.AtomicCollectionAccessor;
import ocd.concurrent.MemoryOrder;
import ocd.concurrent.util.AtomicObjectArray.Accessor.Getter;
import ocd.concurrent.util.AtomicObjectArray.Accessor.Setter;

public interface AtomicObjectArray<T>
{
    T getVolatile(int index);

    T getAcquire(int index);

    T getOpaque(int index);

    T getPlain(int index);

    void setVolatile(int index, T value);

    void setRelease(int index, T value);

    void setOpaque(int index, T value);

    void setVolatileExclusive(int index, T value);

    void setReleaseExclusive(int index, T value);

    void setOpaqueExclusive(int index, T value);

    void setPlain(int index, T value);

    default T get(int index, AccessMode.Read accessMode)
    {
        return Accessor.INSTANCE.get(accessMode).get(this, index);
    }

    default T get(int index, MemoryOrder memoryOrder)
    {
        return Accessor.INSTANCE.get(memoryOrder).get(this, index);
    }

    default void set(int index, T value, AccessMode.Write accessMode)
    {
        Accessor.INSTANCE.set(accessMode).set(this, index, value);
    }

    default void set(int index, T value, MemoryOrder memoryOrder)
    {
        Accessor.INSTANCE.set(memoryOrder).set(this, index, value);
    }

    default void setExclusive(int index, T value, MemoryOrder memoryOrder)
    {
        Accessor.INSTANCE.setExclusive(memoryOrder).set(this, index, value);
    }

    class Accessor implements AtomicCollectionAccessor<Getter, Setter>
    {
        public static final Accessor INSTANCE = new Accessor();

        public interface Getter
        {
            <T> T get(AtomicObjectArray<T> map, int index);
        }

        public interface Setter
        {
            <T> void set(AtomicObjectArray<T> map, int index, T value);
        }

        @Override
        public Getter getVolatile()
        {
            return AtomicObjectArray::getVolatile;
        }

        @Override
        public Getter getAcquire()
        {
            return AtomicObjectArray::getAcquire;
        }

        @Override
        public Getter getOpaque()
        {
            return AtomicObjectArray::getOpaque;
        }

        @Override
        public Getter getPlain()
        {
            return AtomicObjectArray::getPlain;
        }

        @Override
        public Setter setVolatile()
        {
            return AtomicObjectArray::setVolatile;
        }

        @Override
        public Setter setRelease()
        {
            return AtomicObjectArray::setRelease;
        }

        @Override
        public Setter setOpaque()
        {
            return AtomicObjectArray::setOpaque;
        }

        @Override
        public Setter setVolatileExclusive()
        {
            return AtomicObjectArray::setVolatileExclusive;
        }

        @Override
        public Setter setReleaseExclusive()
        {
            return AtomicObjectArray::setReleaseExclusive;
        }

        @Override
        public Setter setOpaqueExclusive()
        {
            return AtomicObjectArray::setOpaqueExclusive;
        }

        @Override
        public Setter setPlain()
        {
            return AtomicObjectArray::setPlain;
        }
    }
}
