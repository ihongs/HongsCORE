package app.hongs.db;

import app.hongs.HongsError;
import app.hongs.HongsException;
import app.hongs.db.DB.Roll;
import app.hongs.util.Synt;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 查询结构及操作
 *
 * <p>
 * 字段名前, 用"."表示属于当前表, 用":"表示属于上级表.<br/>
 * 关联字段, 用"表.列"描述字段时, "."的两侧不得有空格.<br/>
 * 本想自动识别字段的所属表(可部分区域), 但总是出问题;<br/>
 * 好的规则胜过万行代码, 定此规矩, 多敲了一个符号而已.<br/>
 * setOption用于登记特定查询选项, 以备组织查询结构的过程中读取.
 * </p>
 *
 * <p>
 * [2015/11/24 00:28]
 * 已解决加表名前缀的问题;
 * 上级表请使用上级表别名;
 * 且兼容上面旧的前缀规则.
 * 增加了表示别名的前缀"!", 单纯统计行数须写 COUNT(!*).
 * 以下 select,where,groupBy,which,orderBy,on 均可如此.
 * 本类处理一般的查询尚可, 过于复杂的 SQL 语句不用为好.
 * </p>
 *
 * <h3>将SQL语句拆解成以下对应部分:</h3>
 * <pre>
 * fields         SELECT    field1, field2...
 * tableName name FROM      tableName AS name
 * wheres         WHERE     expr1 AND expr2...
 * groups         GROUP BY  field1, field2...
 * havins         HAVING    expr1 AND expr2...
 * orders         ORDER BY  field1, field2...
 * limits         LIMIT     start, limit
 * </pre>
 *
 * <h3>系统已定义的"options":</h3>
 * <pre>
 * ASSOCS       : Set         仅对某些表做关联; 作用域: FetchMore.fetchMore
 * ASSOC_TYPES  : Set         仅对某些类型关联; 作用域: FetchMore.fetchMore
 * ASSOC_JOINS  : Set         仅对某些类型连接; 作用域: FetchMore.fetchMore
 * ASSOC_MULTI  : boolean     多行关联(使用IN方式关联); 作用域: FetchMore
 * ASSOC_MERGE  : boolean     归并关联(仅限非多行关联); 作用域: FetchMore
 * FETCH_OBJECT : boolean     获取对象; 作用域: DB.fetchMore
 * page         : int|String  分页页码; 作用域: FetchPage
 * pags         : int|String  链接数量; 作用域: FetchPage
 * rows         : int|String  分页行数; 作用域: FetchPage
 * </pre>
 *
 * <h3>异常代码:</h3>
 * <pre>
 * 区间: 0x10b0~0x10bf
 * 0x10b0 无法识别关联类型(JOIN)
 * 0x10b2 必须指定关联条件(FULL|LEFT|RIGHT)_JOIN
 * 0x10b4 没有指定查询表名
 * 0x10b6 没有指定查询的库
 * </pre>
 *
 * @author Hongs
 */
public class FetchCase
  implements Cloneable, Serializable
{

  protected String              tableName;
  protected String              name;

  protected StringBuilder       fields;
  protected StringBuilder       wheres;
  protected StringBuilder       groups;
  protected StringBuilder       havins;
  protected StringBuilder       orders;
  protected int[]               limits;

  protected List<Object>        wparams;
  protected List<Object>        hparams;
  protected Map<String, Object> options;

  protected byte                joinType;
  protected String              joinExpr;
  protected String              joinName;
  protected Set<FetchCase>      joinList;

  public    static final byte    LEFT = 1;
  public    static final byte   RIGHT = 2;
  public    static final byte    FULL = 3;
  public    static final byte   INNER = 4;
  public    static final byte   CROSS = 5;

  private static final Pattern ps = Pattern
          .compile("^\\s*,\\s*" /***/ , Pattern.CASE_INSENSITIVE);
  private static final Pattern pw = Pattern
          .compile("^\\s*(AND|OR)\\s+", Pattern.CASE_INSENSITIVE);

  // 查找列名加关联层级名
  private static final Pattern pa = Pattern
          .compile("(['`\\)\\w]\\s+)?(?:(\\w+)|`(\\w+)`)(\\s*(?:,|$))");

  // 通过约定标识确定字段
  private static final Pattern pc = Pattern
          .compile("(?<![`\\w])[\\.:!](`.+?`|\\w+|\\*)");

  // 查找与字段相关的元素
  private static final Pattern p0 = Pattern
          .compile("('.+?'|`.+?`|\\w+|\\*|\\))\\s*");
  // 后面不跟字段可跟别名, \\d 换成 \\w 则不处理没 "`" 包裹的字段
  private static final Pattern p1 = Pattern
          .compile("AS|END|NULL|TRUE|FALSE|\\)|\\d.*"
                     , Pattern.CASE_INSENSITIVE);
  // 后面可跟字段的关键词
  private static final Pattern p2 = Pattern
          .compile("IS|IN|ON|OR|AND|NOT|TOP|CASE|WHEN|THEN|ELSE|LIKE|ESCAPE|BETWEEN|DISTINCT"
                     , Pattern.CASE_INSENSITIVE);

  //** 构造 **/

  /**
   * 构建表结构对象
   */
  public FetchCase()
  {
    this.tableName  = null;
    this.name       = null;
    this.fields     = new StringBuilder();
    this.wheres     = new StringBuilder();
    this.groups     = new StringBuilder();
    this.havins     = new StringBuilder();
    this.orders     = new StringBuilder();
    this.limits     = new int[0];
    this.wparams    = new ArrayList();
    this.hparams    = new ArrayList();
    this.options    = new  HashMap ();
    this.joinType   = 0 ;
    this.joinExpr   = "";
    this.joinList   = new LinkedHashSet();
  }

  /**
   * 克隆
   * @return 新查询结构对象
   */
  @Override
  public FetchCase clone()
  {
    try {
    FetchCase caze  = (FetchCase)  super.clone( );
    caze.fields     = new StringBuilder( this.fields );
    caze.wheres     = new StringBuilder( this.wheres );
    caze.groups     = new StringBuilder( this.groups );
    caze.havins     = new StringBuilder( this.havins );
    caze.orders     = new StringBuilder( this.orders );
    caze.limits     = this.limits.clone( );
    caze.wparams    = new ArrayList(this.wparams);
    caze.hparams    = new ArrayList(this.hparams);
    caze.options    = new  HashMap (this.options);
    caze.joinType   = 0 ;
    caze.joinExpr   = "";
    caze.joinList   = new LinkedHashSet(this.joinList);
    return caze;
    }
    catch (CloneNotSupportedException ex) {
        throw  new  HongsError.Common(ex);
    }
  }

  //** 查询 **/

  /***
   * 设置查询表和别名
   * @param tableName
   * @param name
   * @return 当前查询结构对象
   */
  public FetchCase from(String tableName, String name)
  {
    this.tableName = tableName;
    this.name = name;
    return this;
  }

  /**
   * 设置查询表(如果别名已设置则不会更改)
   * @param tableName
   * @return 当前查询结构对象
   */
  public FetchCase from(String tableName)
  {
    this.tableName = tableName;
    if (this.name == null)
        this.name  = tableName;
    return this;
  }

  /**
   * 追加查询字段
   * 必须包含当前表字段, 必须在当前表字段前加"."
   * @param fields
   * @return 当前查询结构对象
   */
  public FetchCase select(String fields)
  {
    this.fields.append(", ").append(fields);
    return this;
  }

  /**
   * 追加查询条件
   * 字段名前, 用"."表示属于当前表, 用":"表示属于上级表
   * @param where
   * @param params 对应 where 中的 ?
   * @return 当前查询结构对象
   */
  public FetchCase where(String where, Object... params)
  {
    this.wheres.append(" AND ").append(where);
    this.wparams.addAll(Arrays.asList(params));
    return this;
  }

  /**
   * where 的别名
   * @param where
   * @param params
   * @return
   */
  public FetchCase filter(String where, Object... params) {
    return where(where, params);
  }

  /**
   * 追加分组字段
   * 必须包含当前表字段, 必须在当前表字段前加"."
   * @param fields
   * @return 当前查询结构对象
   */
  public FetchCase groupBy(String fields)
  {
    this.groups.append(", ").append(fields);
    return this;
  }

  /**
   * 追加过滤条件
   * 字段名前, 用"."表示属于当前表, 用":"表示属于上级表
   * @param where
   * @param params 对应 where 中的 ?
   * @return 当前查询结构对象
   */
  public FetchCase which(String where, Object... params)
  {
    this.havins.append(" AND ").append(where);
    this.hparams.addAll(Arrays.asList(params));
    return this;
  }

  /**
   * which 的正名
   * @param where
   * @param params
   * @return
   */
  public FetchCase having(String where, Object... params) {
    return which(where, params);
  }

  /**
   * 追加排序字段
   * 必须包含当前表字段, 必须在当前表字段前加"."
   * @param fields
   * @return 当前查询结构对象
   */
  public FetchCase orderBy(String fields)
  {
    this.orders.append(", ").append(fields);
    return this;
  }

  //** 限额 **/

  /**
   * 设置限额
   * @param start
   * @param limit
   * @return 当前查询结构对象
   */
  public FetchCase limit(int start, int limit)
  {
    this.limits = limit == 0 ? new int[0] : new int[] {start, limit};
    return this;
  }

  /**
   * 设置限额
   * @param limit
   * @return 当前查询结构对象
   */
  public FetchCase limit(int limit)
  {
    this.limits = limit == 0 ? new int[0] : new int[] {  0  , limit};
    return this;
  }

  //** 关联 **/

  public FetchCase join(String tableName, String name)
  {
    FetchCase caze = this.join(new FetchCase());
    caze.from(tableName, name);
    return caze;
  }

  public FetchCase join(String tableName)
  {
    FetchCase caze = this.join(new FetchCase());
    caze.from(tableName);
    return caze;
  }

  public FetchCase join(FetchCase caze)
  {
    caze.options  = this.options;
    this.joinList.add(caze);
    caze.joinName = null;
    caze.joinExpr = null;
    caze.joinType = LEFT;
    return caze;
  }

  public FetchCase by( byte  type)
  {
    this.joinType = type;
    return this;
  }

  public FetchCase on(String expr)
  {
    this.joinExpr = expr;
    return this;
  }

  public FetchCase in(String name)
  {
    this.joinName = name;
    return this;
  }

  //** 获取结果 **/

  /**
   * 获取SQL
   * @return SQL
   */
  public String getSQL()
  {
    return this.getSQLStrb().toString();
  }

  /**
   * 获取SQL字串
   * @return SQL字串
   */
  private StringBuilder getSQLStrb()
  {
    StringBuilder t = new StringBuilder();
    StringBuilder f = new StringBuilder();
    StringBuilder g = new StringBuilder();
    StringBuilder o = new StringBuilder();
    StringBuilder w = new StringBuilder();
    StringBuilder h = new StringBuilder();
    this.getSQLDeep(  t, f, g, o, w, h, null);

    StringBuilder sql = new StringBuilder("SELECT");

    // 字段
    if (f.length() != 0)
    {
      sql.append( " " )
         .append(ps.matcher(f).replaceFirst(""));
    }
    else
    {
      sql.append(" `" )
         .append(this.name)
         .append("`.*");
    }

    // 表名
    sql.append(" FROM ").append(t);

    // 条件
    if (w.length() != 0)
    {
      sql.append(" WHERE " )
         .append(pw.matcher(w).replaceFirst(""));
    }

    // 分组
    if (g.length() != 0)
    {
      sql.append(" GROUP BY ")
         .append(ps.matcher(g).replaceFirst(""));
    }

    // 过滤
    if (h.length() != 0)
    {
      sql.append(" HAVING ")
         .append(pw.matcher(h).replaceFirst(""));
    }

    // 排序
    if (o.length() != 0)
    {
      sql.append(" ORDER BY ")
         .append(ps.matcher(o).replaceFirst(""));
    }

    // 限额, 不同库不同方式, 就不在此处理了
//    if (this.limits.length > 0)
//    {
//      sql.append(" LIMIT ?, ?");
//    }

//    sql = DB.formatSQLFields(sql);

    return sql;
  }

  /**
   * 获取SQL组合
   * @return SQL组合
   */
  private void getSQLDeep(StringBuilder t, StringBuilder f,
                          StringBuilder g, StringBuilder o,
                          StringBuilder w, StringBuilder h,
                          String pn)
  {
    if (this.tableName == null
    ||  this.tableName.length() == 0)
    {
        throw new Error( new HongsException(0x10b4) );
    }

    // 表名
    String tn;
    StringBuilder b = new StringBuilder();
    b.append("`").append(this.tableName ).append("`");
    if (this.name != null &&  this.name.length() != 0
    && !this.name.equals(this.tableName))
    {
      b.append(" AS `").append(this.name).append("`");
      tn = this.name;
    }
    else
    {
      tn = this.tableName;
    }

    // 关联
    if (pn != null)
    {
      switch (this.joinType)
      {
        case FetchCase.LEFT : b.insert(0, " LEFT JOIN "); break;
        case FetchCase.RIGHT: b.insert(0," RIGHT JOIN "); break;
        case FetchCase.FULL : b.insert(0, " FULL JOIN "); break;
        case FetchCase.INNER: b.insert(0," INNER JOIN "); break;
        case FetchCase.CROSS: b.insert(0," CROSS JOIN "); break;
        default: return;
      }
      if (this.joinExpr != null && this.joinExpr.length() != 0)
      {
        String s  =  this.joinExpr;
        s = addSQLTbls( s, tn, pn);
        b.append(" ON ").append(s);
      }
    }

    t.append(b);

    // 字段
    if (this.fields.length() != 0)
    {
      String s = this.fields.toString().trim();
      s = addSQLTbls(s, tn, pn);

      // Add in 2016/4/15
      // 为关联表的查询列添加层级名
      if (null != joinName) {
          if (!"".equals(joinName)) {
              s = addSQLTbln(s, joinName);
          } else {
              s = addSQLTbln(s, tn);
          }
      }

      f.append(" ").append( s );
    }

    // 条件
    if (this.wheres.length() != 0)
    {
      String s = this.wheres.toString().trim();
      s = addSQLTbls(s, tn, pn);
      w.append(" ").append( s );
    }

    // 分组
    if (this.groups.length() != 0)
    {
      String s = this.groups.toString().trim();
      s = addSQLTbls(s, tn, pn);
      g.append(" ").append( s );
    }

    // 下级
    for  ( FetchCase caze : this.joinList)
    {
      if ( caze.joinType != 0 )
      {
        caze.getSQLDeep(t, f, g, o, w, h, tn );
      }
    }

    // 筛选
    if (this.havins.length() != 0)
    {
      String s = this.havins.toString().trim();
      s = addSQLTbls(s, tn, pn);
      h.append(" ").append( s );
    }

    // 排序
    if (this.orders.length() != 0)
    {
      String s = this.orders.toString().trim();
      s = addSQLTbls(s, tn, pn);
      o.append(" ").append( s );
    }
  }

  /**
   * JOIN 查询时
   * 需要给字段赋别名
   * 否则可能导致重名
   * 影响到结果丢失或不能分配到层级数据
   * @param s
   * @param an
   * @return
   */
  private String addSQLTbln(String s, String an)
  {
      StringBuffer sb = new StringBuffer( );
      Matcher      ma = pa.matcher(s);
      String       fn;
      while ( ma.find() ) {
              fn = ma.group(2);
          if (fn == null) {
              fn = ma.group(3);
          }
          if (ma.group(1) == null) {
              fn = "`"+fn+"` AS `"+an+"."+fn+"`";
          } else {
              fn = /* alias */ "`"+an+"."+fn+"`";
          }
          fn = Matcher.quoteReplacement(fn);
          ma.appendReplacement(sb, "$1"+fn+"$4");
      }
      ma.appendTail ( sb );
      return sb.toString();
  }

  /**
   * 替换SQL表名
   * @param s
   * @param tn
   * @param pn
   * @return
   */
  private String addSQLTbls(String s, String tn, String pn)
  {
      // 没有关联表则直接去掉前缀
      if (this.joinList.isEmpty()
      &&  this.joinType  ==  0  ) {
          return delSQLTbls( s  );
      }

      String       x, y, z;
      StringBuffer f = new StringBuffer(s);
      StringBuffer b;
      Matcher      m;

      z = "`"+tn+"`.$0";
      m = p0.matcher(f);
      b = new StringBuffer();
      int i , j, k = -1, l = f.length();
      while ( m.find( )) {
//        System.out.println(m.group());
          // 以 .|(|{ 结尾的要跳过
          j = m.end ( );
          if ( j <  l ) {
              char r = f.charAt(j/**/);
              if ( r == '.' || r == '(' || r == '{' ) {
                   k = j;
                  continue;
              }
          }
          // 以 .|:|! 开头的要跳过
          i = m.start();
          if ( i >  0 ) {
              char r = f.charAt(i - 1);
              if ( r == '.' || r == ':' || r == '!' ) {
                   k = j;
                  continue;
              }
          }
          x = m.group(1);
          if (x.startsWith("'")/*ST*/) {
              // 字符串后不跟字段
              k  = j;
          } else
          if (x.startsWith("*")&&k==i) {
              // 跳过乘号且不偏移
          } else
          if (p2.matcher(x).matches()) {
              // 跳过保留字不偏移
          } else
          if (p1.matcher(x).matches()) {
              // 跳过别名和数字等
              k  = j;
          } else
          if (k == i) {
              // 紧挨前字段要跳过
              k  = j;
          } else {
              // 为字段添加表前缀
              k  = j;
              m.appendReplacement(b,z);
          }
      }
      f = m.appendTail(b);

      //** 符号标识方式(旧,兼容) **/

      x = "$1";
      y = "`"+pn+"`.$1";
      z = "`"+tn+"`.$1";
      m = pc.matcher(f);
      b = new StringBuffer();
      while ( m.find( )) {
          switch (f.charAt(m.start())) {
              case '.': m.appendReplacement(b, z); break;
              case ':': m.appendReplacement(b, y); break;
              case '!': m.appendReplacement(b, x); break;
          }
      }
      f = m.appendTail(b);

      return f.toString();
  }

  /**
   * 替换SQL表名
   * @param s
   * @return
   */
  private String delSQLTbls(String s)
  {
      Matcher      m;
      String       x;
      StringBuffer b;
      StringBuffer c = new StringBuffer(s);

      x = "$1";
      m = pc.matcher(c);
      b = new StringBuffer();
      while ( m.find( )) {
          m.appendReplacement(b, x);
      }
      c = m.appendTail(b);

      return c.toString();
  }

  public int getStart() {
    return this.limits.length > 0 ? this.limits[0] : 0;
  }

  public int getLimit() {
    return this.limits.length > 1 ? this.limits[1] : 0;
  }

  /**
   * 获取参数
   * @return 参数
   */
  public Object[] getParams()
  {
    return this.getParamsList().toArray();
  }

  /**
   * 获取参数列表
   * @return 参数列表
   */
  private List getParamsList()
  {
    List paramz = new ArrayList();
    List wparamz = new ArrayList();
    List hparamz = new ArrayList();

    // 参数
    this.getParamsDeep(wparamz, hparamz);
    paramz.addAll(wparamz);
    paramz.addAll(hparamz);

    // 限额(不同数据库的限额方式不一样, 在 DB.limit 中实现)
//    if (this.limits.length > 0)
//    {
//      paramz.add(this.limits[0]);
//      paramz.add(this.limits[1]);
//    }

    return paramz;
  }

  /**
   * 获取参数组合
   * @return 参数组合
   */
  private void getParamsDeep(List wparamz, List hparamz)
  {
    wparamz.addAll(this.wparams);
    hparamz.addAll(this.hparams);

    for (FetchCase caze  :  this.joinList)
    {
      if (0 == caze.joinType)
      {
        continue;
      }

      caze.getParamsDeep(wparamz, hparamz);
    }
  }

  /**
   * 转换为字符串
   * @return 合并了SQL和参数
   */
  @Override
  public String toString()
  {
    StringBuilder sb = this.getSQLStrb();
    List   paramz = this.getParamsList();
    try
    {
      DB.checkSQLParams(sb, paramz);
      DB.mergeSQLParams(sb, paramz);
    }
    catch (HongsException ex)
    {
           return   null;
    }
    return sb.toString();
  }

  //** 获取关联 **/

  /**
   * 获取关联对象
   * @param name
   * @return
   */
  public FetchCase getJoin(String name)
  {
    for (FetchCase caze : this.joinList)
    {
      if (name.equals(caze.name))
      {
        return caze;
      }
    }
    return null;
  }

  /**
   * 获取关联的关联对象
   * @param name
   * @return
   */
  public FetchCase getJoin(String... name)
  {
    FetchCase caze = this;
    for (String n : name)
    {
      FetchCase c = caze.getJoin(n);
      if (null != c) {
          caze  = c;
      } else {
          caze  = null; break; /* ignore */
      }
    }
    return caze;
  }

  /**
   * 获取关联的关联对象
   *
   * 与 getJoin 不同在于不存在的关联会自动则创建
   * 注意:
   * 命名虽与 Core.got 类似, 但意义却不同
   * Core.got 为调用原 Map 的 get, 没有则返回 null
   * FetchCase.gotJoin 相反没有则创建关联
   *
   * @param name
   * @return
   * @throws HongsException
   */
  public FetchCase gotJoin(String... name)
    throws HongsException
  {
    FetchCase caze = this;
    for (String n : name)
    {
      FetchCase c = caze.getJoin(n);
      if (null != c) {
          caze  = c;
      } else {
          caze  = caze.join(n).by((byte)0);
      }
    }
    return caze;
  }

  //** 获取选项 **/

  /**
   * 是否存在选项
   * @param key
   * @return 存在为true, 反之为false
   */
  public boolean hasOption(String key)
  {
    return this.options.containsKey(key);
  }

  /**
   * 获取选项
   * @param key
   * @return 指定选项
   */
  public Object getOption(String key)
  {
    return this.options.get(key);
  }

  /**
   * 获取选项(可指定类型)
   * @param <T>
   * @param key
   * @param def
   * @return 指定选项
   */
  public <T> T getOption(String key, T def)
  {
    return Synt.asserts(this.getOption(key), def);
  }

  /**
   * 设置参数(单个)
   * @param key
   * @param obj
   * @return 当前查询结构对象
   */
  public FetchCase setOption(String key, Object obj)
  {
    this.options.put(key, obj);
    return this;
  }

  //** 不推荐的方法 **/

  /**
   * 是否有设置表名
   * @return 存在未true, 反之为false
   * @deprecated
   */
  public boolean hasFrom()
  {
    return this.tableName != null;
  }

  /**
   * 是否有关联的表
   * @return 存在未true, 反之为false
   * @deprecated
   */
  public boolean hasJoin()
  {
    return !this.joinList.isEmpty();
  }

  /**
   * 是否有设置查询字段
   * @return 存在为true, 反之为false
   * @deprecated
   */
  public boolean hasSelect()
  {
    return this.fields.length() != 0;
  }

  /**
   * 设置查询字段
   * @param fields
   * @return 当前查询结构对象
   * @deprecated
   */
  public FetchCase setSelect(String fields)
  {
    this.fields = new StringBuilder(checkField(fields));
    return this;
  }

  /**
   * 是否有设置查询条件
   * @return 存在为true, 反之为false
   * @deprecated
   */
  public boolean hasWhere()
  {
    return this.wheres.length() != 0;
  }

  /**
   * 设置查询条件
   * 字段名前, 用"."表示属于当前表, 用":"表示属于上级表
   * @param where
   * @param params 对应 where 中的 ?
   * @return 当前查询结构对象
   * @deprecated
   */
  public FetchCase setWhere(String where, Object... params)
  {
    this.wheres = new StringBuilder(checkWhere(where));
    this.wparams = Arrays.asList(params);
    return this;
  }

  /**
   * 是否有设置分组
   * @return 存在为true, 反之为false
   * @deprecated
   */
  public boolean hasGroupBy()
  {
    return this.groups.length() != 0;
  }

  /**
   * 设置分组字段
   * @param fields
   * @return 当前查询结构对象
   * @deprecated
   */
  public FetchCase setGroupBy(String fields)
  {
    this.groups = new StringBuilder(checkField(fields));
    return this;
  }

  /**
   * @deprecated
   */
  public boolean hasWhich()
  {
    return this.havins.length() != 0;
  }

  /**
   * @deprecated
   */
  public FetchCase setWhich(String where, Object... params)
  {
    this.havins = new StringBuilder(checkWhere(where));
    this.hparams = Arrays.asList(params);
    return this;
  }

  /**
   * 是否有设置排序
   * @return 存在为true, 反之为false
   * @deprecated
   */
  public boolean hasOrderBy()
  {
    return this.orders.length() != 0;
  }

  /**
   * 设置排序字段
   * @param fields
   * @return 当前查询结构对象
   * @deprecated
   */
  public FetchCase setOrderBy(String fields)
  {
    this.orders = new StringBuilder(checkField(fields));
    return this;
  }

  private String checkField(String field)
  {
    if (field == null) return "";
        field = field.trim();
    if (field.length() != 0
    && !field.startsWith(","))
    {
      return ", " + field;
    }
    else
    {
      return field;
    }
  }

  private String checkWhere(String where)
  {
    if (where == null) return "";
        where = where.trim();
    if (where.length() != 0
    && !where.matches("^(AND|OR) (?i)"))
    {
      return "AND " + where;
    }
    else
    {
      return where;
    }
  }

  //** 串联查询/操作 **/

  private DB _db_ = null;

  /**
   * 指定查询要查询的库
   * @param db
   * @return
   */
  public FetchCase use(DB db)
  {
    _db_ =  db ;
    return this;
  }

  /**
   * 查询并获取单个结果
   * @return
   * @throws HongsException
   */
  public Map  one() throws HongsException {
    if (_db_ == null) {
      throw new HongsException(0x10b6);
    }
    return _db_.fetchLess(this);
  }

  /**
   * 查询并获取全部结果
   * @return
   * @throws HongsException
   */
  public List all() throws HongsException {
    if (_db_ == null) {
      throw new HongsException(0x10b6);
    }
    return _db_.fetchMore(this);
  }

  /**
   * 查询并获取记录集合
   * @return
   * @throws HongsException
   */
  public Roll rol() throws HongsException {
    if (_db_ == null) {
      throw new HongsException(0x10b6);
    }
    return _db_.queryMore(this);
  }

  /**
   * 删除全部匹配的记录
   * 注意: 会忽略 join 的条件, 有 :fn, xx.fn 的字段条件会报 SQL 错误
   * @return
   * @throws HongsException
   */
  public int  delete() throws HongsException {
    if (_db_ == null) {
      throw new HongsException(0x10b6);
    }
    String whr = delSQLTbls(wheres.toString());
    whr  =  pw.matcher(whr).replaceFirst( "" );
    return _db_.delete(tableName, /**/ whr, wparams.toArray());
  }

  /**
   * 更新全部匹配的数据
   * 注意: 会忽略 join 的条件, 有 :fn, xx.fn 的字段条件会报 SQL 错误
   * @param dat
   * @return
   * @throws HongsException
   */
  public int  update(Map<String, Object> dat) throws HongsException {
    if (_db_ == null) {
      throw new HongsException(0x10b6);
    }
    String whr = delSQLTbls(wheres.toString());
    whr  =  pw.matcher(whr).replaceFirst( "" );
    return _db_.update(tableName, dat, whr, wparams.toArray());
  }

  /**
   * 插入当前指定的数据
   * 其实与 FetchCase 无关, 因为 insert 是没有 where 等语句的
   * 但为保障支持的语句完整让 FetchCase 看着像 ORM 还是放一个
   * @param dat
   * @return
   * @throws HongsException
   */
  public int  insert(Map<String, Object> dat) throws HongsException {
    if (_db_ == null) {
      throw new HongsException(0x10b6);
    }
    return _db_.insert(tableName, dat);
  }

}
