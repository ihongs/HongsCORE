package io.github.ihongs.serv;

import io.github.ihongs.dh.Roster;

/**
 * 简单数据存储
 * @author Hongs
 * @deprecated 与 java.lang.Record 重名, 已迁移到 io.github.ihongs.dh.Roster
 */
public class Record {

    /**
     * 获取数据
     * @param key
     * @return 可用 Synt.declare 辅助得到所需类型
     * @deprecated 改用 Stores.get
     */
    public static Object get(String key) {
        return Roster.get(key);
    }

    /**
     * 设置数据在某时间失效
     * @param key
     * @param val
     * @param exp 到期时间(秒)
     * @deprecated 改用 Stores.set
     */
    public static void set(String key, Object val, long exp) {
        Roster.set(key, val, exp);
    }

    /**
     * 设置数据过多久后失效
     * @param key
     * @param val
     * @param exp 剩余时间(秒)
     * @deprecated 改用 Stores.put
     */
    public static void put(String key, Object val, long exp) {
        Roster.put(key, val, exp);
    }

    /**
     * 设置数据在某时间失效
     * @param key
     * @param exp 到期时间(秒)
     * @deprecated 改用 Stores.set
     */
    public static void set(String key, long exp) {
        Roster.set(key, exp);
    }

    /**
     * 设置数据过多久后失效
     * @param key
     * @param exp 剩余时间(秒)
     * @deprecated 改用 Stores.put
     */
    public static void put(String key, long exp) {
        Roster.put(key, exp);
    }

    /**
     * 删除数据
     * @param key
     * @deprecated 改用 Stores.del
     */
    public static void del(String key) {
        Roster.del(key);
    }

    /**
     * 清除数据
     * @param exp
     * @deprecated 改用 Stores.del
     */
    public static void del(long exp) {
        Roster.del(exp);
    }

}
