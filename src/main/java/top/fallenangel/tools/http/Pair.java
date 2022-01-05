package top.fallenangel.tools.http;

import lombok.*;

/**
 * 一个键值对，把一个键映射到一个值
 */
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class Pair<K, V> {
    private K key;
    private V value;

    private Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    /**
     * 生成一个键值对
     *
     * @param key   键
     * @param value 值
     */
    public static <K, V> Pair<K, V> of(K key, V value) {
        return new Pair<>(key, value);
    }
}
