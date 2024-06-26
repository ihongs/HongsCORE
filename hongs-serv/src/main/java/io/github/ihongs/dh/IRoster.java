package io.github.ihongs.dh;

import io.github.ihongs.CruxException;

/**
 * 简单数据存取接口
 * @author Hongs
 * @param <T>
 */
public interface IRoster<T> {

    /**
     * 获取数据
     * @param key
     * @return
     * @throws io.github.ihongs.CruxException
     */
    public T get(String key) throws CruxException;

    /**
     * 设置数据
     * @param key
     * @param val
     * @param exp 到期时间(秒), 0 表示长期
     * @throws io.github.ihongs.CruxException
     */
    public void set(String key, T val, long exp) throws CruxException;

    /**
     * 更新过期
     * @param key
     * @param exp 到期时间(秒), 0 表示长期
     * @throws io.github.ihongs.CruxException
     */
    public void set(String key, long exp) throws CruxException;

    /**
     * 删除数据
     * @param key
     * @throws io.github.ihongs.CruxException
     */
    public void del(String key) throws CruxException;

    /**
     * 清理数据
     * @param exp 到期时间(秒)
     * @throws io.github.ihongs.CruxException
     */
    public void del( long  exp) throws CruxException;

}
