package io.github.ihongs.db.util;

import io.github.ihongs.Cnst;
import io.github.ihongs.CruxException;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Table;
import io.github.ihongs.db.link.Link;
import io.github.ihongs.db.link.Lump;
import io.github.ihongs.util.Synt;
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

  private int rows = Cnst.RN_DEF;

  private int past = 0;

  private int page = 1;

  private int pace = 0;

  public FetchPage(FetchCase caze)
  {
    this.db   = null;
    this.tb   = null;
    this.caze = caze;

    chkInit( );
  }

  public FetchPage(FetchCase caze, DB db)
  {
    this.db   = db  ;
    this.tb   = null;
    this.caze = caze;

    chkInit( );
  }

  public FetchPage(FetchCase caze, Table tb)
  {
    this.tb   = tb  ;
    this.db   = null;
    this.caze = caze;

    chkInit( );
  }

  /**
   * 每页条数
   * @param rows
   */
  public void setRows(int rows)
  {
    this.rows = rows;
  }

  /**
   * 跳过条数
   * @param past
   */
  public void setPast(int past)
  {
    this.past = past;
  }

  /**
   * 当前页码
   * @param page
   */
  public void setPage(int page)
  {
    this.page = page;
  }

  /**
   * 续查页数
   * @param pace
   */
  public void setPace(int pace)
  {
    this.pace = pace;
  }

  private void chkInit()
  {
    if (caze == null)
    {
      throw new NullPointerException("FetchPage: FetchCase param required.");
    }

    Object rows2 = caze.getOption(Cnst.RN_KEY);
    if (rows2 != null && rows2.equals(""))
    {
      this.setRows(Synt.asInt(rows2));
    }

    Object past2 = caze.getOption(Cnst.QN_KEY);
    if (past2 != null && past2.equals(""))
    {
      this.setPast(Synt.asInt(past2));
    }

    Object page2 = caze.getOption(Cnst.PN_KEY);
    if (page2 != null && page2.equals(""))
    {
      this.setPage(Synt.asInt(page2));
    }

    Object pace2 = caze.getOption(Cnst.PM_KEY);
    if (pace2 != null && pace2.equals(""))
    {
      this.setPace(Synt.asInt(pace2));
    }
  }

  private Link gotLink()
    throws CruxException
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
      Link link  = this.caze.getLink();
      if ( link != null) return link  ;
      throw new CruxException ( 1163 );
    }
  }

  private List gotList()
    throws CruxException
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
    throws CruxException
  {
    // 设置分页
    int qn = this.past;
    if (this.past == 0 && this.page > 0)
    {
        qn = this.rows * (this.page - 1);
    }
    caze.limit(qn, this.rows );

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
      this.info.put("total",0);
    }

    return list;
  }

  public Map getPage()
    throws CruxException
  {
    this.info.put(Cnst.RN_KEY, this.rows);
    this.info.put(Cnst.QN_KEY, this.past);

    if (this.page > 0)
        this.info.put(Cnst.PN_KEY, this.page);

    if (this.pace > 0)
        this.info.put(Cnst.PM_KEY, this.pace);

    // 列表为空则不用再计算了
    if (this.info.containsKey("count")
    ||  this.info.containsKey("total"))
    {
      return this.info;
    }

    // 有探查数则不用查全部了
    int start;
    int limit;
    if (this.pace > 0)
    {
      start = rows * (page - 1);
      limit = rows *  pace + 1 ;
    }
    else
    {
      start = 0;
      limit = 0;
    }

    FetchCase fc = this.caze.clone();
    Link      dl = this.gotLink();
    String    sql;
    Object[]  pms;

    // 组织总行数查询语句
    if (clnCase(fc) || limit > 0)
    {
      // MySQL 中没排序仍然会遍历全部
      if (tb != null) {
        String fn;
        fn = tb.getField("psort");
        if (fn != null) {
          fc.order (fn);
        } else {
        fn = tb.primaryKey;
        if (fn != null) {
          fc.order (fn);
        }}
      }

      // 内部子查询
      fc.field("1");
      sql = fc.getSQL(   );
      pms = fc.getParams();

      // 限制查询量
      Lump  lp = new Lump(dl, sql, start, limit, pms);
      sql = lp.getSQL(   );
      pms = lp.getParams();

      sql = "SELECT COUNT(1) + "+start+" AS __count__ FROM ("+sql+") AS __table__";
    }
    else
    {
      fc.field("COUNT(1) AS __count__");
      sql = fc.getSQL(   );
      pms = fc.getParams();
    }

    // 计算总行数及总页数
    Map row  = dl.fetchOne( sql, pms );
    if (row != null && ! row.isEmpty())
    {
      long rc, vc, pc;
      Object cc = row.get("__count__");
      if (cc instanceof Number) {
          rc = ((Number) cc).longValue ( );
      } else {
          rc = Long.valueOf(cc.toString());
      }
      // 规避查询总数超两亿的问题
      vc = rc < Integer.MAX_VALUE
         ? rc : Integer.MAX_VALUE;
      pc = (long) Math.ceil((double) vc / rows);

      /**
       * 查得数量与限制数量一致
       * 总数就可能比此数量大些
       * 那么只能作估值
       * 反之为精确数量
       */
      if (limit == 0 || limit + start != rc) {
          this.info.put("state", 1 );
      } else {
          this.info.put("state", 3 );
          rc -= 1;
          pc -= 1;
      }

      this.info.put("count", rc);
      this.info.put("total", pc);
    }

    return this.info;
  }

  private boolean clnCase(FetchCase caze) {
    boolean gos = caze.hasGroup();
    for(FetchCase caz2 : caze.joinSet) {
      if (clnCase(caz2)) {
            gos = true ;
      }
    }
    caze.order(null);
    caze.field(null);
    return  gos; // 有分组不能直接 COUNT
  }

}
