package app.hongs.db.sync;

import app.hongs.HongsException;
import app.hongs.db.DB;
import app.hongs.db.Table;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 库结构同步器(DB structure synchronizer)
 *
 * <b>注意: 参考MySQL编写, 可能不适用于其他数据库</b>
 *
 * @author Hongs
 */
public class DBSync
{

  private DB db;

  /**
   * 通过库对象构造
   * @param db
   * @throws app.hongs.HongsException
   */
  public DBSync(DB db)
  throws HongsException
  {
    this.db = db;
  }

  /**
   * 同步从数据库
   * @param slaver
   * @param tablePrefix 从库表前缀
   * @param tableSuffix 从库表后缀
   * @param delExtraTables 删除多余的表
   * @param delExtraFields 删除多余的字段
   * @throws app.hongs.HongsException
   */
  public void syncSlaver(DB slaver, String tablePrefix, String tableSuffix, boolean delExtraTables, boolean delExtraFields)
  throws HongsException
  {
    List<String> sqls = this.syncSlaverSqls(slaver, tablePrefix, tableSuffix, delExtraTables, delExtraFields);
    DB sdb = slaver;
    sdb.IN_TRNSCT_MODE = true;
    try
    {
      for (String sql : sqls)
      {
        sdb.execute(sql);
      }
      sdb.commit();
    }
    catch (HongsException ex)
    {
      sdb.rolbak();
      throw ex;
    }
  }

  public List<String> syncSlaverSqls(DB slaver, String tablePrefix, String tableSuffix, boolean delExtraTables, boolean delExtraFields)
  throws HongsException
  {
    List<String> sqls = new ArrayList();
    List<String> sqlz;
    String       sql;

    Set tables = new HashSet();

    if (delExtraTables)
    {
      List rows = slaver.fetchAll("SHOW TABLES");
      Iterator it = rows.iterator();
      while (it.hasNext())
      {
        Map.Entry et = (Map.Entry)((Map)it.next()).entrySet().iterator().next();
        String table = (String)et.getValue();
        tables.add(table);
      }
    }

    Set <String> tns = this.db.getTableNames();
    for (String  tab : tns)
    {
      Table table = this.db.getTable(tab);

      Map config = new HashMap();
      config.put("name", table.name);
      if (tablePrefix != null) config.put("tablePrefix", tablePrefix);
      if (tableSuffix != null) config.put("tableSuffix", tableSuffix);
      Table tablz = new Table(slaver, config);

      TableSync sync = new TableSync( table );
      sqlz = sync.syncSlaverSqls(tablz, delExtraFields);
      sqls.addAll( sqlz );

      if (delExtraTables)
      {
        tables.remove(tablz.tableName);
      }
    }

    if (delExtraTables)
    {
      Iterator it2 = tables.iterator();
      while (it2.hasNext())
      {
        String table = (String)it2.next();
        sql = "DROP TABLE '"+table+"'";
        sqls.add(sql);
      }
    }

    return sqls;
  }

}
