package COMP3015_Project_1.Common;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

abstract class SelfHashPool<T> {
    private ConcurrentHashMap<Integer, T> m = new ConcurrentHashMap<>();

    String putNew(T t) {
        String hash = Integer.toHexString(t.hashCode()).intern();
        m.put(t.hashCode(), t);
        return hash;
    }


    public T remove(T t) {
        return m.remove(t);
    }

    public int size() {
        return m.size();
    }

    public Collection<T> getAll() {
        return m.values();
    }
}
