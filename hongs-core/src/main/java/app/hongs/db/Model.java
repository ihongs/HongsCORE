package app.hongs.db;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.CoreConfig;
import app.hongs.HongsException;
import app.hongs.db.util.FetchCase;
import app.hongs.db.util.FetchPage;
import app.hongs.db.util.UniteCase;
import app.hongs.dh.IEntity;
import app.hongs.util.Synt;
import java.util.Iterator;
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
   * 可列举的字段
   */
  public String[] listable = null;

  /**
   * 可排序的字段
   */
  public String[] sortable = null;

  /**
   * 可搜索的字段
   */
  public String[] findable = null;

  /**
   * 可过滤的字段
   */
  public String[] filtable = null;

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
    if (cs != null) this.findable = cs.split(",");
    cs = table.getField("listCols");
    if (cs != null) this.listable = cs.split(",");
    cs = table.getField("sortCols");
    if (cs != null) this.sortable = cs.split(",");
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
    String[] cf = listable;
    String id = add(rd);
    if (cf  ==  null  ) {
        cf = (String[]) table.getFields().keySet().toArray(new String[]{});
    }
    if (cf.length == 0) {
        rd.put(table.primaryKey, id);
        return rd;
    } else {
        Map sd = new LinkedHashMap();
        sd.put(table.primaryKey, id);
        for(String fn: listable) {
        if ( ! fn.contains(".")) {
            sd.put(fn, rd.get( fn ));
        }
        }
        return sd;
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
    Map wh = Synt.declare(rd.get(Cnst.WH_KEY), new HashMap());
    FetchCase fc = caze != null ? caze.clone() : fetchCase( );
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
    Map wh = Synt.declare(rd.get(Cnst.WH_KEY), new HashMap());
    FetchCase fc = caze != null ? caze.clone() : fetchCase( );
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
      caze = fetchCase();
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

    caze.filter(".`"+n+"` = ?", v);

    Iterator it = rd.entrySet().iterator();
    while (it.hasNext())
    {
      Map.Entry entry = (Map.Entry)it.next();
      String field = (String) entry.getKey();
      String value = (String) entry.getValue();

      if (columns.containsKey(field))
      {
        if (field.equals(Cnst.ID_KEY)
        ||  field.equals(this.table.primaryKey))
        {
          caze.filter(".`"+this.table.primaryKey+"` != ?", value);
        }
        else
        {
          caze.filter(".`"+field+"` = ?", value);
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
    caze = caze != null ? caze.clone() : fetchCase();
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
    caze = caze != null ? caze.clone() : fetchCase();
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
    caze = caze != null ? caze.clone() : fetchCase();
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
      caze = fetchCase();
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
      caze = fetchCase();
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
   * "查询"过滤
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
   * 1) 按照rb参数设置查询字段;
   *    限定字段列表: rb=a+b+c或rb=-a+x.b, -表示排除该字段;
   * 2) 按照ob参数设置排序方式,
   *    多个字段排序: ob=a+b+c或ob=-a+b+c, -表示该字段逆序;
   * 3) 按照wd参数设置模糊查询,
   *    多关键词搜索: wd=x+y+z;
   *    指定字段搜索: wd.a=x或wd.a.b=y, 同样适用上面的规则,
   *    a.b为搜索关联表, 但需注意: a,a.b必须在findCols中有指定;
   * 4) 如果有字段名相同的参数则获取与之对应的记录,
   *    可以在字段名后跟.加上!gt,!lt,!ge,!le,!ne分别表示&gt;,&lt;,&ge;,&le;,&ne;
   * 5) 如果有子表.字段名相同的参数则获取与之对应的记录,
   *    可以在子表.字段名后跟.加上!gt,!lt,!ge,!le,!ne分别表示&gt;,&lt;,&ge;,&le;,&ne;
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
     * 如果没指定查询的表、字段
     * 默认只关联 BLS_TO , HAS_ONE 的表(仅能关联一个)
     * 默认只连接 LEFT,INNER,RIGHT 的表(常规关联均可)
     */
    if (null == caze.getOption("ASSOCS")
    && !caze.getOption("INCLUDE_HASMANY", false)
    && ("getAll" .equals(caze.getOption("MODEL_METHOD"))
    ||  "getList".equals(caze.getOption("MODEL_METHOD"))
    ))
    {
      if (null == caze.getOption("ASSOC_TYPES"))
      {
        Set types = new HashSet();
        types.add("BLS_TO" );
        types.add("HAS_ONE");
        caze.setOption("ASSOC_TYPES", types);
      }
      if (null == caze.getOption("ASSOC_JOINS"))
      {
        Set types = new HashSet();
        types.add( "INNER" );
        types.add( "LEFT"  );
        types.add( "RIGNT" );
        types.add( "FULL"  );
        caze.setOption("ASSOC_JOINS", types);
      }
    }

    if (! rd.isEmpty())
    {
      UniteCase uc = new UniteCase(caze);
      uc.allow(this);
      if (this.listable != null) uc.allow(UniteCase.LISTABLE, listable);
      if (this.sortable != null) uc.allow(UniteCase.SORTABLE, sortable);
      if (this.findable != null) uc.allow(UniteCase.FINDABLE, findable);
      if (this.filtable != null) uc.allow(UniteCase.FILTABLE, filtable);
      uc.trans( rd );
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
    if (caze.getOption("ASSOCS") == null && caze.getJoinSet().isEmpty())
    {
        caze.setOption("ASSOCS", new HashSet());
    }
    caze.field (".`"+this.table.primaryKey+"`")
        .filter(".`"+this.table.primaryKey+"`=?", id);
    this.filter(caze, wh);
    return ! this.table.fetchLess( caze ).isEmpty(  );
  }

  /**
   * 内建 FetchCase
   * 用于用户未给 FetchCase 时的默认查询
   * 此方法会设置 UNFIX_FIELD, UNFIX_ALIAS 为 true
   * @return
   * @throws app.hongs.HongsException
   */
  public FetchCase fetchCase() throws HongsException
  {
    return table.fetchCase()
      .setOption("UNFIX_FIELD", true)
      .setOption("UNFIX_ALIAS", true);
  }

}
