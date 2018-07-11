package foo.hongs.normal.serv;

import foo.hongs.HongsException;

/**
 * 简单数据存取接口
 * @author Hongs
 * @param <T>
 */
public interface IRecord<T> {

    /**
     * 获取数据
     * @param key
     * @return
     * @throws foo.hongs.HongsException
     */
    public T get(String key) throws HongsException;

    /**
     * 设置数据
     * @param key
     * @param val
     * @param exp 到期时间(秒), 0 表示长期
     * @throws foo.hongs.HongsException
     */
    public void set(String key, T val, long exp) throws HongsException;

    /**
     * 更新过期
     * @param key
     * @param exp 到期时间(秒), 0 表示长期
     * @throws foo.hongs.HongsException
     */
    public void set(String key, long exp) throws HongsException;

    /**
     * 删除数据
     * @param key
     * @throws foo.hongs.HongsException
     */
    public void del(String key) throws HongsException;

    /**
     * 清理数据
     * @param exp 到期时间(秒)
     * @throws foo.hongs.HongsException
     */
    public void del( long  exp) throws HongsException;

}
