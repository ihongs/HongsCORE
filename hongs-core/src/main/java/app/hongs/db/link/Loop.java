package app.hongs.db.link;

import app.hongs.HongsException;
import app.hongs.HongsExemption;
import app.hongs.util.Dict;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.LinkedHashMap;

/**
 * 查询结果迭代
 *
 * 结束自动关闭 ResultSet 和 Statement, Statement 给 null 则请自行关闭
 *
 * @author Hong
 */
public class Loop implements Iterable<Map>, Iterator<Map>, AutoCloseable {

    private final Statement   ps;
    private final ResultSet   rs;
    private ResultSetMetaData md = null;
    private Map<String,Class> td = null;
    private       Boolean     il = null;
    private       boolean     ib;

    public Loop(ResultSet rs, Statement ps) throws HongsException {
        if (rs == null) {
            throw new NullPointerException("ResultSet can not be null");
        }
        this.ps = ps;
        this.rs = rs;
    }

    public void inObjectMode(boolean ib) {
        this.ib = ib;
    }

    public boolean isObjectMode() {
        return ib;
    }

    public Statement getStatement() {
        return ps;
    }

    public ResultSet getReusltSet() {
        return rs;
    }

    public ResultSetMetaData getMetaData() throws HongsException {
        if (md == null) {
            try {
                md = rs.getMetaData();
            } catch (SQLException ex) {
                throw new HongsException(0x10a0, ex);
            }
        }
        return md;
    }

    public Map<String,Class> getTypeDict() throws HongsException {
        if (td == null) {
            getMetaData();
            try {
                td = new LinkedHashMap();
                for (int i = 1, j = md.getColumnCount(); i <= j; i ++) {
                    td.put(md.getColumnLabel(i), Class.forName(md.getColumnClassName(i)));
                }
            } catch (  SQLException ex ) {
                throw new HongsException(0x10a2, ex);
            } catch (ClassNotFoundException ex) {
                throw new HongsException(0x10a2, ex);
            }
        }
        return td;
    }

    @Override
    public Iterator<Map> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        // SQLite 等的 driver 并不支持 isLast
        // 故不得不在此靠执行 next 来获取状态
        if (il == null) {
            try {
                il = rs.next();
            } catch (SQLException ex) {
                this.close(  );
                throw new HongsExemption(0x10a4, ex);
            }
        }
        return  il;
    }

    @Override
    public Map next() {
        // 判断是否到达末尾
        if ( ! hasNext()) {
            this.close();
            return null ;
        }
        il  = null;

        // 获取当前字段类型
        try {
//          getMetaData();
            getTypeDict();
        } catch (HongsException ex ) {
            this.close( );
            throw  ex.toExemption( );
        }

        // 获取行内每列数据
        try {
            int i = 0;
            Map<String,Object> row = new LinkedHashMap();
            if (ib) {
                for (Map.Entry<String,Class> et : td.entrySet()) {
                    // row.put(et.getKey() , rs.getObject(++ i, et.getState()));
                    Dict.put(row, rs.getObject(++ i, et.getValue()), (Object[]) et.getKey().split("\\."));
                }
            } else {
                for (Map.Entry<String,Class> et : td.entrySet()) {
                    // row.put(et.getKey() , rs.getString(++ i /* No Type */ ));
                    Dict.put(row, rs.getString(++ i /* No Type */ ), (Object[]) et.getKey().split("\\."));
                }
            }
            return  row ;
        } catch (  SQLException ex ) {
            this.close();
            throw new HongsExemption(0x10a6, ex);
        }
    }

    @Override
    public void close() {
        try {
            try {
                if (rs != null && !rs.isClosed()) {
                    rs.close();
                }
            }
            catch (SQLException ex ) {
                throw new HongsExemption(0x1035, ex);
            }
        }
        finally {
            try {
                if (ps != null && !ps.isClosed()) {
                    ps.close();
                }
            }
            catch (SQLException ex ) {
                throw new HongsExemption(0x1034, ex);
            }
        }
    }

    /**
     * @deprecated
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not supported remove in this loop.");
    }

}
