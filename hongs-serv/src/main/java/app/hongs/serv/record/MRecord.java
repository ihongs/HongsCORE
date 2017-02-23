package app.hongs.serv.record;

import app.hongs.HongsException;
import app.hongs.db.DB;
import app.hongs.db.Table;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 简单数据存取模型
 * @author Hongs
 */
public class MRecord implements IRecord {

    private final Table table;

    public MRecord() throws HongsException {
        table = DB.getInstance("common").getTable("record");
    }

    /**
     * 获取数据
     * @param key
     * @return
     * @throws app.hongs.HongsException
     */
    @Override
    public Object get(String key) throws HongsException {
        long now = System.currentTimeMillis() / 1000;

        try {
            PreparedStatement ps = table.db.prepareStatement(
          "SELECT `data` FROM `" + table.tableName + "` WHERE id = ? AND (xtime > ? OR xtime == 0)"
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
            throw new HongsException.Common(ex);
        }
        catch (SQLException ex) {
            throw new HongsException.Common(ex);
        }
        catch ( IOException ex) {
            throw new HongsException.Common(ex);
        }
    }

    /**
     * 设置数据
     * @param key
     * @param val
     * @param exp
     */
    @Override
    public void set(String key, Object val, long exp) throws HongsException {
        long now = System.currentTimeMillis() / 1000;

        del( key );

        try {
            PreparedStatement ps = table.db.prepareStatement(
                 "INSERT INTO `" + table.tableName + "` (id, data, xtime, ctime) VALUES (?, ?, ?, ?)"
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
            throw new HongsException.Common(ex);
        }
        catch ( IOException ex ) {
            throw new HongsException.Common(ex);
        }
    }

    /**
     * 设置过期
     * @param key
     * @param exp
     * @throws HongsException
     */
    @Override
    public void set(String key, long exp) throws HongsException {
        try {
            PreparedStatement ps = table.db.prepareStatement(
                 "UPDATE `" + table.tableName + "` SET xtime = ? WHERE id = ?"
            );

            ps.setString(2, key);
            ps.setLong  (1, exp);
            ps.executeUpdate(  );
        }
        catch (SQLException ex ) {
            throw new HongsException.Common(ex);
        }
    }

    /**
     * 删除数据
     * @param key
     */
    @Override
    public void del(String key) throws HongsException {
        table.delete("id = ?", key);
    }

    /**
     * 清除数据
     * @param exp
     */
    @Override
    public void del( long  exp) throws HongsException {
        table.delete("xtime <= ? AND xtime != 0", exp);
    }

}
