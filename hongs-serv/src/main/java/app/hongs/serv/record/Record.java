package app.hongs.serv.record;

import app.hongs.Core;
import app.hongs.CoreConfig;
import app.hongs.HongsException;
import app.hongs.cmdlet.CmdletHelper;
import app.hongs.cmdlet.anno.Cmdlet;
import app.hongs.util.Data;
import app.hongs.util.Synt;

/**
 * 简单数据存储
 * @author Hongs
 */
@Cmdlet("common.record")
public class Record {

    private static IRecord getRecord() throws HongsException {
        String clsn = CoreConfig.getInstance().getProperty("core.common.record.class", MRecord.class.getName());
        return Synt.declare(Core.getInstance(clsn), MRecord.class);
    }

    /**
     * 获取数据
     * @param key
     * @return 可使用 Synt.declare 来的到所需类型的数据
     */
    public static Object get(String key) {
        try {
            return getRecord( ).get(key);
        }
        catch (HongsException ex ) {
            throw ex.toUnchecked();
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
            throw ex.toUnchecked();
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
            throw ex.toUnchecked();
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
            throw ex.toUnchecked();
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
            throw ex.toUnchecked();
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
            throw ex.toUnchecked();
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
            throw ex.toUnchecked();
        }
    }

    @Cmdlet("clean")
    public static void clean(String[] args) {
        long exp = 0;
        if ( args.length == 0 ) {
             exp = Integer.parseInt ( args  [ 0 ]  );
        }
        del(System.currentTimeMillis() / 1000 - exp);
    }

    @Cmdlet("check")
    public static void check(String[] args) {
        if ( args.length == 0 ) {
            CmdletHelper.println("Record ID required");
        }
        Data.dumps(get(args[0]));
    }

}
