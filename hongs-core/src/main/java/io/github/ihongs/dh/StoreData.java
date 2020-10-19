package io.github.ihongs.dh;

import io.github.ihongs.CoreSerial;
import io.github.ihongs.HongsException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 本地字典缓存
 * @author Hongs
 * @param <K>
 * @param <V>
 */
abstract public class StoreData<K,V> extends CoreSerial implements Map<K,V> {

    protected Map<K,V> data;

    public StoreData(String name, Map data) throws HongsException {
        this.data = data;
        init(name);
    }

    public StoreData(String name) throws HongsException {
        this(name , new HashMap() );
    }

    @Override
    protected void load(Object info) {
        data = (Map<K,V>) info;
    }

    @Override
    protected Object save() {
        return data;
    }

    //* 下为 Map 方法 */

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return data.containsKey(key);
    }

    @Override
    public boolean containsValue(Object val) {
        return data.containsValue(val);
    }

    @Override
    public V get(Object key) {
        return data.get(key);
    }

    @Override
    public V put(K key , V val) {
        return data.put(key, val);
    }

    @Override
    public V remove(Object key) {
        return data.remove(key);
    }

    @Override
    public void putAll(Map map) {
        data.putAll(map);
    }

    @Override
    public void clear() {
        data.clear();
    }

    @Override
    public Set entrySet() {
        return data.entrySet();
    }

    @Override
    public Set keySet() {
        return data.keySet();
    }

    @Override
    public Collection values() {
        return data.values();
    }

}
