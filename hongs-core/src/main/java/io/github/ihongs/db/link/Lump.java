package io.github.ihongs.db.link;

import io.github.ihongs.CruxException;
import io.github.ihongs.CruxExemption;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * SQL 分块工具
 * @author Hongs
 */
public class Lump {

    private String sql;
    private int  start;
    private int  limit;
    private Object[] params;

    public Lump(String pn, String sql, int start, int limit, Object... params) {
        if (pn == null) {
            throw new NullPointerException("Database Product Name required");
        }

        if (params == null) {
            params =  new  Object [0];
        }

        if (limit != 0 || start != 0) {
            switch (pn.toUpperCase()) {
                case "SQLITE"   :
                case "MYSQL"    :
                case "MARIADB"  :
                case "OCEANBASE": {
                    sql += " LIMIT ?,?";
                    Object[] paramz = new Object[params.length + 2];
                    System.arraycopy (params, 0, paramz, 0, params.length);
                    paramz[params.length + 0] = start;
                    paramz[params.length + 1] = limit;
                    params = paramz;
                    start  = 0;
                    limit  = 0;
                } break;
                case "POSTGRESQL" : {
                    sql += " LIMIT ? OFFSET ?";
                    Object[] paramz = new Object[params.length + 2];
                    System.arraycopy (params, 0, paramz, 0, params.length);
                    paramz[params.length + 0] = limit;
                    paramz[params.length + 1] = start;
                    params = paramz;
                    start  = 0;
                    limit  = 0;
                } break;
            }
        }

        this.sql    = sql ;
        this.start  = start ;
        this.limit  = limit ;
        this.params = params;
    }

    public Lump(Link db, String sql, int start, int limit, Object... params) {
        this(getPname(db), sql, start, limit, params );
    }

    public Lump(Connection dc, String sql, int start, int limit, Object... params) {
        this(getPname(dc), sql, start, limit, params );
    }

    public Lump(DatabaseMetaData dm, String sql, int start, int limit, Object... params) {
        this(getPname(dm), sql, start, limit, params );
    }

    public String getSql() {
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

    private static String getPname(Link db) {
        try {
            return getPname(db.open().getMetaData());
        } catch ( SQLException ex) {
            throw new CruxExemption(ex);
        } catch (CruxException ex) {
            throw ex.toExemption();
        }
    }

    private static String getPname(Connection dc) {
        try {
            return getPname(dc.getMetaData( ));
        } catch ( SQLException ex) {
            throw new CruxExemption(ex);
        }
    }

    private static String getPname(DatabaseMetaData dm) {
        try {
            return dm.getDatabaseProductName();
        } catch ( SQLException ex) {
            throw new CruxExemption(ex);
        }
    }

}
