package app.hongs.db;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.CoreConfig;
import app.hongs.HongsException;
import app.hongs.db.util.FetchCase;
import app.hongs.db.util.FetchPage;
import app.hongs.dh.IEntity;
import app.hongs.util.Synt;
import app.hongs.util.Tool;
import java.util.Iterator;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 基础模型
 *
 * <p>
 * 当您需要使用 create,add,update,put,delete,del 等时请确保表有主键.<br/>
 * 基础动作方法: retrieve,create,update,delete
 * 扩展动作方法: unique,exists
 * 通常它们被动作类直接调用;
 * 基础模型方法: get,add,put,del
 * 一般改写只需覆盖它们即可;
 * filter, permit 分别用于获取和更改数据等常规操作时进行过滤,
 * permit 默认调用 filter 来实现的, 可覆盖它来做资源过滤操作.<br/>
 * retrieve 可使用查询参数:
 * <code>
 * ?pn=1&rn=10&f1=123&f2.-gt=456&wd=a+b&ob=-f1+f2&rb=id+f1+f2
 * </code>
 * 详见 filter 方法说明
 </p>
 *
 * <h3>异常代码:</h3>
 * <pre>
 * 区间: 0x1090~0x109f
 * 0x1091 创建时不得含有id
 * 0x1092 更新时必须含有id
 * 0x1093 删除时必须含有id
 * 0x1094 获取时必须含有id
 * 0x1096 无权更新该资源
 * 0x1097 无权删除该资源
 * 0x1098 无权获取该资源
 * 0x109a 参数n和v不能为空(检查存在)
 * 0x109b 指定的字段不存在(检查存在)
 * 0x109c 不支持的运算符: $0
 * </pre>
 *
 * @author Hongs
 */
public class Model
implements IEntity
{

  /**
   * 所属库对象
   */
  public DB db;

  /**
   * 所属表对象
   */
  public Table table;

  /**
   * 可搜索的字段
   */
  public String[] findCols = new String[] {} ;

  /**
   * 可列举的字段
   */
  public String[] listCols = new String[] {} ;

  /**
   * 可排序的字段
   */
  public String[] sortCols = new String[] {} ;

  /**
   * 不查询的字段
   */
  protected static final Set<String> funcKeys;
  static {
    funcKeys = new HashSet( );
    funcKeys.add(Cnst.PN_KEY);
    funcKeys.add(Cnst.GN_KEY);
    funcKeys.add(Cnst.RN_KEY);
    funcKeys.add(Cnst.OB_KEY);
    funcKeys.add(Cnst.RB_KEY);
    funcKeys.add(Cnst.WD_KEY);
    funcKeys.add(Cnst.OR_KEY);
    funcKeys.add(Cnst.AR_KEY);
  }

  /**
   * 构造方法
   *
   * 需指定该模型对应的表对象.
   * 如传递的 id,wd,pn,gn,rn,rb,ob 等参数名不同,
   * 可在构造时分别指定;
   * 请指定被搜索的字段.
   *
   * @param table
   * @throws app.hongs.HongsException
   */
  public Model(Table table)
    throws HongsException
  {
    this.table = table;
    this.db = table.db;

    String cs;
    cs = table.getField("findCols");
    if (cs != null) this.findCols = cs.split(",");
    cs = table.getField("listCols");
    if (cs != null) this.listCols = cs.split(",");
    cs = table.getField("sortCols");
    if (cs != null) this.sortCols = cs.split(",");
  }

  //** 标准动作方法 **/

  /**
   * 综合提取方法
   * @param rd
   * @return
   * @throws HongsException
   */
  @Override
  public Map retrieve(Map rd)
    throws HongsException
  {
    return this.retrieve(rd, null);
  }

  /**
   * 综合提取方法
   * @param rd
   * @param caze
   * @return
   * @throws HongsException
   */
  public Map retrieve(Map rd, FetchCase caze)
    throws HongsException
  {
    Object id = rd.get(this.table.primaryKey);
    if (id == null || id instanceof Map || id instanceof Collection) {
      return  this.getList(rd, caze);
    } else if (!"".equals (id)) {
      return  this.getInfo(rd, caze);
    } else {
      return  new HashMap (  );
    }
  }

  /**
   * 创建记录
   *
   * @param rd
   * @return 记录ID
   * @throws HongsException
   */
  @Override
  public Map create(Map rd)
    throws HongsException
  {
    String id = add(rd);
    if (null != listCols
    ||   0   != listCols.length) {
        Map sd = new LinkedHashMap();
        sd.put(table.primaryKey, id);
        for(String fn: listCols) {
        if ( ! fn.contains(".")) {
            sd.put(fn, rd.get( fn ));
        }
        }
        return sd;
    } else {
        rd.put(table.primaryKey, id);
        return rd;
    }
  }

  /**
   * 更新记录
   *
   * @param rd
   * @return 更新条数
   * @throws app.hongs.HongsException
   */
  @Override
  public int update(Map rd)
    throws HongsException
  {
    return this.update(rd, null);
  }

  /**
   * 更新记录
   *
   * @param rd
   * @param caze
   * @return 更新条数
   * @throws app.hongs.HongsException
   */
  public int update(Map rd, FetchCase caze)
    throws HongsException
  {
    Object idz = rd.get(Cnst.ID_KEY);
    if (idz == null) {
        idz =  rd.get(table.primaryKey);
    }
    if (idz == null) {
        return this.put(null, null);
    }

    Set<String> ids = new LinkedHashSet();
    if (idz instanceof Collection ) {
        ids.addAll((Collection)idz);
    } else {
        ids.add   ( idz.toString());
    }

    Map dat = new HashMap(rd);
    dat.remove(this.table.primaryKey);

    // 检查是否可更新
    Map wh  = Synt.declare(rd.get(Cnst.WH_KEY) , new HashMap( ));
    FetchCase fc = caze != null ? caze.clone() : new FetchCase();
    fc.setOption("MODEL_METHOD" , "update");
    for (String id : ids)
    {
      if (!this.permit(fc, wh, id))
      {
        throw new HongsException (0x1097, "Can not update for id '"+id+"'");
      }

      this.put( id , dat);
    }

    return ids.size();
  }

  /**
   * 删除记录
   *
   * @param rd
   * @return 删除条数
   * @throws app.hongs.HongsException
   */
  @Override
  public int delete(Map rd)
    throws HongsException
  {
    return this.delete(rd, null);
  }

  /**
   * 删除记录
   *
   * @param rd
   * @param caze
   * @return 删除条数
   * @throws app.hongs.HongsException
   */
  public int delete(Map rd, FetchCase caze)
    throws HongsException
  {
    Object idz = rd.get ( Cnst.ID_KEY );
    if (idz == null) {
        idz =  rd.get(table.primaryKey);
    }
    if (idz == null) {
        return this.del(null, null);
    }

    Set<String> ids = new LinkedHashSet();
    if (idz instanceof Collection ) {
        ids.addAll((Collection)idz);
    } else {
        ids.add   ( idz.toString());
    }

    // 检查是否可删除
    Map wh  = Synt.declare(rd.get(Cnst.WH_KEY) , new HashMap( ));
    FetchCase fc = caze != null ? caze.clone() : new FetchCase();
    fc.setOption("MODEL_METHOD" , "delete");
    for (String id : ids)
    {
      if (!this.permit(fc, wh, id))
      {
        throw new HongsException (0x1097, "Can not delete for id '"+id+"'");
      }

      this.del( id  );
    }

    return ids.size();
  }

  //** 扩展动作方法 **/

  public boolean unique(Map rd)
    throws HongsException
  {
    return !exists(rd);
  }

  public boolean unique(Map rd, FetchCase caze)
    throws HongsException
  {
    return !exists(rd, caze);
  }

  /**
   * 检查是否存在
   *
   * @param rd
   * @return 存在为true, 反之为false
   * @throws app.hongs.HongsException
   */
  public boolean exists(Map rd)
    throws HongsException
  {
    return  exists(rd, null);
  }

  /**
   * 检查是否存在
   *
   * @param rd
   * @param caze
   * @return 存在为true, 反之为false
   * @throws app.hongs.HongsException
   */
  public boolean exists(Map rd, FetchCase caze)
    throws HongsException
  {
    if (rd == null)
    {
      rd = new HashMap();
    }
    if (caze == null)
    {
      caze = new FetchCase();
    }

    // 默认不关联
    if (!caze.hasOption("ASSOCS")
    &&  !caze.hasOption("ASSOC_TYPES")
    &&  !caze.hasOption("ASSOC_JOINS"))
    {
      caze.setOption("ASSOCS", new HashSet());
    }

    // 是否缺少n或v参数
    if (!rd.containsKey("n") || !rd.containsKey("v"))
    {
      throw new HongsException(0x109a, "Param n or v can not be empty");
    }

    String n = (String) rd.get("n");
    String v = (String) rd.get("v");

    Map columns = this.table.getFields();

    // 是否缺少n对应的字段
    if (!columns.containsKey(n))
    {
      throw new HongsException(0x109b, "Column " + n + " is not exists");
    }

    caze.where(".`"+n+"` = ?", v);

    Iterator it = rd.entrySet().iterator();
    while (it.hasNext())
    {
      Map.Entry entry = (Map.Entry)it.next();
      String field = (String) entry.getKey();
      String value = (String) entry.getValue();

      if (columns.containsKey(field))
      {
        if (field.equals( this.table.primaryKey) || field.equals(Cnst.ID_KEY))
        {
          caze.where(".`"+this.table.primaryKey+"` != ?", value);
        }
        else
        {
          caze.where(".`"+field+"` = ?", value);
        }
      }
    }

    Map row = this.table.fetchLess(caze);
    return !row.isEmpty();
  }

  //** 标准模型方法 **/

  /**
   * 保存记录
   * 有 id 则修改, 无 id 则添加
   * @param rd
   * @return
   * @throws HongsException
   */
  public String set(Map rd)
    throws HongsException
  {
    String id = (String) rd.get(this.table.primaryKey);
    if (id == null || id.length() == 0)
    {
      id = this.add(rd);
    }
    else
    {
      this.put(id , rd);
    }
    return id;
  }

  /**
   * 添加记录

 由于获取自增 id 在各数据库中的获取方式不同
 故如需要自增 id 请自行扩展本类并重写此方法
 配置文件中用 model 来指定新的模型类

 MySQL,SQLite add 方法主体改造:
    rd.remove(this.table.primaryKey );
    // 存入主数据
    this.table.insert          ( rd );
    // 查询自增ID, MySQL: last_insert_id() SQLite: last_insert_rowid()
    Map  rd = this.db.fetchOne ("SELECT last_insert_id() AS id");
    id = rd.get("id").toString (    );
    rd.put(this.table.primaryKey, id);
    // 存入子数据
    this.table.insertSubValues ( rd );

 或对 DB,Table,Model 同步改造, 使用 PreparedStatement.getGeneratedKeys 获取
   *
   * @param rd
   * @return 记录ID
   * @throws app.hongs.HongsException
   */
  public String add(Map rd)
    throws HongsException
  {
    String id = Synt.declare(rd.get(Cnst.ID_KEY), String.class );
    if (id != null && id.length() != 0)
    {
      throw new HongsException (0x1091, "Add can not have a id");
    }

    id = Core.getUniqueId();
    rd.put(this.table.primaryKey, id);

    // 存入主数据
    this.table.insert/* new */ ( rd );

    // 存入子数据
    this.table.insertSubValues ( rd );

    return id;
  }

  /**
   * 更新记录
   *
   * @param rd
   * @param id
   * @return 更新条数
   * @throws app.hongs.HongsException
   */
  public int put(String id, Map rd)
    throws HongsException
  {
    return this.put(id, rd, null);
  }

  /**
   * 更新记录
   *
   * @param rd
   * @param id
   * @param caze
   * @return 更新条数
   * @throws app.hongs.HongsException
   */
  public int put(String id, Map rd, FetchCase caze)
    throws HongsException
  {
    if (id == null || id.length() == 0)
    {
      throw new HongsException (0x1092, "ID can not be empty for put");
    }

    // 更新主数据
    rd.remove(this.table.primaryKey );
    int an = this.table.update ( rd , "`"+this.table.primaryKey+"` = ?", id);

    // 更新子数据
    rd.put(this.table.primaryKey, id);
    this.table.insertSubValues ( rd );

    return an;
  }

  /**
   * 删除指定记录
   *
   * @param id
   * @return 删除条数
   * @throws app.hongs.HongsException
   */
  public int del(String id)
    throws HongsException
  {
    return this.del(id, null);
  }

  /**
   * 删除指定记录
   *
   * 为避免逻辑复杂混乱
   * 如需重写删除方法,
   * 请总是重写该方法.
   *
   * @param id
   * @param caze
   * @return 删除条数
   * @throws app.hongs.HongsException
   */
  public int del(String id, FetchCase caze)
    throws HongsException
  {
    if (id == null || id.length() == 0)
    {
      throw new HongsException (0x1093, "ID can not be empty for del");
    }

    // 删除主数据
    int an = this.table.delete ("`"+this.table.primaryKey+"` = ?", id);

    // 删除子数据
    this.table.deleteSubValues ( id );

    return an;
  }

  public Map get(String id)
    throws HongsException
  {
    return this.get(id, null);
  }

  public Map get(String id, FetchCase caze)
    throws HongsException
  {
    if (id == null || id.length() == 0)
    {
      throw new HongsException(0x1094, "ID can not be empty for get");
    }

    Map rd = new HashMap();
    rd.put(table.primaryKey, id);

    // 调用filter进行过滤
    caze = caze != null ? caze.clone() : new FetchCase();
    caze.setOption("MODEL_METHOD", "get");
    this.filter(caze , rd);

    return this.table.fetchLess(caze);
  }

  public Map  getOne(Map rd)
    throws HongsException
  {
    return this.getOne(rd, null);
  }

  public Map  getOne(Map rd, FetchCase caze)
    throws HongsException
  {
    if (rd == null) {
        rd = new HashMap();
    }

    // 调用filter进行过滤
    caze = caze != null ? caze.clone() : new FetchCase();
    caze.setOption("MODEL_METHOD", "getOne");
    this.filter(caze , rd);

    return this.table.fetchLess(caze);
  }

  public List getAll(Map rd)
    throws HongsException
  {
    return this.getAll(rd, null);
  }

  public List getAll(Map rd, FetchCase caze)
    throws HongsException
  {
    if (rd == null) {
        rd = new HashMap();
    }

    // 调用filter进行过滤
    caze = caze != null ? caze.clone() : new FetchCase();
    caze.setOption("MODEL_METHOD", "getAll");
    this.filter(caze , rd);

    return this.table.fetchMore(caze);
  }

  /**
   * 获取信息(无查询结构)
   *
   * @param rd
   * @return 记录信息
   * @throws app.hongs.HongsException
   */
  public Map getInfo(Map rd)
    throws HongsException
  {
    return this.getInfo(rd, null);
  }

  /**
   * 获取信息
   *
   * @param rd
   * @param caze
   * @return 记录信息
   * @throws app.hongs.HongsException
   */
  public Map getInfo(Map rd, FetchCase caze)
    throws HongsException
  {
    if (rd == null)
    {
      rd = new HashMap();
    }
    if (caze == null)
    {
      caze = new FetchCase();
    }

    if (rd.containsKey(Cnst.ID_KEY))
    {
      rd.put(table.primaryKey, rd.get(Cnst.ID_KEY));
    }

    caze.setOption("MODEL_METHOD", "getInfo");
    this.filter(caze, rd);

    Map info = table.fetchLess(caze);
    Map data = new HashMap();
    data.put( "info", info );

    return data;
  }

  /**
   * 获取分页(无查询结构)
   *
   * 为空则page.errno为1, 页码超出则page.errno为2
   *
   * 含分页信息
   *
   * @param rd
   * @return 单页列表
   * @throws app.hongs.HongsException
   */
  public Map getList(Map rd)
    throws HongsException
  {
    return this.getList(rd, null);
  }

  /**
   * 获取分页
   *
   * 为空则page.errno为1, 页码超出则page.errno为2
   * 页码等于 0 则不要列表数据
   * 行数小于 0 则不要分页信息
   * 行数等于 0 则不要使用分页
   * 页码小于 0 则逆向倒数获取
   *
   * @param rd
   * @param caze
   * @return 单页列表
   * @throws app.hongs.HongsException
   */
  public Map getList(Map rd, FetchCase caze)
    throws HongsException
  {
    if (rd == null)
    {
      rd = new HashMap();
    }
    if (caze == null)
    {
      caze = new FetchCase();
    }

    if (rd.containsKey(Cnst.ID_KEY))
    {
      rd.put(table.primaryKey, rd.get(Cnst.ID_KEY));
    }

    caze.setOption("MODEL_METHOD", "getList");
    this.filter(caze, rd);

    // 获取页码, 默认为第一页
    int page = 1;
    if (rd.containsKey(Cnst.PN_KEY))
    {
      page = Synt.declare(rd.get(Cnst.PN_KEY), 1);
    }

    // 获取分页, 默认查总页数
    int pags = 0;
    if (rd.containsKey(Cnst.GN_KEY))
    {
      pags = Synt.declare(rd.get(Cnst.GN_KEY), 0);
    }

    // 获取行数, 默认依从配置
    int rows;
    if (rd.containsKey(Cnst.RN_KEY))
    {
      rows = Synt.declare(rd.get(Cnst.RN_KEY), 0);
    }
    else
    {
      rows = CoreConfig.getInstance().getProperty("fore.rows.per.page", Cnst.RN_DEF);
    }

    Map data = new HashMap();

    if (rows != 0)
    {
      caze.from (table.tableName , table.name );
      FetchPage fp = new FetchPage(caze, table);
      fp.setPage(page != 0 ? page : 1);
      fp.setPags(Math.abs(pags));
      fp.setRows(Math.abs(rows));

      // 页码等于 0 则不要列表数据
      if (page != 0)
      {
        List list = fp.getList();
        data.put( "list", list );
      }

      // 行数小于 0 则不要分页信息
      if (rows  > 0)
      {
        Map  info = fp.getPage();
        data.put( "page", info );
      }
    }
    else
    {
      // 行数等于 0 则不要使用分页
        List list = table.fetchMore(caze);
        data.put( "list", list );
    }

    return data;
  }

  //** 辅助过滤方法 **/

  /**
   * "获取"过滤
   *
   * <pre>
   * 作用于getPage,getList上
   *
   * 如需添加过滤条件, 请重写此方法;
   * 注意: 此处需要类似引用参数, 故调用前请务必实例化req和caze;
   * 默认仅关联join类型为LEFT,INNER和link类型为BLS_TO,HAS_ONE的表,
   * 如需指定关联的表请设置FetchCase的option: ASSOCS,
   * 如需指定关联方式请设置FetchCase的option: ASSOC_JOINS, ASSOC_TYEPS
   *
   * 设计目标:
   * 1) 按照cs参数设置查询字段;
   *    限定字段列表: rb=a+b+c或rb=-a+x.b, -表示排除该字段;
   * 2) 按照ob参数设置排序方式,
   *    多个字段排序: ob=a+b+c或ob=-a+b+c, -表示该字段逆序;
   * 3) 按照wd参数设置模糊查询,
   *    多关键词搜索: wd=x+y+z;
   *    指定字段搜索: wd.a=x或wd.a.b=y, 同样适用上面的规则,
   *    a.b为搜索关联表, 但需注意: a,a.b必须在findCols中有指定;
   * 4) 如果有字段名相同的参数则获取与之对应的记录,
   *    可以在字段名后跟.加上-gt,-lt,-ge,-le,-ne分别表示&gt;,&lt;,&ge;,&le;,&ne;
   * 5) 如果有子表.字段名相同的参数则获取与之对应的记录,
   *    可以在子表.字段名后跟.加上-gt,-lt,-ge,-le,-ne分别表示&gt;,&lt;,&ge;,&le;,&ne;
   * 注: "+" 在URL中表示空格; 以上设计目录均已实现; 以上1/2/3中的参数名可统一设置或单独指定;
   * </pre>
   *
   * @param caze
   * @param rd
   * @throws app.hongs.HongsException
   */
  protected void filter(FetchCase caze, Map rd)
    throws HongsException
  {
    /**
     * 如果没指定查询的字
     * 默认只关联 BLS_TO,HAS_ONE 的表(仅能关联一个)
     * 默认只连接 LEFT  ,INNER   的表(必须满足左表)
     */
    if (! rd.containsKey(Cnst.RB_KEY)
    && ("getAll" .equals(rd.get("MODEL_METHOD"))
    ||  "getList".equals(rd.get("MODEL_METHOD"))
    ))
    {
      if (null == caze.getOption("ASSOC_JOINS"))
      {
        Set types = new HashSet();
        types.add( "LEFT"  );
        types.add( "INNER" );
        caze.setOption("ASSOC_JOINS", types);
      }
      if (null == caze.getOption("ASSOC_TYPES"))
      {
        Set types = new HashSet();
        types.add("BLS_TO" );
        types.add("HAS_ONE");
        caze.setOption("ASSOC_TYPES", types);
      }
    }

    if (rd.isEmpty())
    {
      return;
    }

    /**
     * 依据设计规则, 解析请求参数, 转为查询结构
     */

    List finds = Arrays.asList(this.findCols);
    Map fields = this.table.getFields (  );
    Map relats = this.table.relats != null
               ? this.table.relats
               : new HashMap( );
    Object value;

    // 字段
    value = rd.get(Cnst.RB_KEY);
    if (value != null)
    {
      this.colsFilter(caze, value, fields);
    }

    // 排序
    value = rd.get(Cnst.OB_KEY);
    if (value != null)
    {
      this.sortFilter(caze, value, fields);
    }

    Iterator it = rd.entrySet().iterator();
    while (it.hasNext())
    {
      Map.Entry et = (Map.Entry) it.next();
      String   key = (String) et.getKey( );
             value = et.getValue( );

      if (key == null || funcKeys.contains(key))
      {
        continue;
      }

      // 可搜索字段 (用 !eq 仍可精确匹配)
      if (finds.contains(key) && !(value instanceof Map))
      {
        this.findFilter(caze, value, new String[ ] {key});
      }

      // 当前表字段
      else if (fields.containsKey( key ))
      {
        this.mkeyFilter(caze, value, key);
      }

      // 关联表字段
      else if (relats.containsKey( key ))
      {
        this.skeyFilter(caze, value, key);
      }
    }

    // 或
    value = rd.get(Cnst.OR_KEY);
    if (value != null)
    {
      this.packFilter(caze, value, "OR" );
    }

    // 并或
    value = rd.get(Cnst.AR_KEY);
    if (value != null)
    {
      this.packFilter(caze, value, "AND");
    }

    // 搜索
    value = rd.get(Cnst.WD_KEY);
    if (value != null)
    {
      this.findFilter(caze, value, findCols);
    }
  }

  /**
   * "变更"过滤
   * 默认调用 filter 进行判断, 即能操作的一定是能看到的
   * @param caze 条件
   * @param wh   约束
   * @param id 主键值
   * @return
   * @throws HongsException
   */
  protected boolean permit(FetchCase caze, Map wh, String id)
    throws HongsException
  {
    if (! caze.hasOption("ASSOCS") && ! caze.hasJoin() )
    {
      caze.setOption("ASSOCS", new HashSet());
    }
    caze.setSelect(".`"+this.table.primaryKey+"`")
            .where(".`"+this.table.primaryKey+"`=?", id);
    this.filter(caze, wh);
    return ! this.table.fetchLess(caze).isEmpty( );
  }

  /**
   * 字段过滤(被caseFileter调用)
   * 根据请求的字段设置查询及判断需要关联的表
   * @param caze
   * @param val
   * @param columns
   * @throws HongsException
   */
  protected void colsFilter(FetchCase caze, Object val, Map columns)
    throws HongsException
  {
    if (caze.hasSelect( ))
    {
      return;
    }

    Set<String> cols  = Synt.asTerms(val);
    if (null == cols || cols.isEmpty( ) )
    {
      return;
    }

    Set<String> tns = (Set<String>)caze.getOption("ASSOCS");
    if (tns == null)
    {
        tns  = new HashSet();
        caze.setOption("ASSOCS", tns);
    }

    Map<String, Set<String>> colsBuf = new HashMap();
    Map<String, Set<String>> colsExc = new HashMap();

    for (String col : cols)
    {
      String  tbl = "";
      col = col.trim();
      boolean exc = col.startsWith("-");
      if(exc) col = col.substring ( 1 );

      int pos  = col.indexOf(".");
      if (pos != -1)
      {
        tbl = col.substring(0 , pos);
        col = col.substring(pos + 1);
      }

      Set set;
      if (exc)
      {
        set = colsExc.get(tbl);
        if (set == null)
        {
          set = new LinkedHashSet();
          colsExc.put(tbl,set);
        }
      }
      else
      {
        set = colsBuf.get(tbl);
        if (set == null)
        {
          set = new LinkedHashSet();
          colsBuf.put(tbl,set);
        }
      }
      set.add(col);
    }

    // 查询的字段
    for ( Map.Entry  et : colsBuf.entrySet())
    {
      Map          cols2;
      FetchCase    caze2;
      Set<String>  colz = (Set) et.getValue();
      String         tn = (String)et.getKey();

      if (!"".equals(tn))
      {
        Map          tc;
        Table        tb;
        String       tx;
        String[]     ts;

        tc = this.table.getAssoc(tn);
        if (tc == null)
        {
          continue;
        }

        tx = Table.getAssocName (tc);
        ts = Table.getAssocPath (tc);
        tb =  this.db.getTable  (tx);
        cols2 =    tb.getFields (  );
        tns.addAll(Arrays.asList(ts));
        tns.add   (/* current */ tn );
        caze2 = caze.gotJoin(ts).gotJoin(tn);

        // 取交集
        Set<String> colx = new LinkedHashSet( cols2.keySet() );
        colx.retainAll(colz);

        // JOIN 的必须取别名
        Object jn = tc.get("join");
        if (jn != null && !"".equals(jn)) {
            for (String col : colx) {
                caze2.select(".`"+col+"` AS `"+tn+"."+col+"`");
            }
        } else {
            for (String col : colx) {
                caze2.select(".`"+col+"`");
            }
        }
      }
      else
      {
        cols2 = columns;
        caze2 = caze;

        Set<String> colx = new LinkedHashSet( cols2.keySet() );
        colx.retainAll(colz);

        for (String col : colx) {
            caze2.select(".`"+col+"`");
        }
      }
    }

    // 排除的字段
    for ( Map.Entry  et : colsExc.entrySet())
    {
      Map          cols2;
      FetchCase    caze2;
      Set<String>  colz = (Set) et.getValue();
      String         tn = (String)et.getKey();

      // 如果表已有指定查询字段
      // 则不可再指定排除字段了
      if (colsBuf.containsKey(tn))
      {
        continue;
      }

      if (!"".equals(tn))
      {
        Map          tc;
        Table        tb;
        String       tx;
        String[]     ts;

        tc = this.table.getAssoc(tn);
        if (tc == null)
        {
          continue;
        }

        tx = Table.getAssocName (tc);
        ts = Table.getAssocPath (tc);
        tb =  this.db.getTable  (tx);
        cols2 =    tb.getFields (  );
        tns.addAll(Arrays.asList(ts));
        tns.add   (/* current */ tn );
        caze2 = caze.gotJoin(ts).gotJoin(tn);

        // 取差集
        Set<String> colx = new LinkedHashSet( cols2.keySet() );
        colx.removeAll(colz);

        // JOIN 的必须取别名
        Object jn = tc.get("join");
        if (jn != null && !"".equals(jn)) {
            for (String col : colx) {
                caze2.select(".`"+col+"` AS `"+tn+"."+col+"`");
            }
        } else {
            for (String col : colx) {
                caze2.select(".`"+col+"`");
            }
        }
      }
      else
      {
        cols2 = columns;
        caze2 = caze;

        Set<String> colx = new LinkedHashSet( cols2.keySet() );
        colx.removeAll(colz);

        for (String col : colx) {
            caze2.select(".`"+col+"`");
        }
      }
    }
  }

  /**
   * 排序过滤(被caseFileter调用)
   * 如果字段有前缀“-”则该字段为逆序
   * @param caze
   * @param val
   * @param columns
   * @throws HongsException
   */
  protected void sortFilter(FetchCase caze, Object val, Map columns)
    throws HongsException
  {
    if (caze.hasOrderBy())
    {
      return;
    }

    Set<String> cols  = Synt.asTerms(val);
    if (null == cols || cols.isEmpty( ) )
    {
      return;
    }

    Set<String> tns = (Set<String>)caze.getOption("ASSOCS");
    if (tns == null)
    {
        tns  = new HashSet();
//      caze.setOption("ASSOCS", tns);
    }

    // 检查排序限制
    Set obs  = null;
    if ( sortCols != null && sortCols.length != 0 )
    {
        obs  = new HashSet(Arrays.asList(sortCols));
    }

    for (String col : cols)
    {
      col = col.trim();
      boolean esc = col.startsWith("-");
      if(esc) col = col.substring ( 1 );

      if(obs != null && !obs.contains(col))
      {
        continue;
      }

      int pos  = col.indexOf(".");
      if (pos != -1)
      {
        String tn = col.substring(0,  pos);
        String fn = col.substring(pos + 1);

        Map          tc;
        Map          cs;
        Table        tb;
        String       tx;
        String[]     ts;

        tc = this.table.getAssoc(tn);
        if (tc == null)
        {
          continue;
        }

        tx = Table.getAssocName (tc);
        ts = Table.getAssocPath (tc);
        tb =  this.db.getTable  (tx);
        cs =       tb.getFields (  );
        if (  ! cs.containsKey  (fn))
        {
          continue;
        }

        tns.addAll(Arrays.asList(ts));
        tns.add   (/* current */ tn );
        FetchCase cace = caze.gotJoin(ts).gotJoin(tn);

        cace.orderBy(".`"+fn +"`"+(esc ? " DESC":""));
      }
      else
      {
        if ( ! columns.containsKey(col))
        {
          continue;
        }

        caze.orderBy(".`"+col+"`"+(esc ? " DESC":""));
      }
    }
  }

  /**
   * 搜索过滤(被caseFileter调用)
   * @param caze
   * @param val
   * @param keys
   */
  protected void findFilter(FetchCase caze, Object val, String[] keys)
  {
    if (keys == null || keys.length == 0)
    {
      return;
    }

    /**
     * 空串查全部, 故不用理会
     */
    if (val  == null || "".equals( val ))
    {
      return;
    }

    /**
     * 也可指定只匹配其中一个可搜索字段
     */
    if (val instanceof Map)
    {
        List ks = Arrays.asList(keys);
        Map  m1 = (Map) val;
        for (Object o1 : m1.entrySet()) {
            Map.Entry e1 = (Map.Entry) o1;
            Object    v1 = e1.getValue( );
            String    k1 = e1.getKey().toString();

            if (v1 instanceof Map) {
                Map m2 = (Map) v1;
                for (Object o2 : m2.entrySet()) {
                    Map.Entry e2 = (Map.Entry) o2;
                    String    v2 = e2.getValue( ).toString();
                    String    k2 = k1 + "." + e2.getKey().toString();

                    if (ks.contains(k2)) {
                        this.findFilter(caze, v2, new String[] {k2});
                    }
                }
            } else {
                    if (ks.contains(k1)) {
                        this.findFilter(caze, v1, new String[] {k1});
                    }
            }
        }
        return;
    }

    Set<String> vals  = Synt.asWords(val);
    if (null == vals || vals.isEmpty( ) )
    {
      return;
    }

    StringBuilder sb = new StringBuilder();
    Object[]      pa = new String[keys.length * vals.size()];
    int           pi = 0;

    sb.append("(");
    for (String key : keys)
    {
      if (key.indexOf('.') != -1)
      {
        String[] b = key.split("\\." , 2);
        key = "`"+ b[0] +"`.`"+ b[1] +"`";
      }
      else
      {
        key = ".`" + key + "`";
      }

      sb.append("(");
      for (String txt : vals)
      {
        /**
         * 符号 "%_[]" 在 SQL LIKE 中有特殊意义,
         * 需要对这些符号进行转义.
         */
        txt = "%" + Tool.escape(txt, "%_[]/", "/") + "%";

        sb.append(key).append(" LIKE ? ESCAPE '/' AND ");
        pa[ pi ++ ] = txt ;
      }
      sb.delete(sb.length() - 5, sb.length());
      sb.append(") OR ");
    }
    sb.delete(sb.length() - 4, sb.length());
    sb.append(")"/**/);

    caze.where(sb.toString(), pa );
  }

  /**
   * 当前表字段过滤
   * @param caze
   * @param val
   * @param key
   * @throws HongsException
   */
  protected void mkeyFilter(FetchCase caze, Object val, String key)
    throws HongsException
  {
    if (key.indexOf('.') == -1)
    {
      key = ".`" + key + "`";
    }
    if (val instanceof Map)
    {
      Map map = (Map) val;
      Set set = map.keySet();
      if (map.containsKey(Cnst.EQ_REL))
      {
        set.remove(Cnst.EQ_REL);
        Object vaz = map.get(Cnst.EQ_REL);
        if (vaz != null)
        {
          caze.where(key+ " = ?", vaz);
        }
        else
        {
          caze.where(key+ " IS NULL" );
        }
      }
      if (map.containsKey(Cnst.NE_REL))
      {
        set.remove(Cnst.NE_REL);
        Object vaz = map.get(Cnst.NE_REL);
        if (vaz != null)
        {
          caze.where(key+" != ?", vaz);
        }
        else
        {
          caze.where(key+" IS NOT NULL" );
        }
      }
      if (map.containsKey(Cnst.LT_REL))
      {
        set.remove(Cnst.LT_REL);
        Object vaz = map.get(Cnst.LT_REL);
        caze.where(key + " < ?" , vaz);
      }
      if (map.containsKey(Cnst.LE_REL))
      {
        set.remove(Cnst.LE_REL);
        Object vaz = map.get(Cnst.LE_REL);
        caze.where(key + " <= ?", vaz);
      }
      if (map.containsKey(Cnst.GT_REL))
      {
        set.remove(Cnst.GT_REL);
        Object vaz = map.get(Cnst.GT_REL);
        caze.where(key + " > ?" , vaz);
      }
      if (map.containsKey(Cnst.GE_REL))
      {
        set.remove(Cnst.GE_REL);
        Object vaz = map.get(Cnst.GE_REL);
        caze.where(key + " >= ?", vaz);
      }
      if (map.containsKey(Cnst.IN_REL))
      {
        set.remove(Cnst.IN_REL);
        Object vaz = map.get(Cnst.IN_REL);
        caze.where(key+" IN (?)", vaz);
      }
      if (map.containsKey(Cnst.NI_REL))
      {
        set.remove(Cnst.NI_REL);
        Object vaz = map.get(Cnst.NI_REL);
        caze.where(key+" NOT IN (?)",vaz);
      }
      if (!set.isEmpty())
      {
        String ss = set.toString();
        HongsException ex = new HongsException(0x109c, "Unrecognized symbols: "+ss);
        ex.setLocalizedOptions(ss);
        throw ex;
      }
    } else
    if (val instanceof Collection)
    {
      Set col = new HashSet((Collection) val);
      col.remove(null); col.remove( "" );
      if (!col.isEmpty())
      {
          caze.where(key+" IN (?)", val);
      }
    } else
    {
      if (val != null && !"".equals(val))
      {
          caze.where(key + " = ?" , val);
      }
    }
  }

  /**
   * 关联表字段过滤
   * @param caze
   * @param val
   * @param key
   * @throws app.hongs.HongsException
   */
  protected void skeyFilter(FetchCase caze, Object val, String key)
  throws HongsException
  {
    Set<String> tns = (Set<String>) caze.getOption("ASSOCS");
    if (tns == null)
    {
        tns  = new HashSet();
//      caze.setOption("ASSOCS", tns);
    }

    /**
     * 在 packFilter 内部
     * 不能将条件放入关联对象下
     * 否则无法依据层级包裹条件
     */
    boolean inPack = caze.getOption("IN_PACK", false);

    Map tc =  this.table.getAssoc( key );
    if (tc == null) return;
    String[] ts = Table.getAssocPath(tc);
    String   tn = Table.getAssocName(tc);
    Table    tb =  this.db.getTable (tn);
    Map      cs = tb.getFields();

    Map<String, Object> vs = Synt.declare(val, Map.class);
    for ( Map.Entry et2 : vs.entrySet())
    {
      String key2 = (String)et2.getKey();
      Object val2 = et2.getValue();

      if (cs.containsKey(key2))
      {
        if (val2 != null)
        {
          tns.addAll(Arrays.asList (ts));
          tns.add   (key);
          this.mkeyFilter(inPack ? caze :
              caze.gotJoin(ts).gotJoin(key),
              val2, "`"+key+"`.`"+key2+"`");
        }
      }
    }
  }

  /**
   * 组条件查询语句过滤
   * @param caze
   * @param val
   * @param key
   * @throws HongsException
   */
  protected void packFilter(FetchCase caze, Object val, String key)
  throws HongsException
  {
    /**
     * 构建一个新的查询结构体
     * 仅能获取条件和条件参数
     */
    FetchCase caxe = new FetchCase() {
        public FetchCase init(FetchCase caze) {
            options = caze.getOptions();
            return this;
        }
        @Override
        public Object[ ] getParams() {
            return wparams.toArray();
        }
        @Override
        public String getSQL( ) {
            String ws = wheres.length() > 0 ? wheres.substring(5) : "";
            wheres.setLength(0);
            return ws;
        }
    }.init(caze);

    StringBuilder  ws = new StringBuilder(/**/);
    Set<Map> set = Synt.declare(val, Set.class);
    caxe.setOption("IN_PACK", true);
    for(Map  map : set)
    {
      filter(caxe, map);
      String  wx = caxe.getSQL( );
      if (wx.length() > 0)
      {
        ws.append(' ')
          .append(key)
          .append(' ')
          .append('(')
          .append(wx.substring(5))
          .append(')');
      }
    }
    if (ws.length() > 0)
    {
      caze.filter('('+ws.substring(key.length()+2)+")", caxe.getParams());
    }

    caxe.delOption("IN_PACK");
  }

}
