package app.hongs.serv.common;

import app.hongs.HongsException;
import app.hongs.db.DB;
import app.hongs.db.Table;
import app.hongs.db.link.Loop;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 简单数据存取模型
 * @author Hongs
 * @param <T>
 */
public class JRecord<T> implements IRecord<T>, AutoCloseable {

    protected  final  Table table;

    protected JRecord(Table table) throws HongsException {
        this.table =  table;
    }

    public JRecord() throws HongsException {
        this(DB.getInstance("common").getTable("record"));
    }

    /**
     * 获取数据
     * @param key
     * @return
     * @throws app.hongs.HongsException
     */
    @Override
    public T get(String key) throws HongsException {
        long now = System.currentTimeMillis() / 1000;

        try (
            Loop lp = table.db.query("SELECT `data` FROM `"
                    + table.tableName
                    + "` WHERE id = ? AND (xtime > ? OR xtime == 0)"
            , 0,1, key, now);
        ) {
            ResultSet rs = lp.getReusltSet ();
            if (! rs.next()) {
                return null;
            }

            // 反序列化
            try (
                      InputStream ins =    rs.getBinaryStream( 1 );
                ObjectInputStream ois = new ObjectInputStream(ins);
            ) {
                return ( T ) ois.readObject(  );
            }
        }
        catch (SQLException ex) {
            throw new HongsException.Common(ex);
        }
        catch ( IOException ex) {
            throw new HongsException.Common(ex);
        }
        catch (ClassNotFoundException ex) {
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
    public void set(String key, T val, long exp) throws HongsException {
        long now = System.currentTimeMillis() / 1000;

        // 序列化值
        byte[] arr;
        try (
            ByteArrayOutputStream bos = new ByteArrayOutputStream(   );
               ObjectOutputStream out = new    ObjectOutputStream(bos);
        ) {
            out.writeObject ( val );
            out.flush();
            arr = bos.toByteArray();
        }
        catch (IOException ex) {
            throw new HongsException.Common(ex);
        }

//      table.db.open( );
        table.db.ready();

        try (
            PreparedStatement ps = table.db.prepareStatement(
                 "DELETE FROM `" + table.tableName + "` WHERE id = ?"
            );
        ) {
            ps.setString(1, key);
            ps.executeUpdate(  );
        }
        catch (SQLException ex ) {
            throw new HongsException.Common(ex);
        }

        try (
            PreparedStatement ps = table.db.prepareStatement(
                 "INSERT INTO `" + table.tableName + "` (id, data, xtime, mtime) VALUES (?, ?, ?, ?)"
            );
        ) {
            ps.setString(1, key);
            ps.setBytes (2, arr);
            ps.setLong  (3, exp);
            ps.setLong  (4, now);
            ps.executeUpdate(  );
        }
        catch (SQLException ex ) {
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
        long now = System.currentTimeMillis() / 1000;

//      table.db.open( );
        table.db.ready();

        try (
            PreparedStatement ps = table.db.prepareStatement(
                      "UPDATE `" + table.tableName + "` SET xtime = ?, mtime = ? WHERE id = ?"
            );
        ) {
            ps.setString(3, key);
            ps.setLong  (1, exp);
            ps.setLong  (2, now);
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
//      table.db.open( );
        table.db.ready();

        try (
            PreparedStatement ps = table.db.prepareStatement(
                 "DELETE FROM `" + table.tableName + "` WHERE id = ?"
            );
        ) {
            ps.setString(1, key);
            ps.executeUpdate(  );
        }
        catch (SQLException ex ) {
            throw new HongsException.Common(ex);
        }
    }

    /**
     * 清除数据
     * @param exp
     */
    @Override
    public void del( long  exp) throws HongsException {
//      table.db.open( );
        table.db.ready();

        try (
            PreparedStatement ps = table.db.prepareStatement(
                 "DELETE FROM `" + table.tableName + "` WHERE xtime <= ? AND xtime != ?"
            );
        ) {
            ps.setLong  (1, exp);
            ps.executeUpdate(  );
        }
        catch (SQLException ex ) {
            throw new HongsException.Common(ex);
        }
    }

    @Override
    public void close() throws HongsException {
        table.db.close();
    }

}
