package app.hongs.serv.record;

import app.hongs.HongsException;
import app.hongs.HongsUnchecked;
import app.hongs.cmdlet.CmdletHelper;
import app.hongs.cmdlet.anno.Cmdlet;
import app.hongs.db.DB;
import app.hongs.db.Table;
import app.hongs.util.Data;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

/**
 * 简单数据存储
 * @author Hongs
 */
@Cmdlet("common.record")
public class Record {

    private static Table getTable() throws HongsException {
        return DB.getInstance("common").getTable("record");
    }

    /**
     * 删除数据
     * @param key
     */
    public static void del(String key) {
        try {
            getTable().delete("id = ?",key);
        }
        catch (HongsException ex ) {
            throw ex.toUnchecked();
        }
    }

    /**
     * 获取数据
     * @param key
     * @return 可使用 Synt.asserts 来的到所需类型的数据
     */
    public static Object get(String key) {
        long now = System.currentTimeMillis() / 1000;

        try {
            Table tb = getTable();
            PreparedStatement ps = tb.db.prepareStatement(
                 "SELECT `data` FROM `" + tb.tableName + "` WHERE id = ? AND xtime > ?"
            );

            ps.setString(1, key);
            ps.setLong  (2, now);
            ps.setFetchSize(1);
            ps.setMaxRows  (1);
            ResultSet rs = ps.executeQuery();
            if (! rs.next()) {
                return null;
            }

            // 反序列化
                  InputStream ins =    rs.getBinaryStream( 1 );
            ObjectInputStream ois = new ObjectInputStream(ins);
            Object obj = ois.readObject();
            return obj;
        }
        catch (ClassNotFoundException ex) {
            throw new HongsUnchecked.Common(ex);
        }
        catch (SQLException ex) {
            throw new HongsUnchecked.Common(ex);
        }
        catch ( IOException ex) {
            throw new HongsUnchecked.Common(ex);
        }
        catch (HongsException ex ) {
            throw ex.toUnchecked();
        }
    }

    /**
     * 设置数据
     * @param key
     * @param val
     * @param exp 到期时间
     * @param now 存入时间
     */
    public static void set(String key, Object val, long exp, long now){
        del(key);

        try {
            Table tb = getTable();
            PreparedStatement ps = tb.db.prepareStatement(
                 "INSERT INTO `" + tb.tableName + "` (id, data, xtime, ctime) VALUES (?, ?, ?, ?)"
            );

            // 序列化值
            byte[] arr ;
            ByteArrayOutputStream bos = new ByteArrayOutputStream(   );
               ObjectOutputStream out = new    ObjectOutputStream(bos);
            out.writeObject ( val );
            arr = bos.toByteArray();
             ByteArrayInputStream bis = new  ByteArrayInputStream(arr);

            ps.setString(1, key);
            ps.setBinaryStream(2, bis);
            ps.setLong  (3, exp);
            ps.setLong  (4, now);
            ps.executeUpdate(  );
        }
        catch (SQLException ex ) {
            throw new HongsUnchecked.Common(ex);
        }
        catch ( IOException ex ) {
            throw new HongsUnchecked.Common(ex);
        }
        catch (HongsException ex ) {
            throw ex.toUnchecked();
        }
    }

    /**
     * 设置数据过多久后失效
     * @param key
     * @param val
     * @param exp 剩余秒数
     */
    public static void set(String key, Object val, long exp) {
        long now = System.currentTimeMillis() / 1000;
        /**/ exp = now + exp;
        set( key , val , exp, now);
    }

    /**
     * 设置数据到某天后失效
     * @param key
     * @param val
     * @param exp 过期日期
     */
    public static void set(String key, Object val, Date exp) {
        long now = System.currentTimeMillis() / 1000;
        long ext = exp.getTime(   ) / 1000;
        set( key , val , ext , now);
    }

    @Cmdlet("check")
    public static void check(String[] args) {
        if (args.length < 1) {
            CmdletHelper.println("Record ID required");
        }
        Data.dumps ( get(args[0]) );
    }

    @Cmdlet("clean")
    public static void clean(String[] args) {
        long now = System.currentTimeMillis() / 1000;
        try {
            getTable().delete("xtime != 0 AND xtime <= ?", now);
        }
        catch (HongsException ex) {
            throw new Error ( ex);
        }
    }

}
