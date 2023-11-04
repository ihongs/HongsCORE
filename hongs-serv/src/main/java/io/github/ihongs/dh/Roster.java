package io.github.ihongs.dh;

import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.HongsException;

/**
 * 简单数据存储
 * @author Hongs
 */
public class Roster {

    private static IRoster getModel() throws HongsException {
        String cls = CoreConfig.getInstance().getProperty("core.normal.roster.model");
        if (null == cls || 0 == cls.length()) {
               cls = JRoster.class.getName( );
        }
        return (IRoster)Core.getInstance(cls);
    }

    /**
     * 获取数据
     * @param key
     * @return 可用 Synt.declare 辅助得到所需类型
     */
    public static Object get(String key) {
        try {
            return getModel().get(key);
        }
        catch (HongsException ex ) {
            throw ex.toExemption();
        }
    }

    /**
     * 设置数据在某时间失效
     * @param key
     * @param val
     * @param exp 到期时间(秒)
     */
    public static void set(String key, Object val, long exp) {
        try {
            getModel().set(key, val, exp);
        }
        catch (HongsException ex ) {
            throw ex.toExemption();
        }
    }

    /**
     * 设置数据过多久后失效
     * @param key
     * @param val
     * @param exp 剩余时间(秒)
     */
    public static void put(String key, Object val, long exp) {
        exp += System.currentTimeMillis() / 1000;
        try {
            getModel().set(key, val, exp);
        }
        catch (HongsException ex ) {
            throw ex.toExemption();
        }
    }

    /**
     * 设置数据在某时间失效
     * @param key
     * @param exp 到期时间(秒)
     */
    public static void set(String key, long exp) {
        try {
            getModel().set(key, exp);
        }
        catch (HongsException ex ) {
            throw ex.toExemption();
        }
    }

    /**
     * 设置数据过多久后失效
     * @param key
     * @param exp 剩余时间(秒)
     */
    public static void put(String key, long exp) {
        exp += System.currentTimeMillis() / 1000;
        try {
            getModel().set(key, exp);
        }
        catch (HongsException ex ) {
            throw ex.toExemption();
        }
    }

    /**
     * 删除数据
     * @param key
     */
    public static void del(String key) {
        try {
            getModel().del(key);
        }
        catch (HongsException ex ) {
            throw ex.toExemption();
        }
    }

    /**
     * 清除数据
     * @param exp
     */
    public static void del(long exp) {
        try {
            getModel().del(exp);
        }
        catch (HongsException ex ) {
            throw ex.toExemption();
        }
    }

}
