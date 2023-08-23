package io.github.ihongs.serv;

import io.github.ihongs.Core;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.HongsException;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Table;
import io.github.ihongs.db.link.Link;
import static io.github.ihongs.db.link.Link.checkSQLParams;
import static io.github.ihongs.db.link.Link.mergeSQLParams;
import io.github.ihongs.db.link.Loop;
import io.github.ihongs.db.util.AssocMore;
import io.github.ihongs.db.util.FetchCase;
import io.github.ihongs.util.Synt;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 加密表
 * @author Hongs
 */
public abstract class STable extends Table {

    public STable(DB db, Map conf) throws HongsException {
        super(db, conf);
    }

    /**
     * 加密字段
     * @return
     */
    public abstract String[] getCryptoFields();

    /**
     * 加密方法
     * @return
     */
    public abstract Function<String, String> encrypt();

    /**
     * 解密方法
     * @return
     */
    public abstract Function<String, String> decrypt();

    @Override
    public int insert(Map<String, Object> values)
    throws HongsException {
        // 加密
        for (String fn : getCryptoFields()) {
            if (values.containsKey(fn)) {
                String fv = Synt.asString(values.get(fn));
                       fv = encrypt().apply(fv);
                values.put(fn, fv);
            }
        }

        return super.insert(values);
    }

    @Override
    public int update(Map<String, Object> values, String where, Object... params)
    throws HongsException {
        // 加密
        for (String fn : getCryptoFields()) {
            if (values.containsKey(fn)) {
                String fv = Synt.asString(values.get(fn));
                       fv = encrypt().apply(fv);
                values.put(fn, fv);
            }
        }

        return super.update(values, where, params);
    }

    @Override
    public FetchCase fetchCase()
    throws HongsException {
        FetchCase  fc = new SCase(this)
              .use(db).from(tableName, name);
        AssocMore.checkCase(fc, getParams());
        return     fc ;
    }

    private static class SCase extends FetchCase {

        private final STable table;

        public SCase (STable table) {
            super ( (byte) 0 );
            this.table = table;
        }

        @Override
        public int insert(Map<String, Object> values) throws HongsException {
            // 加密
            for (String fn : table.getCryptoFields()) {
                if (values.containsKey(fn)) {
                    String fv = Synt.asString(values.get(fn));
                           fv = table.encrypt().apply(fv);
                    values.put(fn, fv);
                }
            }

            return super.insert(values);
        }

        @Override
        public int update(Map<String, Object> values) throws HongsException {
            // 加密
            for (String fn : table.getCryptoFields()) {
                if (values.containsKey(fn)) {
                    String fv = Synt.asString(values.get(fn));
                           fv = table.encrypt().apply(fv);
                    values.put(fn, fv);
                }
            }

            return super.update(values);
        }

        @Override
        public Loop select() throws HongsException {
            Loop rs = query(getSQL(), getStart(), getLimit(), getParams());
            if (hasOption("STRING_MODE")) {
                 rs.inStringMode(getOption("STRING_MODE", false));
            }
            return  rs;
        }

        private Loop query(String sql, int start, int limit, Object... params)
        throws HongsException {
          Link db = linker();

          /**
           * 由于 SQLite 等不支持 absolute 方法
           * 故对这样的库采用组织语句的分页查询
           */
          if (limit == 0) {
              db.open();
          }   else   try  {
              String dpn =
              db.open()
              .getMetaData()
              .getDatabaseProductName()
              .toUpperCase();

              if ("SQLITE".equals(dpn)) {
                  sql += " LIMIT ?,?";
                  Object[] paramz = new Object[params.length + 2];
                  System.arraycopy(params, 0, paramz, 0, params.length);
                  paramz[params.length + 0] = start;
                  paramz[params.length + 1] = limit;
                  params = paramz;
                  start  = 0;
                  limit  = 0;
              }
          } catch (SQLException ex) {
              throw new HongsException(ex);
          }

          if (4 == (4 & Core.DEBUG))
          {
            StringBuilder sb = new StringBuilder(sql);
            List      paramz = new ArrayList(Arrays.asList(params));
            checkSQLParams(sb, paramz);
            mergeSQLParams(sb, paramz);
            CoreLogger.debug("DB.query: "+ sb.toString());
          }

          PreparedStatement ps = db.prepareStatement(sql, params);
                  ResultSet rs;

          try
          {
            if (limit > 0)
            {
              ps.setFetchSize   (   limit);
              ps.setMaxRows(start + limit);
            }
            rs = ps.executeQuery();
            if (start > 0)
            {
              rs. absolute (start);
            }
          }
          catch (SQLException ex )
          {
            throw new HongsException(ex, 1043);
          }

          return  new SLoop(table, rs, ps);
        }

    }

    private static class SLoop extends Loop {

        private final String[] fs;
        private final Function<String, String> dc;

        public SLoop (STable table, ResultSet rs, Statement ps)
        throws HongsException {
            super(rs, ps);
            this.dc = table.decrypt();
            this.fs = table.getCryptoFields();
        }

        @Override
        public Map next() {
            Map row = super.next();

            if (row != null) {
                // 解密
                for (String fn : fs) {
                    if (row.containsKey(fn)) {
                        String fv = Synt.asString(row.get(fn));
                        fv = dc.apply(fv);
                        row.put (fn , fv);
                    }
                }
            }

            return row;
        }

    }



}
