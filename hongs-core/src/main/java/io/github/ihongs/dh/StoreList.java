package io.github.ihongs.dh;

import io.github.ihongs.CoreSerial;
import io.github.ihongs.HongsException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * 本地列表缓存
 * @author Hongs
 * @param <T>
 */
abstract public class StoreList<T> extends CoreSerial implements Collection<T> {

    protected Collection<T> list;

    public StoreList(String name, Collection list) throws HongsException {
        this.list = list;
        init(name ,  0 );
    }

    public StoreList(String name) throws HongsException {
        this(name , new ArrayList() );
    }

    /**
     * 是否已过期, 默认规则永不过期
     * @param time 缓存文件修改时间
     * @return 过期 true 反之 flase
     * @throws HongsException
     */
    @Override
    protected boolean expired(long time) throws HongsException {
        return false;
    }

    @Override
    protected void load(Object info) {
        list = (Collection<T>) info;
    }

    @Override
    protected Object save() {
        return list;
    }

    //* 下为 Collection 方法 */

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return list.contains(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return list.containsAll(c);
    }

    @Override
    public boolean add(T e) {
        return list.add(e);
    }

    @Override
    public boolean remove(Object o) {
        return list.remove(o);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        return list.addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return list.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return list.retainAll(c);
    }

    @Override
    public void clear() {
        list.clear();
    }

    @Override
    public Iterator<T> iterator( ) {
        return list.iterator( );
    }

    @Override
    public Object[] toArray( ) {
        return list.toArray( );
    }

    @Override
    public <T> T [] toArray(T[] a) {
        return list.toArray(a);
    }

}
