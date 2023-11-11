package io.github.ihongs.db;

import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLocale;
import io.github.ihongs.CruxException;
import io.github.ihongs.HongsException;
import io.github.ihongs.db.util.FetchCase;
import io.github.ihongs.db.util.AssocMore;
import io.github.ihongs.util.Synt;
import java.sql.Types;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 表基础类
 *
 * <p>
 * 请总是用DB.getTable("Table_Name")来获取表对象
 * </p>
 *
 * <h3>错误代码:</h3>
 * <pre>
 * 区间: 1070~1084
 *
 * 1071 缺少数据库对象
 * 1072 配置不能为空
 * 1073 缺少表名
 *
 * 1074 不能为空
 * 1075 字符串超长
 * 1076 小数位超出
 * 1077 不是数值类型
 * 1078 不可以为负值
 * 1079 无法识别的日期或时间格式
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
      throw new CruxException(1071, "Param db can not be null");
    }

    if (conf == null)
    {
      throw new CruxException(1072, "Param conf can not be null");
    }

    if (!conf.containsKey("name"))
    {
      throw new CruxException(1073, "Table name in conf required.");
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
   * @throws io.github.ihongs.HongsException
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
    return this.fields;
  }

  /**
   * 获取特殊字段名
   * @param field
   * @return
   * @throws io.github.ihongs.HongsException
   */
  public String getField(String field)
    throws HongsException
  {
    String param = (String) params.get("field."+ field);
    if (null != param) {
        field = param;
    } else {
        param = "core.table."+ field +".field";
        field = Core.getInstance(CoreConfig.class)
                    .getProperty(param, field);
    }
    if (null != field && getFields().containsKey(field)) {
        return  field;
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
    if (null != param) {
        state = param;
    } else {
        param = "core.table."+ state +".state";
        state = Core.getInstance(CoreConfig.class)
                    .getProperty(param, null );
    }
    if (null != state && state.length() != 0 ) {
        return  state;
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
    Map item = (Map) fields.get(name);
    int type = (Integer) item.get("type");
    int size = (Integer) item.get("size");
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
    }
    /**
     * 部分数据库空表时取不到类型
     * 但如果给了宽度则可估算容量
     * 0 为未知, 十位够到 2200 年
     * 但很多人习惯设 INTEGER(11)
     */
    if (size >= 01 && size <= 11) {
        return time / 1000;
    } else {
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
   * 用于Model中, Table中不自动写入关联数据
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
        if ( value == null
        && (Boolean) column.get("required"))
        {
          throw nullException(namc);
        }
      }
      else
      {
        if ( isNew
        && (Boolean) column.get("required"))
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

      /**
       * Double型如果直接用toString
       * 则可能为科学计数法
       * 会使校验的正则失效
       */
      String valueStr;
      if (value instanceof Number ) {
        valueStr = Synt .asString((Number) value);
      } else {
        valueStr = value.toString().trim();
      }

      int type = (Integer)column.get("type" );
      int size = (Integer)column.get("size" );
      int scle = (Integer)column.get("scale");

      switch (type) {
      // 判断字串类型
      case Types.CHAR : case Types.VARCHAR : case Types.LONGVARCHAR :
      case Types.NCHAR: case Types.NVARCHAR: case Types.LONGNVARCHAR:
      if (0 < size) {// 不限长为 0, 数值类型亦同此
        // 多字节字符长度统统记为 2, 依此计算总长度
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
        break;
      } else {
        mainValues.put(namc, valueStr);
        break;
      }

      // 判断数字类型
      case Types.INTEGER: case Types.TINYINT: case Types.BIGINT: case Types.SMALLINT:
      case Types.NUMERIC: case Types.DECIMAL: case Types.DOUBLE: case Types.FLOAT   :
      {
        double valueNum ;
        try
        {
          valueNum = Synt.asDouble(value);
        }
        catch (ClassCastException ex)
        {
          throw numeException (namc, valueStr);
        }
        if (0 > valueNum && (Boolean) column.get("unsigned"))
        {
          throw unsiException (namc, valueStr);
        }

        // 计算位数, 填充小数位
        StringBuilder bf = new StringBuilder();
          bf.append("#.#");
        for (int i = 0; i < scle; i ++)
          bf.append( '#' );

        // 取绝对值, 以便算长度
        DecimalFormat df = new DecimalFormat(bf.toString());
        String valueStr2 = df.format( Math.abs( valueNum ));

        int dotPos = valueStr2.indexOf ( '.' );
        if (dotPos < 0)
        {
          /**
           * 判断精度, 0 表示不限
           */
          if (size > 0 && size < valueStr2.length())
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
           * 判断精度, 0 表示不限
           */
          if (size > 0 && size < allLen)
          {
            throw sizeException(namc, valueStr, size);
          }

          /**
           * 判断小数,-1 表示不限
           */
          if (scle >-1 && scle < subLen)
          {
            throw scleException(namc, valueStr, scle);
          }
        }

        mainValues.put(namc, valueNum);
        break;
      }

      // 判断日期类型
      case Types.DATE:
      {
        if (value instanceof Date || value instanceof java.util.Date)
        {
          mainValues.put(namc, value);
        }
        else if (valueStr.matches("^\\d+$"))
        {
          long valueNum = Long.parseLong(valueStr);
          mainValues.put(namc, new Date(valueNum));
        }
        else
        {
          if (dateFormat == null)
          {
            dateFormat = getDtfmt(namc, "date", values);
          }
          DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateFormat, Core.getLocale());

          try
          {
            mainValues.put(namc, Date.valueOf(LocalDate.parse(valueStr, dtf)));
          }
          catch (DateTimeParseException ex)
          {
            throw dateException(namc, valueStr, dateFormat);
          }
        }
        break;
      }

      // 判断时间类型
      case Types.TIME:
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
          DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateFormat, Core.getLocale());

          try
          {
            mainValues.put(namc, Time.valueOf(LocalTime.parse(valueStr, dtf)));
          }
          catch (DateTimeParseException ex)
          {
            throw dateException(namc, valueStr, timeFormat);
          }
        }
        break;
      }

      // 判断时间戳或日期时间类型
      case Types.TIMESTAMP:
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
          DateTimeFormatter dtf = DateTimeFormatter.ofPattern(dateFormat, Core.getLocale());

          try
          {
            mainValues.put(namc, Timestamp.valueOf(LocalDateTime.parse(valueStr, dtf)));
          }
          catch (DateTimeParseException ex)
          {
            throw dateException(namc, valueStr, datetimeFormat);
          }
        }
        break;
      }

      // 其他类型则直接放入(推荐建库时采用上面有校验的类型)
      default:
        mainValues.put(namc, value);
      } // End switch.
    }

    return mainValues;
  }

  //** 私有方法 **/

  private HongsException nullException(String field) {
    String error = "Value for field `$2` in table `$0.$1` can not be NULL";
    return new CruxException(1074, error, db.name,name, field);
  }

  private HongsException sizeException(String field, String value, int size) {
    String error = "Value for field `$2` in table `$0.$1` must be a less than $4, value: $3";
    return new CruxException (1075, error, db.name,name, field, value, size);
  }

  private HongsException scleException(String field, String value, int scle) {
    String error = "Scale for field `$2` in table `$0.$1` must be a less than $4, value: $3";
    return new CruxException (1076, error, db.name,name, field, value, scle);
  }

  private HongsException numeException(String field, String value) {
    String error = "Value for field `$2` in table `$0.$1` must be a standard number, value: $3";
    return new CruxException (1077, error, db.name,name, field, value);
  }

  private HongsException unsiException(String field, String value) {
    String error = "Value for field `$2` in table `$0.$1` must be a unsigned number, value: $3";
    return new CruxException (1078, error, db.name,name, field, value);
  }

  private HongsException dateException(String field, String value, String format) {
    String error = "Value for field `$2` in table `$0.$1` must like '$4', value: $3";
    return new CruxException (1079, error, db.name,name, field, value, format);
  }

  /**
   * 与 DB.getTable(String) 方法不同, 不会获取 table 配置
   * @param db
   * @param name
   * @param tnam
   * @param pkey
   * @return
   * @throws io.github.ihongs.HongsException
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
   * @throws io.github.ihongs.HongsException
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
   * @throws io.github.ihongs.HongsException
   */
  public static Table newInstance(DB db, String name)
    throws HongsException
  {
    return newInstance(db, name, name, null);
  }

}
