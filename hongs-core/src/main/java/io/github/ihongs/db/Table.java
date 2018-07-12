package io.github.ihongs.db;

import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLocale;
import io.github.ihongs.HongsException;
import io.github.ihongs.db.util.FetchCase;
import io.github.ihongs.db.util.AssocMore;
import io.github.ihongs.util.Synt;
import java.sql.Types;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Iterator;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 数据表基础类
 *
 * <p>
 * 请总是用DB.getTable("Table_Name")来获取表对象
 * </p>
 *
 * <h3>错误代码:</h3>
 * <pre>
 * 区间: 0x1080~0x108f
 *
 * 0x1081 缺少数据库对象
 * 0x1082 配置不能为空
 * 0x1083 缺少表名
 *
 * 0x1089 不能为空
 * 0x108a 精度超出
 * 0x108b 小数位超出
 * 0x108c 不是浮点数值
 * 0x108d 不是整型数值
 * 0x108e 不能为负值
 * 0x108f 无法识别的日期或时间格式
 * </pre>
 *
 * <h3>配置选项:</h3>
 * <pre>
 * core.default.date.format   可识别的日期类型, 默认"yyyy/MM/dd", 已移到语言
 * core.default.time.format   可识别的时间类型, 默认  "HH:mm:ss", 已移到语言
 * core.table.check.value     设置为true禁止在存储时对数据进行检查
 * core.table.ctime.field     创建时间字段名
 * core.table.mtime.field     修改时间字段名
 * core.table.etime.field     结束时间字段名
 * core.table.state.field     状态字段名
 * core.table.default.state   默认状态
 * core.table.removed.state   删除状态
 * </pre>
 *
 * @author Hongs
 */
public class Table
{
  /**
   * DB对象
   */
  public DB db;

  /**
   * 表名
   */
  public String name;

  /**
   * 表全名
   */
  public String tableName;

  /**
   * 主键名
   */
  public String primaryKey = "";

  private   Map fields;
  protected Map params;
  protected Map assocs;
  protected Map relats;

  public Table(DB db, Map conf)
    throws HongsException
  {
    if (db == null)
    {
      throw new HongsException(0x1081, "Param db can not be null");
    }

    if (conf == null)
    {
      throw new HongsException(0x1082, "Param conf can not be null");
    }

    if (!conf.containsKey("name"))
    {
      throw new HongsException(0x1083, "Table name in conf required.");
    }

    this.db   =  db ;
    this.name = (String)conf.get("name");

    if (conf.containsKey("tableName"))
    {
      this.tableName = (String)conf.get("tableName");
    }
    else
    {
      this.tableName = name;
    }

    if (db.tablePrefix != null)
    {
      this.tableName = db.tablePrefix + this.tableName;
    }
    if (db.tableSuffix != null)
    {
      this.tableName = this.tableName + db.tableSuffix;
    }

    if (conf.containsKey("primaryKey"))
    {
      this.primaryKey = (String)conf.get("primaryKey");
    }

    if (conf.containsKey("assocs"))
    {
      this.assocs = (Map)conf.get("assocs");
    }
    else
    {
      this.assocs = new HashMap();
    }

    if (conf.containsKey("relats"))
    {
      this.relats = (Map)conf.get("relats");
    }
    else
    {
      this.relats = new HashMap();
    }

    if (conf.containsKey("params"))
    {
      this.params = (Map)conf.get("params");
    }
    else
    {
      this.params = new HashMap();
    }

    if (!assocs.isEmpty() && relats.isEmpty())
    {
      // 有关联无逆向结构, 自动生成; 后续补充.
    }
  }

  /**
   * 查询多条记录(会根据配置自动关联)
   * @param caze
   * @return 全部记录
   * @throws io.github.ihongs.HongsException
   */
  public List fetchMore(FetchCase caze)
    throws HongsException
  {
    caze.from(tableName, name);

    String rstat = getField( "state" );
    String rflag = getState("removed");

    // 默认不查询已经删除的记录
    if (rstat != null && rflag != null
    && !caze.getOption("INCLUDE_REMOVED" , false))
    {
      caze.filter("`"+name+"`.`" + rstat +"` != ?", rflag);
    }

    // 默认列表查询不包含对多的, 可用此开启
    if (caze.getOption("INCLUDE_HASMANY" , false))
    {
      Set s  = (Set) caze.getOption("ASSOC_TYPES");
      if (s != null) {
          s.add("HAS_MANY");
          s.add("HAS_MORE");
      }
    }

    return AssocMore.fetchMore(this, caze, assocs);
  }

  /**
   * 获取单条记录(会根据配置自动关联)
   * @param caze
   * @return 单条记录
   * @throws io.github.ihongs.HongsException
   */
  public Map fetchLess(FetchCase caze)
    throws HongsException
  {
    caze.limit(1);
    List<Map> rows = this.fetchMore(caze);

    if (! rows.isEmpty( ))
    {
      return rows.get( 0 );
    }
    else
    {
      return new HashMap();
    }
  }

  /**
   * 调用 FetchCase 构建查询
   * 可用 getAll, getOne  得到结果, 以及 delete, update 操作数据
   * 但与 fetchMore,fetchLess 不同, 不会自动关联和排除已删的数据
   * @return 绑定了 db, table 的查询对象
   * @throws io.github.ihongs.HongsException
   */
  public FetchCase fetchCase()
    throws HongsException
  {
    FetchCase  fc = new FetchCase()
          .use(db).from(tableName, name);
    AssocMore.checkCase(fc, getParams());
    return     fc ;
  }

  /**
   * 插入数据
   * @param values
   * @return 插入条数
   * @throws io.github.ihongs.HongsException
   */
  public int insert(Map<String, Object> values)
    throws HongsException
  {
    String mtime = getField("mtime");
    String ctime = getField("ctime");
    String rstat = getField("state");

    long time = System.currentTimeMillis();

    // 存在 mtime 字段则自动放入当前时间
    if (mtime != null && !values.containsKey(mtime))
    {
      values.put(mtime, getDtval(mtime, time));
    }

    // 存在 ctime 字段则自动放入当前时间
    if (ctime != null && !values.containsKey(ctime))
    {
      values.put(ctime, getDtval(ctime, time));
    }

    // 存在 state 字段则自动放入默认值
    if (rstat != null && !values.containsKey(rstat))
    {
      String s = getState("default");
      if ( s  != null )
      {
        values.put(rstat, s);
      }
    }

    // 整理数据
    Map mainValues = this.checkMainValues(values, true);

    // 插入数据
    return  this.db.insert(this.tableName , mainValues);
  }

  /**
   * 更新数据
   * @param values
   * @param where
   * @param params
   * @return 更新条数
   * @throws io.github.ihongs.HongsException
   */
  public int update(Map<String, Object> values, String where, Object... params)
    throws HongsException
  {
    String mtime = getField("mtime");

    long time = System.currentTimeMillis();

    // 存在 mtime 字段则自动放入当前时间
    if (mtime != null && !values.containsKey(mtime))
    {
      values.put(mtime, getDtval(mtime, time));
    }

    // 整理数据
    Map mainValues = this.checkMainValues(values, false);

    // 更新数据
    return this.db.update(this.tableName, mainValues, where, params);
  }

  /**
   * 删除数据
   * <pre>
   * 如果状态有多个, 且希望删除后可恢复回以前的状态,
   * 可采用负值记录, 请自行构建和调用类似下面的 SQL:
   * 删除: "UPDATE `"+tableName+"` SET `state` = 0 - `state` WHERE `state` &gt; 0 AND " + where
   * 恢复: "UPDATE `"+tableName+"` SET `state` = 0 - `state` WHERE `state` &lt; 0 AND " + where
   * </pre>
   * @param where
   * @param params
   * @return 删除条数
   * @throws io.github.ihongs.HongsException
   */
  public int remove(String where, Object... params)
    throws HongsException
  {
    String rstat = getField ( "state" );
    String rflag = getState ("removed");

    // 存在 rstat 字段则将删除标识设为 removed 值
    if (rstat != null && rflag != null)
    {
      Map data = new HashMap();
      data.put( rstat, rflag );
      return this.update(data, where, params);
    }
    else
    {
      return this.delete(/***/ where, params);
    }
  }

  /**
   * 物理删除
   * <pre>
   * 不理会是否存在状态字段, 此方法总是执行物理删除
   * </pre>
   * @param where
   * @param params
   * @return
   * @throws HongsException
   */
  public int delete(String where, Object... params)
    throws HongsException
  {
    return this.db.delete(this.tableName, where, params);
  }

  //** 工具方法 **/

  public Map getParams()
  {
    return this.params;
  }

  public Map getAssocs()
  {
    return this.assocs;
  }

  /**
   * 获取字段(包含名称及类型等)
   * @return 全部字段信息
   * @throws io.github.ihongs.HongsException
   */
  public Map getFields()
    throws HongsException
  {
    if (null == this.fields)
    {
      this.fields = (new DBFields(this)).fields;
    }
    return /**/ this.fields;
  }

  /**
   * 获取特殊字段名
   * @param field
   * @return
   * @throws HongsException
   */
  public String getField(String field)
    throws HongsException
  {
    String param = (String) params.get("field."+ field);
    if (null == param) {
        param = Core.getInstance (  CoreConfig.class  )
    .getProperty("core.table."+ field +".field", field);
    }
    if (null != param && getFields().containsKey(param)) {
        return  param;
    } else {
        return  null ;
    }
  }

  /**
   * 获取基础状态值
   * @param state
   * @return
   */
  public String getState(String state)
  {
    String param = (String) params.get("state."+ state);
    if (null == param) {
        param = Core.getInstance (  CoreConfig.class  )
    .getProperty("core.table."+ state +".state", state);
    }
    if (null != param) {
        return  param;
    } else {
        return  null ;
    }
  }

  /**
   * 获取日期(时间)取值
   * @param name
   * @param time
   * @return
   */
  protected Object getDtval(String name, long time)
  {
    int type = (Integer) ((Map) this.fields.get(name) ).get("type");
    switch (type)
    {
      case Types.DATE:
        return new Date(time);
      case Types.TIME:
        return new Time(time);
      case Types.TIMESTAMP:
        return new Timestamp(time);
      case Types.INTEGER  :
        return time / 1000;
      default:
        return time ;
    }
  }

  /**
   * 获取日期(时间)格式
   * <p>
   * 也可在 values 中通过 __type_format__,__name__format__ 来告知格式;
   * 其中的 type 为 date,time,datetime; name 为 values 中的键
   * </p>
   * @param type
   * @param name
   * @param values
   * @return
   */
  protected String getDtfmt(String name, String type, Map values)
  {
    String key;
    key = name+"_format__";
    if (values.containsKey(key))
    {
      if (values.get(key) instanceof String)
      {
        return (String) values.get(key);
      }
    }
    key = type+"_format__";
    if (values.containsKey(key))
    {
      if (values.get(key) instanceof String)
      {
        return (String) values.get(key);
      }
    }

    String fmt;
    if ("time".equals(type)) {
      fmt = "HH:mm:ss";
    }
    else
    if ("date".equals(type)) {
      fmt = "yyyy/MM/dd";
    }
    else {
      fmt = "yyyy/MM/dd HH:mm:ss";
    }

    CoreLocale conf = Core.getInstance(CoreLocale.class);
    return conf.getProperty("core.default."+ type +".format", fmt );
  }

  /**
   * 删除子数据
   *
   * 用于Model中, Table中不自动删除关联数据
   *
   * @param ids
   * @throws io.github.ihongs.HongsException
   */
  public void deleteSubValues(Object... ids)
    throws HongsException
  {
    AssocMore.deleteMore(this, assocs , ids);
  }

  /**
   * 插入子数据
   *
   * 用于Model中, Table中不自动删除关联数据
   *
   * @param values
   * @throws io.github.ihongs.HongsException
   */
  public void insertSubValues(Map values)
    throws HongsException
  {
    AssocMore.insertMore(this, assocs , values);
  }

  /**
   * 检验主数据
   *
   * <pre>
   * 会进行校验的类型:
   * CHAR, VARCHAR, LONGVARCHAR, NCHAR, NVARCHAR, NLONGVARCHAR
   * INTEGER, TINYINT, SMALLINT, BIGINT
   * FLOAT, DOUBLE, NUMERIC, DECIMAL
   * DATE, TIME, TIMESTAMP
   * 推荐构建数据库时采用以上类型
   * 日期时间格式采用默认配置定义
   * 通过配置"core.table.check.value=true"来开启检查
   * 通过语言"core.default.date.format=日期格式串"来设置可识别的日期格式
   * 通过语言"core.defualt.time.format=时间格式串"来设置可识别的时间格式
   * </pre>
   *
   * @param values
   * @param isNew 新增还是修改
   * @return 可供提交的数据
   * @throws io.github.ihongs.HongsException
   */
  private Map checkMainValues(Map values, boolean isNew)
    throws HongsException
  {
    Map mainValues = new HashMap();

    /**
     * 是否开启检查
     */
    boolean checked;
    if (params.containsKey("check.value")) {
        checked = Synt.declare(params.get("check.value"), false);
    } else {
        checked = CoreConfig.getInstance()
                  .getProperty("core.table.check.value" , false);
    }

    /**
     * 日期时间格式
     */
    String dateFormat = null,
           timeFormat = null,
       datetimeFormat = null;

    /**
     * 获取字段信息
     */
    this.getFields();

    Iterator it = this.fields.entrySet().iterator();
    while (it.hasNext())
    {
      Map.Entry et = (Map.Entry)it.next();
      String  namc = (String) et.getKey();
      Map   column = (Map)  et.getValue();

      Object value = values.get(namc);

      /**
       * 如果存在该值, 而该值为空
       * 或不存在该值, 而处于新建
       * 则判断该字段是否可以为空
       * 新建时自增值为空跳过
       */
      if (values.containsKey(namc))
      {
        if ((value == null || value.equals( "" ))
        && (Boolean) column.get(   "required"  ))
        {
          throw nullException(namc);
        }
      }
      else
      {
        if ( isNew
        && (Boolean) column.get(   "required"   )
        &&!(Boolean) column.get("autoIncrement"))
        {
          throw nullException(namc);
        }
        continue;
      }

      /**
       * 如果关闭了检查或值不是基础类型, 则跳过数据检查
       * 通常POST或GET过来的总是String, JSON过来的是String/Number/Boolean/Null
       */
      if (!checked || !(value instanceof String || value instanceof Number))
      {
        mainValues.put(namc, value);
        continue;
      }

      String valueStr = value.toString().trim();

      int type = (Integer)column.get("type" );
      int size = (Integer)column.get("size" );
      int scle = (Integer)column.get("scale");

      // 判断字符串类型
      if (type == Types.CHAR  || type == Types.VARCHAR  || type == Types.LONGVARCHAR
       || type == Types.NCHAR || type == Types.NVARCHAR || type == Types.LONGNVARCHAR)
      {
        // 判断长度, 多字节的字符统统被记为 2
        int l  = 0, i , c;
        for(i  = 0; i < valueStr.length(); i ++)
        {
          c = Character.codePointAt(valueStr, i);
          if (c >= 0 && c <= 255) {
            l += 1;
          } else {
            l += 2;
          }
        }
        if (l > size && 0 < size)
        {
          throw sizeException(namc, valueStr, size);
        }

        mainValues.put(namc, valueStr);
      }

      // 判断整型数值
      else if (type == Types.INTEGER || type == Types.TINYINT || type == Types.SMALLINT || type == Types.BIGINT)
      {
        if (!valueStr.matches("^[\\-+]?[0-9]+(\\.0+)?$"))
        {
          throw intgrException(namc, valueStr);
        }

        if ((Boolean)column.get("unsigned") && valueStr.startsWith("-"))
        {
          throw usngdException(namc, valueStr);
        }

        /**
         * 取数字的绝对值(去负号), 便于检查长度
         */
        DecimalFormat df = new DecimalFormat ("#");
        double valueNum  = Double.parseDouble(valueStr);
        String valueStr2 = df.format(Math.abs(valueNum));

        // 判断精度
        if (valueStr2.length() > size)
        {
          throw sizeException(namc, valueStr, size);
        }

        mainValues.put(namc, valueNum);
      }

      // 判断非整型数值
      else if (type == Types.NUMERIC || type == Types.DECIMAL || type == Types.DOUBLE || type == Types.FLOAT)
      {
        if (!valueStr.matches("^[\\-+]?[0-9]+(\\.[0-9]+)?$"))
        {
          throw floatException(namc, valueStr);
        }

        if ((Boolean)column.get("unsigned") && valueStr.startsWith("-"))
        {
          throw usngdException(namc, valueStr);
        }

        // 判断小数位数, 填充小数位
        StringBuilder sb = new StringBuilder();
        sb.append("#.#");
        for (int i = 0; i < scle; i ++)
          sb.append('#');
        String sbs = sb.toString();

        /**
         * 取数字的绝对值(去负号), 便于检查长度
         */
        DecimalFormat df = new DecimalFormat (sbs);
        double valueNum  = Double.parseDouble(valueStr);
        String valueStr2 = df.format(Math.abs(valueNum));

        int dotPos = valueStr2.indexOf('.');
        if (dotPos == -1)
        {
          /**
           * 判断精度
           */
          if (valueStr2.length() > size)
          {
            throw sizeException(namc, valueStr, size);
          }
        }
        else
        {
          int allLen;
          int subLen;
          if (valueStr.startsWith("0"))
          {
            allLen = valueStr.length() - 2;
            subLen = allLen;
          }
          else
          {
            allLen = valueStr.length() - 1;
            subLen = allLen - dotPos;
          }

          /**
           * 判断精度
           */
          if (allLen > size)
          {
            throw sizeException(namc, valueStr, size);
          }

          /**
           * 判断小数
           */
          if (subLen > scle)
          {
            throw scleException(namc, valueStr, scle);
          }
        }

        mainValues.put(namc, valueNum);
      }

      // 判断日期类型
      else if (type == Types.DATE)
      {
        if (value instanceof Date || value instanceof java.util.Date)
        {
          mainValues.put(namc, value);
        }
        else if (valueStr.matches("^\\d+$"))
        {
          long valueNum = Long.parseLong(valueStr) ;
          mainValues.put( namc, new Date(valueNum));
        }
        else
        {
          if (dateFormat == null)
          {
            dateFormat = getDtfmt(namc, "date", values);
          }
          SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
          sdf.setTimeZone(Core.getTimezone());

          try
          {
            mainValues.put(namc, sdf.parse(valueStr));
          }
          catch (ParseException ex)
          {
            throw datetimeException(namc , valueStr , dateFormat);
          }
        }
      }

      // 判断时间类型
      else if (type == Types.TIME)
      {
        if (value instanceof Time || value instanceof java.util.Date)
        {
          mainValues.put(namc, value);
        }
        else if (valueStr.matches("^\\d+$"))
        {
          long valueNum = Long.parseLong(valueStr);
          mainValues.put(namc, new Time(valueNum));
        }
        else
        {
          if (timeFormat == null)
          {
            timeFormat = getDtfmt(namc, "time", values);
          }
          SimpleDateFormat sdf = new SimpleDateFormat(timeFormat);
          sdf.setTimeZone(Core.getTimezone());

          try
          {
            mainValues.put(namc, sdf.parse(valueStr));
          }
          catch (ParseException ex)
          {
            throw datetimeException(namc , valueStr , timeFormat);
          }
        }
      }

      // 判断时间戳或日期时间类型
      else if (type == Types.TIMESTAMP)
      {
        if (value instanceof Timestamp || value instanceof java.util.Date)
        {
          mainValues.put(namc, value);
        }
        else if (valueStr.matches("^\\d+$"))
        {
          long valueNum = Long.parseLong(valueStr);
          mainValues.put(namc, new Timestamp(valueNum));
        }
        else
        {
          if (datetimeFormat == null)
          {
            datetimeFormat = getDtfmt(namc, "datetime", values);
          }
          SimpleDateFormat sdf = new SimpleDateFormat(datetimeFormat);
          sdf.setTimeZone(Core.getTimezone());

          try
          {
            mainValues.put(namc, sdf.parse(valueStr));
          }
          catch (ParseException ex)
          {
            throw datetimeException(namc , valueStr , datetimeFormat);
          }
        }
      }

      // 其他类型则直接放入(推荐建库时采用上面有校验的类型)
      else
      {
        mainValues.put(namc, value);
      }
    }

    return mainValues;
  }

  //** 私有方法 **/

  private HongsException nullException(String name) {
    String error = "Value for column '"+name+"' can not be NULL";
    return validateException(0x1089, error, name);
  }

  private HongsException sizeException(String name, String value, int size) {
    String error = "Value for column '"+name+"'("+value+") must be a less than "+size;
    return validateException(0x108a, error, name, value, String.valueOf(size));
  }

  private HongsException scleException(String name, String value, int scle) {
    String error = "Scale for column '"+name+"'("+value+") must be a less than "+scle;
    return validateException(0x108b, error, name, value, String.valueOf(scle));
  }

  private HongsException floatException(String name, String value) {
    String error = "Value for column '"+name+"'("+value+") is not a float number";
    return validateException(0x108c, error, name, value);
  }

  private HongsException intgrException(String name, String value) {
    String error = "Value for column '"+name+"'("+value+") is not a integer number";
    return validateException(0x108d, error, name, value);
  }

  private HongsException usngdException(String name, String value) {
    String error = "Value for column '"+name+"'("+value+") must be a unsigned number";
    return validateException(0x108e, error, name, value);
  }

  private HongsException datetimeException(String name, String value, String format) {
    String error = "Format for column '"+name+"'("+value+") must like this '"+format+"'";
    return validateException(0x108f, error, name, value, format);
  }

  private HongsException validateException(int code, String error, String fieldName, String... otherParams)
  {
    List<String> trans = new ArrayList(/**/);
    trans.add(db.name+"."+name);
    trans.add(/*the*/fieldName);
    trans.addAll(Arrays.asList(otherParams));

    return new HongsException(code, error+" (Table:"+name+")")
         .setLocalizedOptions(trans.toArray (new String[]{}) );
  }

  /**
   * 与 DB.getTable(String) 方法不同, 不会获取 table 配置
   * @param db
   * @param name
   * @param tnam
   * @param pkey
   * @return
   * @throws HongsException
   */
  public static Table newInstance(DB db, String name, String tnam, String pkey)
    throws HongsException
  {
    Map map = new HashMap();
    map.put("name" , name );
    map.put("tableName" , tnam);
    map.put("primaryKey", pkey);
    Table inst = new Table(db, map);
    return inst;
  }

  /**
   * 与 DB.getTable(String) 方法不同, 不会获取 table 配置
   * @param db
   * @param name
   * @param pkey
   * @return
   * @throws HongsException
   */
  public static Table newInstance(DB db, String name, String pkey)
    throws HongsException
  {
    return newInstance(db, name, name, pkey);
  }

  /**
   * 与 DB.getTable(String) 方法不同, 不会获取 table 配置
   * @param db
   * @param name
   * @return
   * @throws HongsException
   */
  public static Table newInstance(DB db, String name)
    throws HongsException
  {
    return newInstance(db, name, name, null);
  }

}
