package io.github.ihongs.normal.serv;

import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.HongsException;
import io.github.ihongs.combat.CombatHelper;
import io.github.ihongs.combat.anno.Combat;

/**
 * 简单数据存储
 * @author Hongs
 */
@Combat("normal.record")
public class Record {

    private static IRecord getRecord() throws HongsException {
        String cls = CoreConfig.getInstance().getProperty("core.normal.record.model");
        if (null == cls || 0 == cls.length()) {
               cls = JRecord.class.getName( );
        }
        return (IRecord)Core.getInstance(cls);
    }

    /**
     * 获取数据
     * @param key
     * @return 可用 Synt.declare 辅助得到所需类型
     */
    public static Object get(String key) {
        try {
            return getRecord( ).get(key);
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
            getRecord( ).set(key, val, exp);
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
            getRecord( ).set(key, val, exp);
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
            getRecord( ).set(key, exp);
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
            getRecord( ).set(key, exp);
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
            getRecord().del( key );
        }
        catch (HongsException ex ) {
            throw ex.toExemption();
        }
    }

    /**
     * 清除数据
     * @param exp
     */
    public static void del( long  exp) {
        try {
            getRecord().del( exp );
        }
        catch (HongsException ex ) {
            throw ex.toExemption();
        }
    }

    /**
     * 清除过期的数据
     * @param args
     */
    @Combat("clean")
    public static void clean(String[] args) {
        long exp = 0;
        if (args.length != 0) {
             exp = Integer.parseInt ( args  [ 0 ]  );
        }
        del(System.currentTimeMillis() / 1000 - exp);
    }

    /**
     * 预览存储的数据
     * @param args
     */
    @Combat("check")
    public static void check(String[] args) {
        if (args.length == 0) {
          CombatHelper.println("Record ID required");
        }
          CombatHelper.preview( get ( args [ 0 ] ) );
    }

}
