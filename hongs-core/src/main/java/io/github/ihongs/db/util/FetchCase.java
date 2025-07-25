package io.github.ihongs.db.util;

import io.github.ihongs.CruxException;
import io.github.ihongs.CruxExemption;
import io.github.ihongs.db.link.Link;
import io.github.ihongs.db.link.Loop;
import io.github.ihongs.db.link.Lump;
import io.github.ihongs.util.Synt;
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
 * <h3>使用以下方法将SQL语句拆解成对应部分:</h3>
 * <pre>
 * field        SELECT    field1, field2
 * from         FROM      tableName AS name
 * join         LEFT JOIN assocName AS nam2 ON nam2.xx = name.yy
 * where        WHERE     expr1 AND expr2
 * group        GROUP BY  field1, field2
 * havin        HAVING    expr1 AND expr2
 * order        ORDER BY  field1, field2
 * limit        LIMIT     start, limit
 *
 * 注意:
 * field, where, group, havin, order  为设置方法, 将清空原值;
 * select,filter,gather,having,assort 为追加方法, 保留原设值;
 * 如果 CLEVER_MODE 开启, 将自动根据关联层级补全表名和别名;
 * 避免 CLEVER_MODE 下使用较复杂的语句, 仅对常规语句有测试;
 * </pre>
 *
 * <h3>系统已知 options:</h3>
 * <pre>
 * CLEVER_MODE  : boolean     聪明模式, 设为 true 则自动根据关联层级补全表名和别名
 * STRING_MODE  : boolean     字串模式, 设为 true 则获取结果中的字段值全部转为字串
 * ASSOC_MULTI  : boolean     对多关联(使用IN方式关联); 作用域: FetchMore
 * ASSOC_MERGE  : boolean     归并关联(仅限非对多关联); 作用域: FetchMore
 * ASSOC_PATCH  : boolean     给缺失的关联补全空白数据; 作用域: FetchMore
 * ASSOC_TYPES  : Set         仅对某些类型关联; 作用域: AssocMore
 * ASSOC_JOINS  : Set         仅对某些类型连接; 作用域: AssocMore
 * ASSOCS       : Set         仅对某些表做关联; 作用域: AssocMore
 * CHECKS       ：Set         查询检查设置标识; 作用域: AssocMore
 * pn           : int         分页页码; 作用域: FetchPage
 * gn           : int         链接数量; 作用域: FetchPage
 * rn           : int         分页行数; 作用域: FetchPage
 * </pre>
 *
 * <h3>异常代码:</h3>
 * <pre>
 * 区间: 1160~1169
 * 1160 无法识别关联类型(JOIN)
 * 1161 必须指定关联条件
 * 1162 没有指定查询表名
 * 1163 没有指定查询的库
 * </pre>
 *
 * @author Hongs
 */
public class FetchCase
  implements Cloneable, Serializable
{

  protected       String         tableName;
  protected       String         name;
  protected       int            start;
  protected       int            limit;
  protected final StringBuilder  fields;
  protected final StringBuilder  wheres;
  protected final StringBuilder  groups;
  protected final StringBuilder  havins;
  protected final StringBuilder  orders;
  protected final List           wparams;
  protected final List           vparams;
  protected       Map            options;
  protected final Set<FetchCase> joinSet;
  protected       FetchCase      joinSup;
  protected       String         joinName;
  protected       String         joinExpr;
  protected       byte           joinType;

  public static final byte DISTINCT = -1;
  public static final byte NONE     =  0;
  public static final byte INNER    =  1;
  public static final byte LEFT     =  2;
  public static final byte RIGHT    =  3;
  public static final byte FULL     =  4;
  public static final byte CROSS    =  5;
  public static final byte STRICT   =  2;
  public static final byte CLEVER   =  3;
  public static final byte STRING   =  4;

  /**
   * 查找列名加关联层级名
   */
  private static final Pattern SQL_ALIAS = Pattern
        .compile("([\"'\\w\\)]\\s+)?(?:\"(\\w+)\"|(\\w+))(\\s*,?$)");

  /**
   * 查找与字段相关的元素
   */
  private static final Pattern SQL_FIELD = Pattern
        .compile("(\\s*)(\".*?\"|'.*?'|\\w+|\\*|\\))(\\s*)");

  /**
   * 后面不跟字段可跟别名, \\d 换成 \\w 则仅处理被 '"' 包裹的字段
   */
  private static final Pattern SQL_FIELD_BEFORE_ALIAS = Pattern
        .compile("AS|END|NULL|TRUE|FALSE|\\)|\\d.*"
                    ,  Pattern.CASE_INSENSITIVE);

  /**
   * 后面可跟字段的关键词
   */
  private static final Pattern SQL_FIELD_BEFORE_WORDS = Pattern
        .compile("IS|IN|ON|OR|AND|NOT|TOP|CASE|WHEN|THEN|ELSE|LIKE|ESCAPE|BETWEEN|DISTINCT"
                    ,  Pattern.CASE_INSENSITIVE);

  //** 构造 **/

  /**
   * 简单构造
   */
  public FetchCase()
  {
    this ((byte) 0 );
  }

  /**
   * 高级构造
   * mode 可选常量:
   * CLEVER 智能模式, 自动根据关联层级补全表名和别名
   * STRICT 严格模式, 与智能模式相反, 但拼接速度较快
   * STRING 字符模式, 总是获取字符串
   * 不设置 CLEVER 或 STRICT 则当有 JOIN 时使用 CLEVER 模式
   * @param mode 2 STRICT, 3 CLEVER, 4 STRING
   */
  public FetchCase(byte mode)
  {
    this.start      = 0;
    this.limit      = 0;
    this.fields     = new StringBuilder();
    this.wheres     = new StringBuilder();
    this.groups     = new StringBuilder();
    this.havins     = new StringBuilder();
    this.orders     = new StringBuilder();
    this.wparams    = new ArrayList();
    this.vparams    = new ArrayList();
    this.options    = new  HashMap ();
    this.joinSet    = new LinkedHashSet();
    this.joinType   = NONE;

    if (4 == (4 & mode)) {
      this.options.put("STRING_MODE", true );
    }
    if (3 == (3 & mode)) {
      this.options.put("CLEVER_MODE", true );
    } else
    if (2 == (2 & mode)) {
      this.options.put("CLEVER_MODE", false);
    }
  }

  /**
   * 拷贝构造
   * 用于 clone/klone
   * @param caze 源用例
   * @param deep 深拷贝
   */
  protected FetchCase(FetchCase caze, boolean deep)
  {
    this(caze,null, new HashMap(caze.options),deep);
  }
  private FetchCase(FetchCase caze, FetchCase csup, Map opts, boolean deep)
  {
    this.tableName  = caze.tableName;
    this.name       = caze.name;
    this.start      = caze.start;
    this.limit      = caze.limit;
    this.fields     = new StringBuilder(caze.fields);
    this.wheres     = new StringBuilder(caze.wheres);
    this.groups     = new StringBuilder(caze.groups);
    this.havins     = new StringBuilder(caze.havins);
    this.orders     = new StringBuilder(caze.orders);
    this.wparams    = new ArrayList(caze.wparams);
    this.vparams    = new ArrayList(caze.vparams);
    this.options    = opts;
    this.joinSet    = new LinkedHashSet();
    this.joinType   = caze.joinType;
    this.joinExpr   = caze.joinExpr;
    this.joinName   = caze.joinName;
    this.joinSup    = csup;
    this.link       = caze.link;
    this.doer       = caze.doer;

    if (deep) for(/**/FetchCase caxe : caze.joinSet/**/) {
      joinSet.add(new FetchCase(caxe , this, opts, deep));
    }
  }

  //** 查询 **/

  /***
   * 设置查询表和别名
   * @param tableName
   * @param name
   * @return 当前实例
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
   * @return 当前实例
   */
  public FetchCase from(String tableName)
  {
    this.tableName = tableName;
    if ( this.name == null )
         this.name = tableName;
    return this;
  }

  /**
   * 设置查询字段
   * @param field
   * @return 当前实例
   */
  public FetchCase field(String field)
  {
    this.fields.setLength(0);
    if ( field != null && field.length() != 0) {
        select ( field );
    }
    return this;
  }

  /**
   * 设置查询条件
   * @param where
   * @param params 对应 where 中的 ?
   * @return 当前实例
   */
  public FetchCase where(String where, Object... params)
  {
    this.wheres.setLength(0);
    this.wparams.clear(/**/);
    if ( where != null && where.length() != 0) {
        filter(where,params);
    }
    return this;
  }

  /**
   * 设置分组字段
   * @param field
   * @return 当前实例
   */
  public FetchCase group(String field)
  {
    this.groups.setLength(0);
    if ( field != null && field.length() != 0) {
        gather(field);
    }
    return this;
  }

  /**
   * 设置过滤条件
   * @param where
   * @param params 对应 where 中的 ?
   * @return 当前实例
   */
  public FetchCase havin(String where, Object... params)
  {
    this.havins.setLength(0);
    this.vparams.clear(/**/);
    if ( where != null && where.length() != 0) {
        having(where,params);
    }
    return this;
  }

  /**
   * 设置排序字段
   * @param field
   * @return 当前实例
   */
  public FetchCase order(String field)
  {
    this.orders.setLength(0);
    if ( field != null && field.length() != 0) {
        assort(field);
    }
    return this;
  }

  /**
   * 限额, 关联查询上无效
   * @param start
   * @param limit
   * @return 当前实例
   */
  public FetchCase limit(int start, int limit)
  {
    this.start = start;
    this.limit = limit;
    return this;
  }

  /**
   * 限额, 关联查询上无效
   * @param limit
   * @return 当前实例
   */
  public FetchCase limit(int limit)
  {
    this.limit(0 , limit);
    return this;
  }

  //** 追加 **/

  /**
   * 追加查询字段
   * @param field
   * @return 当前实例
   */
  public FetchCase select(String field)
  {
    if (this.fields.length() > 0) {
        this.fields.append( ", ");
    }   this.fields.append(field);
    return this;
  }

  /**
   * 追加查询条件
   * @param where
   * @param params 对应 where 中的 ?
   * @return 当前实例
   */
  public FetchCase filter(String where, Object... params)
  {
    if (this.wheres.length() > 0) {
        this.wheres.append(" AND ");
    }   this.wheres.append( where );
    this.wparams.addAll(Arrays.asList(params));
    return this;
  }

  /**
   * 追加分组字段
   * @param field
   * @return 当前实例
   */
  public FetchCase gather(String field)
  {
    if (this.groups.length() > 0) {
        this.groups.append( ", ");
    }   this.groups.append(field);
    return this;
  }

  /**
   * 追加过滤条件
   * @param where
   * @param params 对应 where 中的 ?
   * @return 当前实例
   */
  public FetchCase having(String where, Object... params)
  {
    if (this.havins.length() > 0) {
        this.havins.append(" AND ");
    }   this.havins.append( where );
    this.vparams.addAll(Arrays.asList(params));
    return this;
  }

  /**
   * 追加排序字段
   * @param field
   * @return 当前实例
   */
  public FetchCase assort(String field)
  {
    if (this.orders.length() > 0) {
        this.orders.append( ", ");
    }   this.orders.append(field);
    return this;
  }

  //** 关联 **/

  /**
   * 关联已有的用例
   * <pre>
   * 注意: <b>不建议使用</b>.
   * 选项与上级会共用, 关联的下级将重用.
   * 请避免在不同的用例上重用关联的用例,
   * 稍不注意就会致与后者的关联关系混乱.
   * 可以使用 clone/klone 进行深或浅克隆,
   * 构造时非 STRICT 且未设置 CLEVER_MODE 则将开启 CLEVER_MODE, 其它 join 同此
   * </pre>
   * @param caze
   * @return join 前(左) 的用例
   */
  public FetchCase join(FetchCase caze)
  {
    this.joinSet.add(caze);
    caze.joinSup =   this ;
    caze.options = options;
    if (caze.joinType <= NONE)
    {
        caze.joinType = INNER;
    }
    if (caze.joinName == null)
    {
        caze.joinName =  ""  ;
    }
    return this;
  }

  /**
   * 快速关联
   * @param tableName
   * @param name
   * @param on
   * @param by
   * @return join 前(左) 的用例
   */
  public FetchCase join(String tableName, String name, String on, byte by)
  {
    FetchCase caze = new FetchCase();
    this.join(caze);
    caze.from(tableName, name)
        .on  ( on )
        .by  ( by );
    return    this ;
  }

  /**
   * 快速关联
   * @param tableName
   * @param name
   * @param on
   * @return join 前(左) 的用例
   */
  public FetchCase join(String tableName, String name, String on)
  {
    FetchCase caze = new FetchCase();
    this.join(caze);
    caze.from(tableName, name)
        .on  ( on );
    return    this ;
  }

  /**
   * 关联
   * 注意: 此方法返回新构建的关联对象
   * @param tableName
   * @param name
   * @return
   */
  public FetchCase join(String tableName, String name)
  {
    FetchCase caze = new FetchCase();
    this.join(caze);
    caze.from(tableName, name);
    return    caze ;
  }

  /**
   * 关联
   * 注意: 此方法返回新构建的关联对象
   * @param tableName
   * @return
   */
  public FetchCase join(String tableName)
  {
    FetchCase caze = new FetchCase();
    this.join(caze);
    caze.from(tableName);
    return    caze ;
  }

  /**
   * 关联类型
   * @param type
   * @return
   */
  public FetchCase by(byte type)
  {
    this.joinType = type;
    return this;
  }

  /**
   * 关联条件
   * @param expr
   * @return
   */
  public FetchCase on(String expr)
  {
    this.joinExpr = expr;
    return this;
  }

  /**
   * 查询字段前缀
   * 比如 select("fn1,fn2").in("foo") 得到 "foo.fn1","foo.fn2"
   * 查询结果会按 . 自动拆解为层级结构, 如 foo={fn1=v1,fn2=v2}
   * 仅当选项 CLEVER_MODE 为 true 时才有效
   * @param name
   * @return
   */
  public FetchCase in(String name)
  {
    this.joinName = name;
    return this;
  }

  //** 结果 **/

  /**
   * 获取SQL
   * @return SQL
   */
  public String getSQL()
  {
    return this.getSQLText().toString();
  }

  /**
   * 获取SQL字串
   * @return SQL字串
   */
  private StringBuilder getSQLText()
  {
    StringBuilder t = new StringBuilder();
    StringBuilder f = new StringBuilder();
    StringBuilder w = new StringBuilder();
    StringBuilder g = new StringBuilder();
    StringBuilder h = new StringBuilder();
    StringBuilder o = new StringBuilder();

    boolean hasJoins = (joinSet.isEmpty( )  ==  false); // 有关联表
    boolean fixField = getOption("CLEVER_MODE", false); // 补全语句

    getSQLDeep(t, f, w, g, h, o, null, null, fixField, hasJoins);

    StringBuilder sql = new StringBuilder("SELECT");

    // 去重
    if (joinType == DISTINCT )
    {
      sql.append( " DISTINCT");
    }

    // 字段
    if (f.length() > 0)
    {
      sql.append( ' ' )
         .append(  f  );
    }
    else
    {
      sql.append( ' ' )
         .append(Link.Q(getName()))
         .append( '.' )
         .append( '*' );
    }

    // 表名
    sql.append(" FROM ").append(t);

    // 条件
    if (w.length() > 0)
    {
      sql.append(" WHERE " )
         .append(  w  );
    }

    // 分组
    if (g.length() > 0)
    {
      sql.append(" GROUP BY ")
         .append(  g  );
    }

    // 过滤
    if (h.length() > 0)
    {
      sql.append(" HAVING ")
         .append(  h  );
    }

    // 排序
    if (o.length() > 0)
    {
      sql.append(" ORDER BY ")
         .append(  o  );
    }

    return sql;
  }

  /**
   * 获取SQL组合
   * @return SQL组合
   */
  private void getSQLDeep(StringBuilder t, StringBuilder f,
                          StringBuilder w, StringBuilder g,
                          StringBuilder h, StringBuilder o,
                          String       pn, String       qn,
                         boolean fixField,
                         boolean hasJoins)
  {
    StringBuilder b ;

    // 表名
    String tn = this.tableName;
    if (tn == null || tn.length() == 0)
    {
        throw new CruxExemption(1162, "tableName can not be empty");
    }
    b = new StringBuilder( Link.Q(tn) );

    // 别名
    String an = this.name;
    if (an != null && an.length() != 0 && !an.equals(tn))
    {
      b.append(" AS ").append( Link.Q(an) );
      tn = an;
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

      CharSequence s = this.joinExpr;
      if (s != null && s.length( ) != 0)
      {
        if (fixField) {
          s = fixSQLField( s, tn );
        }
        b.append(" ON ").append(s);
      }

      /**
       * 别名层级
       * 第一层是不许前缀的
       * 第二层等同于与表名
       * 二层之后携带所有上级表名(不包含第一层表名)
       * 下面代码逻辑需从下往上看
       */
      if (! "".equals(qn)) {
        qn = qn+ "." +tn;
      } else {
        qn = tn;
      }
    } else {
        qn = "";
    }

    t.append(b);

    // 字段
    if (this.fields.length() != 0)
    {
      CharSequence s = this.fields.toString();

      // 为关联表的查询列添加层级名
      String jn = joinName;
      if (fixField && jn != null ) {
        if ( 0 < jn.length( ) ) {
          s  = fixSQLAlias(s , jn);
        } else
        if ( 0 < qn.length( ) ) {
          s  = fixSQLAlias(s , qn);
        }
      }

      if (fixField && hasJoins) {
          s  = fixSQLField(s , tn);
      }

      if (f.length() > 0) {
          f.append( ", ");
      }   f.append(  s  );
    }

    // 条件
    if (this.wheres.length() != 0)
    {
      CharSequence s = this.wheres.toString();

      if (fixField && hasJoins) {
          s  = fixSQLField(s , tn);
      }

      if (w.length() > 0) {
          w.append(" AND ");
      }   w.append(  s  );
    }

    // 分组
    if (this.groups.length() != 0)
    {
      CharSequence s = this.groups.toString();

      if (fixField && hasJoins) {
          s  = fixSQLField(s , tn);
      }

      if (g.length() > 0) {
          g.append( ", ");
      }   g.append(  s  );
    }

    // 筛选
    if (this.havins.length() != 0)
    {
      CharSequence s = this.havins.toString();

      if (fixField && hasJoins) {
          s  = fixSQLField(s , tn);
      }

      if (h.length() > 0) {
          h.append(" AND ");
      }   h.append(  s  );
    }

    // 排序
    if (this.orders.length() != 0)
    {
      CharSequence s = this.orders.toString();

      if (fixField && hasJoins) {
          s  = fixSQLField(s , tn);
      }

      if (o.length() > 0) {
          o.append( ", ");
      }   o.append(  s  );
    }

    // 下级
    hasJoins = true ;
    for (FetchCase caze : this.joinSet)
    {
      if (caze.joinType != 0)
      {
        caze.getSQLDeep(t, f, w, g, h, o, tn, qn, fixField, hasJoins);
      }
    }
  }

  /**
   * 替换SQL表名(聪明模式, 慢)
   * @param s
   * @param tn
   * @return
   */
  protected static CharSequence fixSQLField(CharSequence s, String tn)
  {
      StringBuffer b = new StringBuffer ( );
      StringBuffer f = new StringBuffer (s);
      Matcher      m = SQL_FIELD.matcher(f);
      String       z = "$1"+Link.Q(tn)+".$2$3";
      String       x ;

      /**
       * 为字段名前添加表别名
       * 先找出所有可能是字段的单元;
       * 判断该单元是不是以 .( 结尾, 这些是表别名或函数名等, 跳过;
       * 判断该单元是不是以 .) 开头, 这些有表别名或字段别名, 跳过;
       * 然后排除掉纯字符串, 保留字, 别名, 数字等.
       *
       * 通过疑似字段的前后环境及偏移记录来判断, 符合以下规范:
       * [TABLE.]FIELD[[ AS] ALIAS], FUNCTION(FIELDS...)
       *
       * 以下 i 为单元开始位置, j 为单元结束位置, k 为上一组单元结束位置
       * 单元包含结尾空白字符
       */

      int i;
      int j;
      int k = -1;
      int l = f.length();

      while ( m.find( )) {
          // 以 .( 结尾的要跳过
          j = m.end ( );
          if ( j <  l ) {
              char r = f.charAt(j - 0);
              if ( r == '.' || r == '(' ) {
                   k = j;
                  continue;
              }
          }

          // 以 .) 开头的要跳过
          i = m.start();
          if ( i >  0 ) {
              char r = f.charAt(i - 1);
              if ( r == '.' || r == ')' ) {
                   k = j;
                   continue;
              }
          }

          x = m.group (2);
          if (x.charAt(0) == '\''
          ||  x.charAt(0) == ')') {
              // 字符串后不跟字段, 函数调用块也要跳过
              k  = j;
          } else
          if (x.charAt(0) == '*'
          && (f.charAt(j) == ')'
          ||  k == i) ) {
              // 跳过乘号且不偏移, COUNT (*) 也要跳过
          } else
          if (SQL_FIELD_BEFORE_ALIAS.matcher(x).matches()) {
              // 跳过别名和数字等
              k  = j;
          } else
          if (SQL_FIELD_BEFORE_WORDS.matcher(x).matches()) {
              // 跳过保留字不偏移
          } else
          if (k == i) {
              // 紧挨前字段要跳过
              k  = j;
          } else {
              // 为字段添加表前缀
              k  = j;
              m.appendReplacement(b, z);
          }
      }

      return m.appendTail(b);
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
  protected static CharSequence fixSQLAlias(CharSequence s, String an)
  {
      StringBuilder b = new StringBuilder(); // 新的 SQL
      StringBuilder p = new StringBuilder(); // 字段单元
      boolean quoteBegin = false;
      boolean fieldBegin = false;
      boolean fieldStart = false;
      int     groupLevel = 0;

      for (int i = 0; i < s.length(); i++) {
          char c = s.charAt(i);
          p.append(c);

          if (quoteBegin) {
              if (c == '\'') {
                  quoteBegin = false;
              }
              continue;
          }
          if (fieldBegin) {
              if (c == '\"') {
                  fieldBegin = false;
              }
              continue;
          }
          if (fieldStart) {
              if (c == '`') {
                  fieldBegin = false;
              }
              continue;
          }

          if (c == '\'') {
              quoteBegin = true;
              continue;
          }
          if (c == '\"') {
              fieldBegin = true;
              continue;
          }
          if (c == '`') {
              fieldStart = true;
              continue;
          }

          if (c == '(') {
              groupLevel += 1;
              continue;
          }
          if (c == ')') {
              groupLevel -= 1;
              continue;
          }
          if (groupLevel != 0) {
              continue;
          }

          if (c != ',') {
              continue;
          }

          b.append(fixSQLAliaz(p, an));
          p.setLength( 0);
      }
      if (p.length() > 0) {
          b.append(fixSQLAliaz(p, an));
      }

      return b;
  }

  /**
   * fixSQLAlias 的内部方法
   * @param s
   * @param an
   * @return
   */
  private static CharSequence fixSQLAliaz(CharSequence s, String an)
  {
      Matcher m = SQL_ALIAS.matcher(s);
      String  n ;

      if (m.find()) {
              n = m.group(2);
          if (n == null) {
              n = m.group(3);
          }
          if (m.group(1) == null) {
              n = Link.Q(n)+" AS "
                + Link.Q(an+"."+n);
          } else {
              n = Link.Q(an+"."+n);
          }
          n = Matcher.quoteReplacement(n);
          s = m.replaceFirst("$1"+n+"$4");
      }

      return s;
  }

  /**
   * 清除SQL表名
   * @param s
   * @return
   */
  private String delSQLTable (CharSequence s)
  {
      String n = Pattern.quote (getName());
      String p = "('.*?')|(?:\""+ n +"\"|`"+ n +"`|"+ n +")\\s*\\.\\s*";
      return Pattern.compile(p).matcher(s).replaceAll("$1");
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
    List  paramz = new ArrayList();
    List wparamz = new ArrayList();
    List hparamz = new ArrayList();

    this.getParamsDeep(wparamz , hparamz);
    paramz.addAll(wparamz);
    paramz.addAll(hparamz);

    return paramz;
  }

  /**
   * 获取参数组合
   * @return 参数组合
   */
  private void getParamsDeep(List wparamz, List hparamz)
  {
    wparamz.addAll(this.wparams);
    hparamz.addAll(this.vparams);

    for(FetchCase caze : this.joinSet)
    {
      if ( NONE < caze.joinType)
      {
        caze.getParamsDeep(wparamz, hparamz);
      }
    }
  }

  public int getStart()
  {
    return this.start;
  }

  public int getLimit()
  {
    return this.limit;
  }

  /**
   * 获取本级条件语句
   * 注意: 会清理表名
   * @return
   */
  public String getWhile()
  {
    return this.delSQLTable(wheres);
  }

  /**
   * 获取本级条件语句
   * @return
   */
  public String getWhere()
  {
    return this.wheres.toString(  );
  }

  /**
   * 获取本级条件参数
   * @return
   */
  public Object[] getWheres()
  {
    return this.wparams.toArray(  );
  }

  //** 选项 **/

  /**
   * 设置选项
   * @param key
   * @param obj
   * @return 当前实例
   */
  public FetchCase setOption(String key, Object obj)
  {
    this.options.put(key, obj);
    return this;
  }

  /**
   * 获取选项(可指定类型)
   * 注意: 对应选项不存在时并不会写入默认选项
   * @param <T>
   * @param key
   * @param def
   * @return 指定选项
   */
  public < T > T getOption(String key, T def)
  {
    return Synt.declare(getOption(key) , def);
  }

  /**
   * 获取选项
   * @param key
   * @return 指定选项
   */
  public Object  getOption(String key)
  {
    return this.options.get(key);
  }

  /**
   * 删除选项
   * @param key
   * @return
   */
  public Object  delOption(String key)
  {
    return this.options.remove(key);
  }

  /**
   * 检查选项
   * @param key
   * @return
   */
  public boolean hasOption(String key)
  {
    return this.options.containsKey(key);
  }

  //** 递进 **/

  /**
   * 获取关联对象
   * @param name
   * @return
   */
  public FetchCase getJoin(String name)
  {
    for (FetchCase caze : this.joinSet)
    {
      if (name.equals(caze.getName( )))
      {
        return caze;
      }
    }
    return null;
  }

  /**
   * 获取关联的关联对象
   *
   * 注意:
   * 与 gotJoin 不同,
   * 仅提取已经关联了的对象,
   * 不存在的关联会返回空值.
   *
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
          caze  = null; break;
      }
    }
    return caze;
  }

  /**
   * 获取关联的关联对象
   *
   * 注意:
   * 与 getJoin 不同,
   * 不存在的关联会自动创建,
   * 不会设置关联关系和类型.
   *
   * @param name
   * @return
   * @throws CruxException
   */
  public FetchCase gotJoin(String... name)
    throws CruxException
  {
    FetchCase caze = this;
    for (String n : name)
    {
      FetchCase c = caze.getJoin(n);
      if (null != c) {
          caze  = c;
      } else {
          caze  = caze.join(n).by(NONE);
      }
    }
    return caze;
  }

  /**
   * 获取关联的上级对象
   * @return
   */
  public FetchCase getSup()
  {
    FetchCase  caze = joinSup;
    if (caze.joinSet.contains(this)) {
        return caze ;
    }
    return null;
  }

  /**
   * 获取关联的顶级对象
   * @return
   */
  public FetchCase getTop()
  {
    FetchCase  caxe ;
    FetchCase  caze = this;
    do {
        caxe = caze.getSup();
    }
    while (caxe != null);
    return caze;
  }

  /**
   * 获取查询用例名称
   * 同时也是关联名称
   * @return
   */
  public String getName()
  {
    String   n  = name;
    if (n == null || n.length() == 0) {
        n  = tableName;
    }
    return n;
  }

  //** 探查 **/

  /**
   * 是否有设置查询字段
   * @return
   */
  public boolean hasField()
  {
    return this.fields.length() > 0;
  }

  /**
   * 是否有设置查询条件
   * @return
   */
  public boolean hasWhere()
  {
    return this.wheres.length() > 0;
  }

  /**
   * 是否有设置分组
   * @return
   */
  public boolean hasGroup()
  {
    return this.groups.length() > 0;
  }

  /**
   * 是否有设置过滤条件
   * @return
   */
  public boolean hasHavin()
  {
    return this.havins.length() > 0;
  }

  /**
   * 是否有设置排序
   * @return
   */
  public boolean hasOrder()
  {
    return this.orders.length() > 0;
  }

  //** 对象 **/

  /**
   * 转换为字符串
   * @return 合并了SQL的参数和分页
   */
  @Override
  public String toString()
  {
    Link  db  = link;
    try
    {
      if (db != null)
      {
        return new Lump(db, getSQL(), getStart(), getLimit(), getParams()).toString();
      }
      else
      {
        StringBuilder sb = getSQLText();
        List ps = getParamsList();
        Link.checkSQLParams (sb , ps);
        Link.mergeSQLParams (sb , ps);
        if (start != 0 || limit != 0)
        {
          sb.append(" /* LIMIT ")
            .append(start)
            .append( "," )
            .append(limit)
            .append(" */");
        }
        return sb.toString();
      }
    }
    catch (CruxException ex)
    {
      throw ex.toExemption();
    }
  }

  /**
   * 深度克隆用例
   * 关联用例一并克隆
   * @return 全新的用例对象
   */
  @Override
  public FetchCase clone()
  {
    return new FetchCase(this, true );
  }

  /**
   * 浅层拷贝用例
   * 关联用例全部抛弃
   * @return 全新的用例对象
   */
  public FetchCase klone()
  {
    return new FetchCase(this, false);
  }

  //** 串联 **/

  public static class Doer {

    protected final FetchCase that;

    public Doer(FetchCase caze) {
      that =  caze;
    }

    /**
     * 获取数据连接, 未指定时返空
     * @return
     */
    protected Link getLink() {
      return  that.getLink();
    }

    /**
     * 获取数据连接, 未指定抛异常
     * @return
     * @throws CruxException
     */
    protected Link gotLink() throws CruxException {
      return  that.gotLink();
    }

    /**
     * 查询并获取记录迭代
     * 注意: 完全重写需区分选项是否有设置字符模式
     * @return
     * @throws CruxException
     */
    public Loop select() throws CruxException {
      Link db = that.gotLink();

      Loop rs = db.query(that.getSQL(), that.getStart(), that.getLimit(), that.getParams());
      rs . inStringMode (that.getOption("STRING_MODE", false));

      return rs;
    }

    /**
     * 删除全部匹配的记录
     * 注意: 考虑到 SQL 兼容性, 忽略了 join 的条件, 有 xx.n 的字段条件会报 SQL 错误
     * @return
     * @throws CruxException
     */
    public int delete() throws CruxException {
      Link db = that.gotLink();

      return db.delete(that.tableName, /**/ that.getWhile(), that.getWheres());
    }

    /**
     * 更新全部匹配的数据
     * 注意: 考虑到 SQL 兼容性, 忽略了 join 的条件, 有 xx.n 的字段条件会报 SQL 错误
     * @param dat
     * @return
     * @throws CruxException
     */
    public int update(Map<String, Object> dat) throws CruxException {
      Link db = that.gotLink();

      return db.update(that.tableName, dat, that.getWhile(), that.getWheres());
    }

    /**
     * 插入当前指定的数据
     * @param dat
     * @return
     * @throws CruxException
     */
    public int insert(Map<String, Object> dat) throws CruxException {
      Link db = that.gotLink();

      return db.insert(that.tableName, dat);
    }

  }

  protected Doer doer;
  protected Link link;

  /**
   * 指定操作方法
   * @param dr
   * @return
   */
  public FetchCase use(Doer dr)
  {
    doer = dr;
    return this;
  }

  /**
   * 指定操作的库
   * @param db
   * @return
   */
  public FetchCase use(Link db)
  {
    link = db;
    return this;
  }

  /**
   * 获取操作方法
   * @return
   */
  public Doer getDoer()
  {
    return doer;
  }

  /**
   * 获取操作的库
   * @return
   */
  public Link getLink()
  {
    return link;
  }

  private Doer gotDoer() {
    if (doer == null) {
        doer  = new Doer(this);
    }
    return doer;
  }

  private Link gotLink() throws CruxException {
    if (link == null) {
        throw new CruxException(1163);
    }
    return link;
  }

  /**
   * 查询并获取全部结果
   * @return
   * @throws CruxException
   */
  public List getAll() throws CruxException {
    List<Map> ra = new ArrayList( );
         Map  ro ;

    try (Loop rs = select()) {
    while ( ( ro = rs.next() ) != null ) {
       ra.add(ro);
    } }

    return  ra;
  }

  /**
   * 查询并获取单个结果
   * @return
   * @throws CruxException
   */
  public Map  getOne() throws CruxException {
    this.limit(1);

    try (Loop rs = select()) {
         Map  ro = rs.next();
       return ro != null ? ro : new HashMap();
    }
  }

  /**
   * 查询并获取记录迭代
   * @return
   * @throws CruxException
   */
  public Loop select() throws CruxException {
    return gotDoer().select();
  }

  /**
   * 删除全部匹配的记录
   * 注意: 考虑到 SQL 兼容性, 忽略了 join 的条件, 有 xx.n 的字段条件会报 SQL 错误
   * @return
   * @throws CruxException
   */
  public int delete() throws CruxException {
    return gotDoer().delete();
  }

  /**
   * 更新全部匹配的数据
   * 注意: 考虑到 SQL 兼容性, 忽略了 join 的条件, 有 xx.n 的字段条件会报 SQL 错误
   * @param dat
   * @return
   * @throws CruxException
   */
  public int update(Map<String, Object> dat) throws CruxException {
    return gotDoer().update(dat);
  }

  /**
   * 插入当前指定的数据
   * 其实与 FetchCase 无关, 因为 insert 是没有 where 等语句的
   * 但为保障支持的语句完整让 FetchCase 看着像 ORM 还是放一个
   * @param dat
   * @return
   * @throws CruxException
   */
  public int insert(Map<String, Object> dat) throws CruxException {
    return gotDoer().insert(dat);
  }

}
