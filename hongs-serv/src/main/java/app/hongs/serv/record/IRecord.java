package app.hongs.serv.record;

import app.hongs.HongsException;

/**
 * 简单数据存取接口
 * @author Hongs
 */
public interface IRecord {

    /**
     * 获取数据
     * @param key
     * @return
     * @throws app.hongs.HongsException
     */
    public Object get(String key) throws HongsException;

    /**
     * 设置数据
     * @param key
     * @param val
     * @param exp 到期时间(秒), 0 表示长期
     * @throws app.hongs.HongsException
     */
    public void set(String key, Object val, long exp) throws HongsException;

    /**
     * 更新过期
     * @param key
     * @param exp 到期时间(秒), 0 表示长期
     * @throws app.hongs.HongsException
     */
    public void set(String key, long exp) throws HongsException;

    /**
     * 删除数据
     * @param key
     * @throws app.hongs.HongsException
     */
    public void del(String key) throws HongsException;

    /**
     * 清理数据
     * @param exp 到期时间(秒)
     * @throws app.hongs.HongsException
     */
    public void del( long  exp) throws HongsException;

}
