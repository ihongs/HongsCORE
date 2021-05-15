package io.github.ihongs.db.util;

import io.github.ihongs.Cnst;
import io.github.ihongs.HongsException;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Table;
import io.github.ihongs.db.link.Link;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分页查询
 * @author Hongs
 */
public final class FetchPage
{

  private final DB db;

  private final Table tb;

  private final FetchCase caze;

  private final Map info = new HashMap();

  private int page = 1;

  private int gaps = 0;

  private int rows = Cnst.RN_DEF;

  public FetchPage(FetchCase caze) throws HongsException
  {
    this.db   = null;
    this.tb   = null;
    this.caze = caze;

    chkInit( );
  }

  public FetchPage(FetchCase caze, DB db) throws HongsException
  {
    this.db   = db  ;
    this.tb   = null;
    this.caze = caze;

    chkInit( );
  }

  public FetchPage(FetchCase caze, Table tb) throws HongsException
  {
    this.tb   = tb  ;
    this.db   = null;
    this.caze = caze;

    chkInit( );
  }

  /**
   * 设置页码
   * 1 为首页, 0 视为 1, 可用负数逆向推算,
   * 如果可能为负数则建议先于其他参数设置.
   * @param page
   * @throws io.github.ihongs.HongsException
   */
  public void setPage(int page) throws HongsException
  {
    if (page <  0) {
        getPage( ); // 获取分页信息
        Integer O = (Integer) this.info.get("pages");
        if (O == null || O == 0) {
            O = 1;
        }
        page += O;
        page += 1;
    }
    if (page <= 0) {
        page  = 1;
    }
    this.page = page;
  }

  public void setGaps(int gaps)
  {
    this.gaps = gaps;
  }

  public void setRows(int rows)
  {
    this.rows = rows;
  }

  private void chkInit()
    throws HongsException
  {
    if (caze == null)
    {
      throw new NullPointerException("FetchPage: FetchCase param required.");
    }

    Object page2 = caze.getOption(Cnst.PN_KEY);
    if (page2 != null && page2.equals(""))
    {
      this.setPage(Integer.parseInt(page2.toString()));
    }

    Object lnks2 = caze.getOption(Cnst.GN_KEY);
    if (lnks2 != null && lnks2.equals(""))
    {
      this.setGaps(Integer.parseInt(lnks2.toString()));
    }

    Object rows2 = caze.getOption(Cnst.RN_KEY);
    if (rows2 != null && rows2.equals(""))
    {
      this.setRows(Integer.parseInt(rows2.toString()));
    }
  }

  private Link gotLink()
    throws HongsException
  {
    if (this.db != null)
    {
      return this.db;
    } else
    if (this.tb != null)
    {
      return this.tb.db;
    } else
    {
      Link link  = this.caze.linker( );
      if ( link != null ) return link ;
      throw new HongsException( 1163 );
    }
  }

  private List gotList()
    throws HongsException
  {
    if (this.tb != null)
    {
      return this.tb.fetchMore(caze);
    } else
    if (this.db != null)
    {
      return this.db.fetchMore(caze);
    } else
    {
      return this.caze.getAll (    );
    }
  }

  public List getList()
    throws HongsException
  {
    // 设置分页
    caze.limit((this.page - 1) * this.rows, this.rows);

    // 获取行数
    List list = this.gotList();
    if (!list.isEmpty())
    {
      this.info.put("state",1); // 没有异常
    } else
    if ( this.page != 1)
    {
      this.info.put("state",0); // 页码超出
    }
    else
    {
      this.info.put("state",0); // 列表为空
      this.info.put("count",0);
      this.info.put("pages",0);
    }

    return list;
  }

  public Map getPage()
    throws HongsException
  {
    this.info.put(Cnst.PN_KEY, this.page);
    this.info.put(Cnst.GN_KEY, this.gaps);
    this.info.put(Cnst.RN_KEY, this.rows);

    // 列表为空则不用再计算了
    if (this.info.containsKey("count")
    ||  this.info.containsKey("pages"))
    {
      return this.info;
    }

    // 指定链数则不用查全部了
    int limit;
    if (this.gaps > 0)
    {
      limit = page - (gaps / 2);
      if (limit < 1) limit = 1 ;
      limit = gaps + limit - 1 ;
      limit = rows * limit + 1 ;
    }
    else
    {
      limit = 0;
    }

    // 组织总行数查询语句
    String     sql;
    Object[]   params;
    FetchCase  caze2 = this.caze.clone().limit(limit);
    if(clnSort(caze2))
    {
      caze2.field("1");
      params = caze2.getParams();
      sql    = caze2.getSQL(   );
      sql    = "SELECT COUNT(1) AS __count__ FROM (" + sql +") AS __table__";
    }
    else
    {
      caze2.field(/**/"COUNT(1) AS __count__");
      params = caze2.getParams();
      sql    = caze2.getSQL(   );
    }

    // 计算总行数及总页数
    Map row  = gotLink().fetchOne(sql, params);
    if (row != null && ! row.isEmpty())
    {
      int rc, pc;
      Object  cc = row.get("__count__");
      if (cc instanceof Number) {
          rc = ((Number) cc).intValue();
      } else {
          rc = Integer.valueOf(cc.toString( ));
      }
      pc = (int) Math.ceil((double) rc / rows);

      /**
       * 查得数量与限制数量一致
       * 总数就可能比此数量大些
       * 那么只能作估值
       * 反之为精确数量
       */
      if (limit == 0 || limit != rc) {
          this.info.put("state", 1 );
      } else {
          this.info.put("state", 2 );
          rc -= 1;
          pc -= 1;
      }

      this.info.put("count", rc);
      this.info.put("pages", pc);
    }

    return this.info;
  }

  /**
   * 检查是否有启用分组
   * 同时清空排序和查询
   * @param caze
   * @return
   */
  private boolean clnSort(FetchCase caze) {
    boolean gos = caze.hasGroup();
      caze.order( null );
      caze.field( null );
    for(FetchCase caze2 : caze.joinSet) {
    if ( clnSort( caze2)) {
            gos = true  ;
    }}
    return  gos;
  }

}
