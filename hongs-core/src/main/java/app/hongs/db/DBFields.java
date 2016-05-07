package app.hongs.db;

import app.hongs.Cnst;
import app.hongs.CoreSerial;
import app.hongs.HongsException;
import app.hongs.db.link.Loop;

import java.util.Map;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.io.Serializable;

/**
 * 表字段信息缓存类
 *
 * <p>
 * 缓存文件存放在 "var/serial/库名.表名.df.ser" 的文件中;
 * 当表结构发生改变, 程序不会自动重载, 务必删除对应的缓存文件.
 * </p>
 *
 * @author hongs
 */
public class DBFields
     extends CoreSerial
  implements Serializable
{

  private final Table table;

  public Map<String, Map> fields;

  public DBFields(Table table)
    throws HongsException
  {
    this.table = table;
    if ( table.db.name != null && table.db.name.length() != 0 )
    {
      this.init( table.db.name +"."+ table.tableName + Cnst.DF_EXT );
    }
    else
    {
      this.imports();
    }
  }

  @Override
  protected final void imports()
    throws HongsException
  {
    this.fields = new LinkedHashMap();

    Loop rs = this.table.db.query("SELECT * FROM `"
            + this.table.tableName + "`", 0,1);
    try
    {
      ResultSetMetaData md = rs.getMetaData( );

      for (int i = 1; i <= md.getColumnCount(); i ++)
      {
        Map field = new HashMap();
        field.put("type",           md.getColumnType(i));
        field.put("size",           md.getPrecision (i));
        field.put("scale",          md.getScale(i));
        field.put("unsigned",      !md.isSigned(i));
        field.put("required",       md.isNullable(i)
                == ResultSetMetaData.columnNoNulls );
        field.put("autoIncrement",  md.isAutoIncrement(i));
        field.put("caseSensitive",  md.isCaseSensitive(i));

        // 在这里没什么意义的属性:
        /*
        field.put("currency",       md.isCurrency(i));
        field.put("readOnly",       md.isReadOnly(i));
        field.put("writable",       md.isWritable(i));
        field.put("searchable",     md.isSearchable(i));
        field.put("tableName",      md.getTableName(i));
        field.put("schemaName",     md.getSchemaName(i));
        field.put("catalogName",    md.getCatalogName(i));
        field.put("label",          md.getColumnLable(i));
        field.put("typeName",       md.getColumnTypeName(i));
        field.put("className",      md.getColumnClassName(i));
        field.put("displaySize",    md.getColumnDisplaySize(i));
        */

        this.fields.put(md.getColumnName(i), field);
      }
    }
    catch (SQLException ex)
    {
      throw new HongsException(0x107a, ex);
    }
    finally
    {
      rs.over();
    }
  }

}
