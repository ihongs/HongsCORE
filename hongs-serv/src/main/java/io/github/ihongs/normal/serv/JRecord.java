package io.github.ihongs.normal.serv;

import io.github.ihongs.HongsException;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Table;
import io.github.ihongs.db.link.Loop;
import io.github.ihongs.util.Dawn;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.sql.Types;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 简单数据存取模型
 * @author Hongs
 * @param <T>
 */
public class JRecord<T> implements IRecord<T>, AutoCloseable {

    protected final boolean bytes;
    protected final   Table table;

    protected JRecord(Table table) throws HongsException {
        this. table = table;

        // 判断是采用序列化还是 JSON
        Map fs =  table.getFields();
        Map df = (Map    ) fs.get("data");
        int dt = (Integer) df.get("type");
        bytes  =  dt == Types.BLOB || dt == Types.BINARY ;
    }

    public JRecord() throws HongsException {
        this(DB.getInstance("normal").getTable("record"));
    }

    /**
     * 获取数据
     * @param key
     * @return
     * @throws io.github.ihongs.HongsException
     */
    @Override
    public T get(String key) throws HongsException {
        long now = System.currentTimeMillis() / 1000;

        try (
            Loop lp = table.db.query("SELECT `data` FROM `"
                    + table.tableName
                    + "` WHERE id = ? AND (xtime > ? OR xtime = 0)"
            , 0,1, key, now);
        ) {
            ResultSet rs = lp.getReusltSet ();
            if (! rs.next()) {
                return null;
            }

            // 反序列化
            if (! bytes) {
                return ( T ) Dawn.toObject(rs.getString( 1 ));
            }
            else try (
                      InputStream ins =    rs.getBinaryStream( 1 );
                ObjectInputStream ois = new ObjectInputStream(ins);
            ) {
                return ( T ) ois.readObject(  );
            }
        }
        catch (SQLException ex) {
            throw new  HongsException(1152, ex);
        }
        catch ( IOException ex) {
            throw new  HongsException(ex);
        }
        catch (ClassNotFoundException ex) {
            throw new  HongsException(ex);
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
        // 序列化值
        byte[] arr = null;
        String str = null;
        if (! bytes) {
            str = Dawn.toString ( val );
        }
        else try (
            ByteArrayOutputStream bos = new ByteArrayOutputStream(   );
               ObjectOutputStream out = new    ObjectOutputStream(bos);
        ) {
            out.writeObject ( val );
            out.flush();
            arr = bos.toByteArray();
        }
        catch (IOException e) {
            throw new HongsException(e);
        }

        long now = System.currentTimeMillis() / 1000;

//      table.db.open( );
        table.db.ready();

        try (
            PreparedStatement ps = table.db.prepareStatement(
                      "UPDATE `" + table.tableName + "` SET data= ?, xtime= ?, mtime= ? WHERE id = ?"
            );
        ) {
            if (arr != null) {
                ps.setBytes (1, arr);
            } else {
                ps.setString(1, str);
            }
            ps.setLong  (2, exp);
            ps.setLong  (3, now);
            ps.setString(4, key);
            if (ps.executeUpdate() > 0) {
               return;
            }
        }
        catch ( SQLException e ) {
            throw new HongsException(1045, e);
        }

        try (
            PreparedStatement ps = table.db.prepareStatement(
                 "INSERT INTO `" + table.tableName + "` (data, xtime, mtime, id) VALUES (?, ?, ?, ?)"
            );
        ) {
            if (arr != null) {
                ps.setBytes (1, arr);
            } else {
                ps.setString(1, str);
            }
            ps.setLong  (2, exp);
            ps.setLong  (3, now);
            ps.setString(4, key);
            if (ps.executeUpdate() > 0) {
               return;
            }
        }
        catch ( SQLException e ) {
            throw new HongsException(1045, e);
        }
    }

    /**
     * 设置过期
     * @param key
     * @param exp
     * @throws io.github.ihongs.HongsException
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
            ps.setLong  (1, exp);
            ps.setLong  (2, now);
            ps.setString(3, key);
            ps.executeUpdate(  );
        }
        catch ( SQLException e ) {
            throw new HongsException(1045, e);
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
        catch ( SQLException e ) {
            throw new HongsException(1045, e);
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
                 "DELETE FROM `" + table.tableName + "` WHERE xtime <= ? AND xtime != 0"
            );
        ) {
            ps.setLong  (1, exp);
            ps.executeUpdate(  );
        }
        catch ( SQLException e ) {
            throw new HongsException(1045, e);
        }
    }

    @Override
    public void close() throws HongsException {
        table.db.close();
    }

}
