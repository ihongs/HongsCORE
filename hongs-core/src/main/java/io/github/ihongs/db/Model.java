package io.github.ihongs.db;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.HongsExemption;
import io.github.ihongs.db.util.FetchCase;
import io.github.ihongs.db.util.FetchPage;
import io.github.ihongs.db.util.AssocCase;
import io.github.ihongs.dh.IEntity;
import io.github.ihongs.util.Synt;
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
 * 基础动作方法: search,create,update,delete
 * 扩展动作方法: unique,exists
 * 通常它们被动作类直接调用;
 * 基础模型方法: get,add,put,del
 * 一般改写只需覆盖它们即可;
 * filter, permit 分别用于获取和更改数据等常规操作时进行过滤,
 * permit 默认调用 filter 来实现的, 可覆盖它来做资源过滤操作.<br/>
 * search 可使用查询参数:
 * <code>
 * ?pn=1&rn=10&f1=123&f2.gt=456&wd=a+b&ob=f1+f2!&rb=id+f1+f2
 * </code>
 * 详见 filter 方法说明
 * </p>
 *
 * <h3>异常代码:</h3>
 * <pre>
 * 区间: 1085~1099
 * 1091 创建时不得含有id
 * 1092 更新时必须含有id
 * 1093 删除时必须含有id
 * 1094 获取时必须含有id
 * 1096 无权更新该资源
 * 1097 无权删除该资源
 * 1098 无权获取该资源
 * 1085 参数n和v不能为空(检查存在)
 * 1086 指定的字段不存在(检查存在)
 * 1087 不支持的运算符: $0
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
   * 构造方法
   *
   * 需指定该模型对应的表对象.
   * 如传递的 id,wd,pn,gn,rn,rb,ob 等参数名不同,
   * 可在构造时分别指定;
   * 请指定被搜索的字段.
   *
   * @param table
   * @throws io.github.ihongs.HongsException
   */
  public Model(Table table)
    throws HongsException
  {
    this.table = table;
    this.db = table.db;
  }

  //** 标准动作方法 **/

  /**
   * 综合提取方法
   * @param rd
   * @return
   * @throws io.github.ihongs.HongsException
   */
  @Override
  public Map search(Map rd)
    throws HongsException
  {
    return this.search(rd, null);
  }

  /**
   * 综合提取方法
   * @param rd
   * @param caze
   * @return
   * @throws io.github.ihongs.HongsException
   */
  public Map search(Map rd, FetchCase caze)
    throws HongsException
  {
    Object id = rd.get ( Cnst.ID_KEY );
    if (id == null) {
        id =  rd.get(table.primaryKey);
    }
    if (id instanceof String || id instanceof Number) {
        return getInfo(rd , caze);
    } else {
        return getList(rd , caze);
    }
  }

  /**
   * 创建记录
   *
   * @param rd
   * @return 新增编号
   * @throws io.github.ihongs.HongsException
   */
  @Override
  public String create(Map rd)
    throws HongsException
  {
    return this.create(rd, null);
  }

  /**
   * 创建记录
   *
   * @param rd
   * @param caze
   * @return 新增编号
   * @throws io.github.ihongs.HongsException
   */
  public String create(Map rd, FetchCase caze)
    throws HongsException
  {
    String id = caze == null ? null
     : (String) caze.getOption(Cnst.ID_KEY);
    if (id == null || id.isEmpty( )) {
        id = Core.newIdentity();
    }
    if (rd.containsKey(Cnst.ID_KEY)) {
        rd.put(Cnst.ID_KEY, id);
    }

    add(id, rd);
    return  id ;
  }

  /**
   * 更新记录
   *
   * @param rd
   * @return 更新条数
   * @throws io.github.ihongs.HongsException
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
   * @throws io.github.ihongs.HongsException
   */
  public int update(Map rd, FetchCase caze)
    throws HongsException
  {
    Object idz = rd.get ( Cnst.ID_KEY );
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

    Map xd = new HashMap ( rd );
    xd.remove(table.primaryKey);

    // 检查是否可更新
    FetchCase fc = caze != null
                 ? caze.clone()
                 : fetchCase ();
    fc.setOption("MODEL_START", "update");
    permit(fc , rd , ids);

    for (String id : ids)
    {
      this.put( id , xd );
    }

    return ids.size();
  }

  /**
   * 删除记录
   *
   * @param rd
   * @return 删除条数
   * @throws io.github.ihongs.HongsException
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
   * @throws io.github.ihongs.HongsException
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

    // 检查是否可更新
    FetchCase fc = caze != null
                 ? caze.clone()
                 : fetchCase ();
    fc.setOption("MODEL_START", "delete");
    permit(fc , rd , ids);

    for (String id : ids)
    {
      this.del( id , fc );
    }

    return ids.size();
  }

  //** 扩展动作方法 **/

  public boolean unique(Map rd)
    throws HongsException
  {
    return  unique(rd, null);
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
   * @throws io.github.ihongs.HongsException
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
   * @throws io.github.ihongs.HongsException
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
      throw new HongsException(1085, "Param n or v can not be empty" );
    }

    String n = (String) rd.get("n");
    String v = (String) rd.get("v");

    Map columns = this.table.getFields();

    // 是否缺少n对应的字段
    if (!columns.containsKey(n))
    {
      throw new HongsException(1086, "Column " + n + " is not exists");
    }

    caze.filter("`"+this.table.name+"`.`"+n+"` = ?", v);

    Iterator it = rd.entrySet().iterator();
    while (it.hasNext())
    {
      Map.Entry entry = (Map.Entry)it.next();
      String field = (String) entry.getKey();
      String value = (String) entry.getValue();

      if (field.equals( this.table.primaryKey)
      ||  field.equals( Cnst.ID_KEY))
      {
      if (value != null && ! value.equals(""))
      {
        caze.filter("`"+this.table.name+"`.`"+this.table.primaryKey+"` != ?", value);
      }
      } else
      if (columns.containsKey(field))
      {
        caze.filter("`"+this.table.name+"`.`"+field+"` = ?", value);
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
   * @throws io.github.ihongs.HongsException
   */
  public String set(Map rd)
    throws HongsException
  {
    String id = Synt.asString(rd.get(this.table.primaryKey));
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
    // 存入主数据
    rd.remove(this.table.primaryKey );
    int an = this.table.insert ( rd );
    // 自增ID, MySQL: last_insert_id() SQLite: last_insert_rowid()
    Map rd = this.db.fetchOne  ( "SELECT last_insert_id() AS id" );
    Object id = rd.get("id");
    // 存入子数据
    rd.put(this.table.primaryKey, id);
    this.table.insertSubValues ( rd );

 或对 DB,Table,Model 同步改造, 使用 PreparedStatement.getGeneratedKeys 获取
   *
   * @param rd
   * @return 记录ID
   * @throws io.github.ihongs.HongsException
   */
  public String add(Map rd)
    throws HongsException
  {
    String id = Core.newIdentity();
    add(id,rd);
    return id ;
  }

  /**
   * 添加记录
   *
   * @param id
   * @param rd
   * @return 添加条数
   * @throws io.github.ihongs.HongsException
   */
  public int add(String id, Map rd)
    throws HongsException
  {
    if (id == null || id.length() == 0)
    {
      throw new HongsException (1091, "ID can not be empty for add");
    }

    // 存入主数据
    rd.put(this.table.primaryKey, id);
    int an = this.table.insert ( rd );

    // 存入子数据
//  rd.put(this.table.primaryKey, id);
    this.table.insertSubValues ( rd );

    return an;
  }

  /**
   * 更新记录
   *
   * @param rd
   * @param id
   * @return 更新条数
   * @throws io.github.ihongs.HongsException
   */
  public int put(String id, Map rd)
    throws HongsException
  {
    if (id == null || id.length() == 0)
    {
      throw new HongsException (1092, "ID can not be empty for put");
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
   * @throws io.github.ihongs.HongsException
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
   * @throws io.github.ihongs.HongsException
   */
  public int del(String id, FetchCase caze)
    throws HongsException
  {
    if (id == null || id.length() == 0)
    {
      throw new HongsException (1093, "ID can not be empty for del");
    }

    // 删除主数据, 默认可使用逻辑删除
    int an;
    if (caze == null || ! caze.getOption( "INCLUDE_REMOVED", false ) )
    {
      an = this.table.remove ("`"+ this.table.primaryKey +"` = ?", id);
    }
    else
    {
      an = this.table.delete ("`"+ this.table.primaryKey +"` = ?", id);
    }

    // 删除子数据
    if (caze == null || ! caze.getOption( "EXCLUDE_HASMANY", false ) )
    {
      this.table.deleteSubValues ( id );
    }

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
      throw new HongsException (1094, "ID can not be empty for get");
    }

    Map rd = new HashMap();
    rd.put(this.table.primaryKey, id);

    // 调用filter进行过滤
    caze = caze != null ? caze.clone() : fetchCase();
    caze.setOption("MODEL_START", "get");
    this.filter(caze , rd);

    return this.table.fetchLess(caze);
  }

  public Map getOne(Map rd)
    throws HongsException
  {
    return this.getOne(rd, null);
  }

  public Map getOne(Map rd, FetchCase caze)
    throws HongsException
  {
    if (rd == null) {
        rd = new HashMap();
    }

    // 调用filter进行过滤
    caze = caze != null ? caze.clone() : fetchCase();
    caze.setOption("MODEL_START", "getOne");
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
    caze.setOption("MODEL_START", "getAll");
    this.filter(caze , rd);

    return this.table.fetchMore(caze);
  }

  /**
   * 获取信息(无查询结构)
   *
   * @param rd
   * @return 记录信息
   * @throws io.github.ihongs.HongsException
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
   * @throws io.github.ihongs.HongsException
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

    caze.setOption("MODEL_START", "getInfo");
    this.filter(caze, rd);

    Map info = table.fetchLess(caze);

    Map data = new HashMap();
    data.put( "info", info );

    /**
     * 与 list 保持一致, 用 rn 控制 page
     * rn= 1 正常
     * rn= 0 不给 page
     * rn=-1 返回 page.count=0 缺失 page.count=1 受限
     */
    int rn = Synt.declare(rd.get(Cnst.RN_KEY), 1);
    if (rn == 0) {
        return data ;
    }

    Map page = new HashMap();
    data.put( "page", page );

    /**
     * 查不到可能是不存在、已删除或受限
     * 需通过 id 再查一遍，区分不同错误
     */
    page.put(Cnst.RN_KEY,rn);
    Object id = rd.get(table.primaryKey);
    if (info != null && ! info.isEmpty()) {
        page.put("state", 1);
        page.put("count", 1);
    } else
    if (rn >= 0 || id == null) {
        page.put("state", 0);
        page.put("count", 0);
    } else {
        FetchCase fc = new FetchCase(FetchCase.STRICT)
            .filter(table.primaryKey , id )
            .select(table.primaryKey);
        Map row = table.fetchLess(fc);
        if (row != null && ! row.isEmpty()) {
            page.put("state", 0);
            page.put("count", 1);
        }  else {
            page.put("state", 0);
            page.put("count", 0);
        }
    }

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
   * @throws io.github.ihongs.HongsException
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
   * @throws io.github.ihongs.HongsException
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

    caze.setOption("MODEL_START", "getList");
    this.filter(caze, rd);

    // 获取行数, 默认依从配置
    int rows = Cnst.RN_DEF ;
    if (rd.containsKey(Cnst.RN_KEY))
    {
      rows = Synt.declare(rd.get(Cnst.RN_KEY), rows);
    }

    // 获取页码, 默认为第一页
    int page = 1;
    if (rd.containsKey(Cnst.PN_KEY))
    {
      page = Synt.declare(rd.get(Cnst.PN_KEY), page);
    }

    // 续查页数, 默认查总页数
    int ques = 0;
    if (rd.containsKey(Cnst.QN_KEY))
    {
      ques = Synt.declare(rd.get(Cnst.QN_KEY), ques);
    }

    Map data = new HashMap();

    if (rows != 0)
    {
      caze.from (table.tableName , table.name );
      FetchPage fp = new FetchPage(caze, table);
      fp.setPage(page);
      fp.setQues(Math.abs(ques));
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

  /**
   * 内建 FetchCase
   * 此方法会设置 CLEVER_MODE 为 false
   * 后续构建查询语句仅作简单拼接
   * 您必须严格的使用表和字段别名
   * @return
   * @throws io.github.ihongs.HongsException
   */
  public FetchCase fetchCase() throws HongsException
  {
      return table.fetchCase().setOption("CLEVER_MODE", false);
  }

  //** 辅助过滤方法 **/

  /**
   * 操作确认
   *
   * <pre>
   * 作用于 update,delete 上
   *
   * 如需添加过滤条件, 请重写此方法;
   * 有不可操作的行时, 通过 caze 上的 MODEL_START 来区分异常:
   * update 对应 1096
   * delete 对应 1097
   * 其他的 对应 1098
   * 描述为 Can not update|delete|search by id: ID1, ID2, IDn
   * </pre>
   *
   * @param caze
   * @param rd
   * @param id
   * @throws io.github.ihongs.HongsException
   */
  protected void permit(FetchCase caze, Map rd, Set id)
    throws HongsException
  {
    Map wh = new HashMap();
    if (rd.containsKey(Cnst.AR_KEY)) {
        wh.put(Cnst.AR_KEY, rd.get(Cnst.AR_KEY));
    }
    if (rd.containsKey(Cnst.OR_KEY)) {
        wh.put(Cnst.OR_KEY, rd.get(Cnst.OR_KEY));
    }
    if (wh.isEmpty()) {
        return;
    }

    // 组织查询
    wh.put(table.primaryKey, id);
    wh.put(Cnst.RB_KEY, Synt.setOf(table.primaryKey));
    caze.use(db).from ( table.tableName, table.name );
    this.filter (caze , wh);
    Set xd = new HashSet( );
    for(Map row : caze.select()) {
        xd.add(Synt.asString(row.get(table.primaryKey)));
    }

    // 对比数量, 取出多余的部分作为错误消息抛出
    if (xd.size() != id.size() ) {
        Set    zd = new HashSet(id);
               zd . removeAll  (xd);
        String er = zd.toString(  );
        String mm = caze.getOption("MODEL_START", "");
        if ("update".equals(mm)) {
            throw new HongsException(1096, "Can not update by id: " + er);
        } else
        if ("delete".equals(mm)) {
            throw new HongsException(1097, "Can not delete by id: " + er);
        } else
        {
            throw new HongsException(1098, "Can not search by id: " + er);
        }
    }
  }

  /**
   * 查询过滤
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
   *    限定字段列表: rb=a+b+c或rb=a!+x.b, !表示排除该字段;
   * 2) 按照ob参数设置排序方式,
   *    多个字段排序: ob=a+b+c或ob=a!+b+c, !表示该字段逆序;
   * 3) 按照wd参数设置模糊查询,
   *    多关键词搜索: wd=x+y+z;
   *    指定字段搜索: a.cq=x或a.b.cq=y, 同样也适用上述规则,
   *    a.b为搜索关联表, 注意: a,a.b必须在srchable中有指定;
   * 4) 如果有字段名相同的参数则获取与之对应的记录,
   *    可以在字段名后跟.加上.gt,.lt,.ge,.le,.ne分别表示&gt;,&lt;,&ge;,&le;,&ne;
   * 5) 如果有子表.字段名相同的参数则获取与之对应的记录,
   *    可以在子表.字段名后跟.加上.gt,.lt,.ge,.le,.ne分别表示&gt;,&lt;,&ge;,&le;,&ne;
   * 注: "+" 在URL中表示空格; 以上设计目录均已实现; 以上1/2/3中的参数名可统一设置或单独指定;
   *
   * [2016/9/4] 以上过滤逻辑已移至 AssocCase, 但未指定 listable 时字段过滤使用本类的 field,allow 来处理
   * </pre>
   *
   * @param caze
   * @param rd
   * @throws io.github.ihongs.HongsException
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
    && ("getAll" .equals(caze.getOption("MODEL_START"))
    ||  "getList".equals(caze.getOption("MODEL_START"))
    )) {
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
        types.add( "RIGHT" );
        types.add( "FULL"  );
        caze.setOption("ASSOC_JOINS", types);
      }
    }

    /**
     * 补全空白关联数据
     * 避免外部解析麻烦
     */
    caze.setOption("ASSOC_FILLS", true);

    if ( rd == null  ||  rd.isEmpty() )
    {
      return;
    }

    /**
     * 如果不是 table.fetchCase( ) 构建查询
     * 在下面组织过滤等时可能导致表别名为空
     */
    caze.from( table.tableName, table.name );

    /**
     * 没有指定 listable
     * 则不使用 AssocCase  来处理查询的字段
     * 此方法并不会将所有字段绑定在顶层用例
     * 而是按照不同表分别绑在对应下级查询上
     * 这可为非 JOIN 关联的用例指定查询字段
     */
    Object rb  = null ;
    if (! caze.hasField ()
    &&  !table.getParams().containsKey("listable"))
    {
      rb = rd.remove(Cnst.RB_KEY);
      Set <String> sb = Synt.toTerms(rb);
      if ( sb != null && !sb.isEmpty() )
      {
        field(caze,sb);
      }
    }

    // 以此绕开 AssocCase
    // 后续调试可正常观察
    try {
      AssocCase uc = new AssocCase(caze);
      uc.allow(this);
      uc.parse( rd );
    } finally {
      if (rb !=null)
      {
        rd.put(Cnst.RB_KEY, rb);
      }
    }
  }

    //** 查询字段处理 **/

    /**
     * 绑定许可字段
     * @param caze  查询用例
     * @param rb    排序字段, 结构 { FIELD1, FIELD2... }, 后跟 ! 号或前跟 - 号表逆序
     */
    protected final void field(FetchCase caze, Set rb) {
        if (rb == null) {
            rb =  new HashSet();
        }

        Map<String, Object[]> af = new LinkedHashMap();             // 许可的字段
        Map<String, Set<String>>  cf  =  new HashMap();             // 通配符字段
        Set<String> tc = caze.getOption("ASSOCS", new HashSet());   // 可关联的表
        Set<String> ic = new LinkedHashSet();                       // 包含字段
        Set<String> ec = new LinkedHashSet();                       // 排除字段
        Set<String> xc ;

        allow(caze, af);

        // 整理出层级结构, 方便处理通配符
        for(Map.Entry<String, Object[]> et : af.entrySet()) {
            String  fn = et.getKey(   );
            int p = fn.lastIndexOf(".");
            String  k  ;
            if (p > -1) {
                k = fn.substring(0 , p)+".*";
            } else {
                k = "*";
            }

            Set<String> fs = cf.get( k);
            if (fs == null  ) {
                fs  = new LinkedHashSet( );
                cf.put(k, fs);

                // 当字段别名有点时表示是 JOIN 关联, 这种情况总是需要查询
                Object[]  fa = et.getValue();
                String    ln = (String   ) fa[1]; // 别名
                FetchCase fc = (FetchCase) fa[2]; // 用例
                if (ln.contains( "." ) ) {
                    tc.add( fc.getName() );
                }
            }

            fs.add(fn);
        }

        for(Object fo : rb) {
            String fn = fo.toString();
            if (fn.  endsWith("!") ) { // 新的排除后缀
                fn = fn.substring(0, fn.length() - 1);
                xc = ec;
            } else
            if (fn.startsWith("-") ) { // 旧的排除前缀
                fn = fn.substring(1);
                xc = ec;
            } else {
                xc = ic;
            }

            if (cf.containsKey(fn) ) {
                xc.addAll(cf.get(fn));
            } else
            if (af.containsKey(fn) ) {
                xc.add(fn);

                // 排除时, 先在包含中增加全部
                if (xc == ec) {
                    int p  = fn.lastIndexOf(".");
                    if (p != -1) {
                        fn = fn.substring(0 , p)+".*";
                    } else {
                        fn = "*";
                    }
                    ic.addAll(cf.get(fn));
                }
            }
        }

        // 默认没给就是全部
        if (ic.isEmpty() == true ) {
            ic.addAll(af.keySet());
        }

        // 排除字段即取差集
        if (ec.isEmpty() == false) {
            ic.removeAll(ec);
        }

        for(String  kn : ic) {
            Object[]  fa = af.get (kn);
            String    fn = (String   ) fa[0]; // 字段
            String    ln = (String   ) fa[1]; // 别名
            FetchCase fc = (FetchCase) fa[2]; // 用例
            if (fn != null && ln != null) {
                fc.select(fn + " AS `" + ln + "`");
            }

            tc.add( fc.getName( ) );
        }

        caze.setOption("ASSOCS",tc);
    }

    /**
     * 提取许可字段
     * @param caze  查询用例
     * @param af    返回字段, 结构: { KEY: [FIELD, ALIAS, FetchCase]... }
     */
    protected final void allow(FetchCase caze, Map af) {
        String name = Synt.defoult(
               caze.getName( ) , table.name , table.tableName );
        allow( table,table, table.getAssocs(),table.getParams()
             , caze , name, null , null, af );
    }

    /**
     * 递归提取字段
     * @param table 顶层模型库表
     * @param assoc 当前关联库表
     * @param ac    当前下级关联
     * @param pc    当前层级参数
     * @param caze  当前查询用例
     * @param tn    当前表名
     * @param qn    层级名称
     * @param pn    相对层级
     * @param al    字段集合, 结构: {参数: [字段, 别名, 查询用例]}
     */
    private void allow(Table table, Table assoc, Map ac, Map pc,
        FetchCase caze, String tn, String qn, String pn, Map al) {
        String tx, ax, az;
        tx = "`"+tn+"`." ;

        if (null ==  qn  ) {
            qn = "";
            ax = "";
        } else
        if ("".equals(qn)) {
            qn = tn;
            ax = qn + ".";
        } else {
            qn = qn + "."+ tn ;
            ax = qn + ".";
        }

        if (null ==  pn  ) {
            pn = "";
            az = "";
        } else
        if ("".equals(pn)) {
            pn = tn;
            az = pn + ".";
        } else {
            pn = pn + "."+ tn ;
            az = pn + ".";
        }

        if (pc != null && pc.containsKey( "fields" )) {
            al.put(az+"*", new Object[] {
                null, null, caze
            });
        } else try {
            Map fs = assoc.getFields( );
            for(Object n : fs.keySet()) {
                String f = (String) n;
                String k = f;
                String l = f;
                f = tx +"`"+ f +"`"; // 字段完整名
                l = az +/**/ l /**/; // 字段别名
                k = ax +/**/ k /**/; // 外部键
                al.put(k , new Object[] {f, l, caze});
            }
        } catch (HongsException e ) {
            throw e.toExemption(  );
        }

        if (ac == null || ac.isEmpty()) {
            return;
        }

        Iterator it = ac.entrySet().iterator(  );
        while (it.hasNext()) {
            Map.Entry et = (Map.Entry) it.next();
            Map       tc = (Map) et.getValue(  );
            String jn = (String) tc.get ("join");

            // 不是 JOIN 的重置 pn, 层级名随之改变
            if (!"INNER".equals(jn) && !"LEFT".equals(jn)
            &&  !"RIGHT".equals(jn) && !"FULL".equals(jn)) {
                jn = null;
            } else {
                jn =  pn ;
            }

            tn = (String) et.getKey(  );
            ac = (Map) tc.get("assocs");
            pc = (Map) tc.get("params");

            // 获取真实的表名, 构建关联表实例
            String rn;
            rn = (String) tc.get("tableName");
            if (rn == null || "".equals( rn )) {
                rn = (String) tc.get ("name");
            }

            FetchCase caxe;
            try {
                assoc = table.db.getTable(rn);
                caxe  = caze .   gotJoin (tn);
            } catch (HongsException e ) {
                throw e.toExemption(  );
            }
            if (null == assoc) {
                throw new HongsExemption(1026,
                    "Can not get table '"+ rn +"' in DB '"+ table.db.name +"'"
                );
            }

            allow(table, assoc, ac, pc, caxe, tn, qn, jn, al);
        }
    }

}
