package app.hongs.db.sync;

import app.hongs.HongsException;
import app.hongs.db.DB;
import app.hongs.db.Table;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 表结构描述(Table structure descriptions)
 *
 * <b>注意: 参考MySQL编写, 可能不适用于其他数据库</b>
 *
 * @author Hongs
 */
public class TableDesc {

    /**
     * 字段信息 {字段名 : 字段描述串}
     */
    public Map<String, String> columns;

    /**
     * 主键信息 [主键代码]
     */
    public Set<String> priCols;

    /**
     * 唯一键信息 {键名 : [键代码]}
     */
    public Map<String, Set<String>> uniKeys;

    /**
     * 索引键信息 {键名 : [键代码]}
     */
    public Map<String, Set<String>> idxKeys;

    public static final int DROP   = 0;
    public static final int ADD    = 1;
    public static final int MODIFY = 2;

    private static final Pattern typePat = Pattern.compile("^(DATE|TIME)", Pattern.CASE_INSENSITIVE);
    private static final Pattern timePat = Pattern.compile(   "[^/-:]"   , Pattern.CASE_INSENSITIVE);

    public TableDesc() {
        this.columns = new LinkedHashMap<String, String>();
        this.priCols = new LinkedHashSet<String>();
        this.uniKeys = new LinkedHashMap<String, Set<String>>();
        this.idxKeys = new LinkedHashMap<String, Set<String>>();
    }
    
    public static TableDesc getInstance(Table table) throws HongsException {
        TableDesc  desc = new TableDesc();

        try {
            List   rows;
            Iterator it;

            /**
             * 组织字段描述
             */
            rows = table.db.fetchAll("SHOW FULL COLUMNS FROM `" + table.tableName + "`");
            it = rows.iterator();
            while (it.hasNext()) {
                Map row = (Map) it.next();
                String dfn = desc.getDefine(row);
                String col = (String) row.get("Field");

                desc.addColumn(col , dfn);
            }

            /**
             * 获取索引字段
             */
            rows = table.db.fetchAll("SHOW INDEXES FROM `" + table.tableName + "`");
            it = rows.iterator();
            while (it.hasNext()) {
                Map row = (Map) it.next();
                String key = (String) row.get(   "Key_name");
                String col = (String) row.get("Column_name");

                if ("PRIMARY".equals(key)) {
                    desc.addPriCol(col);
                } else if ("0".equals(row.get("Non_unique"))) {
                    desc.addUniKey(col, key);
                } else {
                    desc.addIdxKey(col, key);
                }
            }
        } catch (HongsException ex) {
            if (ex.getCode() == 0x1047) {
                String msg = ex.getMessage();
                if (msg.startsWith("Ex1047: Table ") && msg.endsWith(" doesn't exist")) {
                    return desc;
                }
            }
            throw ex;
        }
        
        return desc;
    }

    public String getDefine(Map row) {
        String val, typ = (String) row.get("Type");
        StringBuilder buf = new StringBuilder(typ);

        if ("NO".equals(row.get("Null"))) {
            buf.append(" NOT NULL");
        } else {
            buf.append(" NULL");
        }

        val = (String) row.get("Default");
        if (val != null) {
            if (!typePat.matcher(typ).matches( )
            ||  !timePat.matcher(val).matches()) {
                val = DB.quoteValue(val);
            }
            buf.append(" DEFAULT ").append(val);
        }

        val = (String) row.get("Extra");
        if (val != null) {
            buf.append(" ").append(val);
        }

        val = (String) row.get("Comment");
        if (val != null) {
            val = DB.quoteValue(val);
            buf.append(" COMMENT ").append(val);
        }

        return buf.toString();
    }

    public TableDesc addColumn(String col, String dfn) {
        this.columns.put(col, dfn);
        return this;
    }

    public TableDesc addPriCol(String col) {
        this.priCols.add(col);
        return this;
    }

    public TableDesc addUniKey(String col, String key) {
        Set cols = this.uniKeys.get(key);
        if (cols == null) {
            cols = new LinkedHashSet();
            this.uniKeys.put(key, cols);
        }
        cols.add(col);
        return this;
    }

    public TableDesc addIdxKey(String col, String key) {
        Set cols = this.idxKeys.get(key);
        if (cols == null) {
            cols = new LinkedHashSet();
            this.idxKeys.put(key, cols);
        }
        cols.add(col);
        return this;
    }

    /**
     * 更改字段
     *
     * @param table 要更改的表
     * @param opr 操作(DROP,ADD,MODIFY)
     * @param col 字段名
     * @throws app.hongs.HongsException
     */
    public void alterColumn(Table table, int opr, String col)
            throws HongsException {
        String sql = this.alterColumnSql(table.tableName, opr, col);
        table.db.execute(sql);
    }

    /**
     * 更改键
     *
     * @param table 要更改的表
     * @param opr 操作
     * @throws app.hongs.HongsException
     */
    public void alterPriKey(Table table, int opr)
            throws HongsException {
        String sql = this.alterPriKeySql(table.tableName, opr);
        table.db.execute(sql);
    }

    /**
     * 更改键
     *
     * @param table 要更改的表
     * @param opr 操作
     * @param key 键名
     * @throws app.hongs.HongsException
     */
    public void alterUniKey(Table table, int opr, String key)
            throws HongsException {
        String sql = this.alterUniKeySql(table.tableName, opr, key);
        table.db.execute(sql);
    }

    /**
     * 更改键
     *
     * @param table 要更改的表
     * @param opr 操作
     * @param key 键名
     * @throws app.hongs.HongsException
     */
    public void alterIdxKey(Table table, int opr, String key)
            throws HongsException {
        String sql = this.alterIdxKeySql(table.tableName, opr, key);
        table.db.execute(sql);
    }

    /**
     * 获取更改字段的SQL
     *
     * @param tableName 要更改的表
     * @param opr 更改类型(DROP,ADD,MODIFY)
     * @param col 字段名
     * @return 返回构造好的SQL语句
     * @throws app.hongs.HongsException
     */
    public String alterColumnSql(String tableName, int opr, String col)
            throws HongsException {
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE `").append(tableName).append("`");
        switch (opr) {
            case TableDesc.DROP:
                sql.append( " DROP `" ).append(col).append("`" );
                break;
            case TableDesc.ADD:
                sql.append(  " ADD `" ).append(col).append("` ").append(this.columns.get(col));
                break;
            case TableDesc.MODIFY:
                sql.append(" MODIFY `").append(col).append("` ").append(this.columns.get(col));
                break;
        }
        return sql.toString();
    }

    public String alterPriKeySql(String tableName, int opr) {
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE `").append(tableName).append("`");
        if (opr == DROP) {
            sql.append(" DROP PRIMARY KEY" );
        } else {
            sql.append(" ADD PRIMARY KEY (").append(getCols(this.priCols)).append(")");
        }
        return sql.toString();
    }

    public String alterUniKeySql(String tableName, int opr, String key) {
        key = key.replaceFirst(":.*$", ""); // 去掉临时键标识
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE `").append(tableName).append("`");
        if (opr == DROP) {
            sql.append(" DROP UNIQUE `").append(key).append("`");
        } else {
            Set<String> cols = this.uniKeys.get(key);
            sql.append( " ADD UNIQUE `").append(key).append("` (").append(getCols(cols)).append(")");
        }
        return sql.toString();
    }

    public String alterIdxKeySql(String tableName, int opr, String key) {
        key = key.replaceFirst(":.*$", ""); // 去掉临时键标识
        StringBuilder sql = new StringBuilder();
        sql.append("ALTER TABLE `").append(tableName).append("`");
        if (opr == DROP) {
            sql.append(" DROP INDEX `" ).append(key).append("`");
        } else {
            Set<String> cols = this.idxKeys.get(key);
            sql.append( " ADD INDEX `" ).append(key).append("` (").append(getCols(cols)).append(")");
        }
        return sql.toString();
    }

    private String getCols(Set<String> keys) {
        StringBuilder sb = new StringBuilder();
        Iterator it = keys.iterator();
        while (it.hasNext()) {
            sb.append(",`").append((String) it.next()).append("`");
        }
        return sb.substring(1);
    }

}
