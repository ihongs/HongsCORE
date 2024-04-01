package io.github.ihongs.db.link;

import io.github.ihongs.CruxException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 分块查询支持
 *
 * 兼容不同数据库的分页语法, 区分并追加 LIMIT OFFSET 等
 *
 * @author Hongs
 */
public class Lump {

    private String sql;
    private int  start;
    private int  limit;
    private Object[] params;

    public Lump(Link db, String sql, int start, int limit, Object... params)
    throws CruxException {
        if (db == null) {
            throw new NullPointerException("DB Link required");
        }

        String pn, pv;
        try {
            Connection  dc;
            dc = db.open();
            DatabaseMetaData   dm;
            dm = dc.getMetaData();
            pn = dm.getDatabaseProductName();
            pv = dm.getDatabaseProductVersion();
        } catch (SQLException ex) {
            throw new CruxException(ex);
        }

        init(pn, pv, sql, start, limit, params);
    }

    public Lump(Connection dc, String sql, int start, int limit, Object... params)
    throws CruxException {
        if (dc == null) {
            throw new NullPointerException("DB Connection required");
        }

        String pn, pv;
        try {
            DatabaseMetaData dm;
            dm = dc.getMetaData();
            pn = dm.getDatabaseProductName();
            pv = dm.getDatabaseProductVersion();
        } catch (SQLException ex) {
            throw new CruxException(ex);
        }

        init(pn, pv, sql, start, limit, params);
    }

    public Lump(DatabaseMetaData dm, String sql, int start, int limit, Object... params)
    throws CruxException {
        if (dm == null) {
            throw new NullPointerException("DatabaseMetaData required");
        }

        String pn, pv;
        try {
            pn = dm.getDatabaseProductName();
            pv = dm.getDatabaseProductVersion();
        }
        catch (SQLException ex) {
            throw new CruxException(ex);
        }

        init(pn, pv, sql, start, limit, params);
    }

    private void init(String pn, String pv, String sql, int start, int limit, Object... params) {
        if (sql == null) {
            throw new NullPointerException("SQL required");
        }
        if (params == null) {
            params =  new  Object [0];
        }

        if (limit != 0 || start != 0) {
            switch (pn.toUpperCase()) {
                case "MYSQL"     :
                case "MARIADB"   : {
                    sql += " LIMIT ?,?";
                    Object[] paramz = new Object[params.length + 2];
                    System.arraycopy (params, 0, paramz, 0, params.length);
                    paramz[params.length + 0] = start;
                    paramz[params.length + 1] = limit;
                    params = paramz;
                    start  = 0;
                    limit  = 0;
                } break;
                case "SQLITE"    :
                case "OCEANBASE" :
                case "POSTGRESQL": {
                    sql += " LIMIT ? OFFSET ?";
                    Object[] paramz = new Object[params.length + 2];
                    System.arraycopy (params, 0, paramz, 0, params.length);
                    paramz[params.length + 0] = limit;
                    paramz[params.length + 1] = start;
                    params = paramz;
                    start  = 0;
                    limit  = 0;
                } break;
                case "ORACLE"    :
                case "SQLSERVER" : {
                    sql += " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY" ;
                    Object[] paramz = new Object[params.length + 2];
                    System.arraycopy (params, 0, paramz, 0, params.length);
                    paramz[params.length + 0] = start;
                    paramz[params.length + 1] = limit;
                    params = paramz;
                    start  = 0;
                    limit  = 0;
                } break;
            }
        }

        this.sql    = sql   ;
        this.start  = start ;
        this.limit  = limit ;
        this.params = params;
    }

    public String getSQL() {
        return sql;
    }

    public Object[] getParams() {
        return params;
    }

    public int getStart() {
        return start ;
    }

    public int getLimit() {
        return limit ;
    }

    @Override
    public String toString() {
        try {
            StringBuilder sb = new StringBuilder (  sql  );
            List ps = new ArrayList(Arrays.asList(params));
            Link.checkSQLParams( sb, ps );
            Link.mergeSQLParams( sb, ps );
            if (limit != 0 || start != 0) {
              sb.append(" /* LIMIT ")
                .append(start)
                .append( "," )
                .append(limit)
                .append(" */");
            }
            return  sb.toString( );
        } catch ( CruxException e) {
            throw e.toExemption( );
        }
    }

}
