package utils;

public class Pair<K, V> {
    K key;
    V value;

    public Pair(K key, V value) {
        this.value = value;
        this.key = key;
    }

    public V getValue() {
        return value;
    }

    public void setValue(V value) {
        this.value = value;
    }

    public K getKey() {
        return key;
    }

    public void setKey(K key) {
        this.key = key;
    }
}
