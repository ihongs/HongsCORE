package app.hongs.db;

import app.hongs.HongsException;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 分页查询
 * @author Hongs
 */
public class FetchPage
{

  private DB db;

  private Table tb = null;

  private FetchCase caze;

  private int page =  1 ;

  private int pags =  0 ;

  private int rows = 20 ;

  private Map info = new HashMap();

  public FetchPage(FetchCase caze, DB db) throws HongsException
  {
    this.db    = db;
    this.caze  = caze;

    Object page2 = caze.getOption("page");
    if (page2 != null && page2.equals(""))
    {
      this.setPage(Integer.parseInt(page2.toString()));
    }

    Object lnks2 = caze.getOption("pags");
    if (lnks2 != null && lnks2.equals(""))
    {
      this.setPags(Integer.parseInt(lnks2.toString()));
    }

    Object rows2 = caze.getOption("rows");
    if (rows2 != null && rows2.equals(""))
    {
      this.setRows(Integer.parseInt(rows2.toString()));
    }
  }

  public FetchPage(FetchCase caze, Table table) throws HongsException
  {
    this.db    = table.db;
    this.tb    = table;
    this.caze  = caze;

    Object page2 = caze.getOption("page");
    if (page2 != null && page2.equals(""))
    {
      this.setPage(Integer.parseInt(page2.toString()));
    }

    Object lnks2 = caze.getOption("pags");
    if (lnks2 != null && lnks2.equals(""))
    {
      this.setPags(Integer.parseInt(lnks2.toString()));
    }

    Object rows2 = caze.getOption("rows");
    if (rows2 != null && rows2.equals(""))
    {
      this.setRows(Integer.parseInt(rows2.toString()));
    }
  }

  public void setPage(int page) throws HongsException
  {
    if (page <  0)
    {
      this.getPage( );
      int pc = (Integer) this.info.get("pagecount");
      int pn = page + 1;
      while (pn <  0)
      {
        pn = pn + pc;
      }
      page = pn;
    } else
    if (page == 0)
    {
      page = 1 ;
    }
    this.page = page;
  }

  public void setPags(int pags)
  {
    this.pags = pags;
  }

  public void setRows(int rows)
  {
    this.rows = rows;
  }

  public List gotList()
    throws HongsException
  {
    if (this.tb != null)
    {
      return this.tb.fetchMore(caze);
    }
    else
    {
      return this.db.fetchMore(caze);
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
      this.info.put("err" , 0); // 没有异常
    } else
    if (this.page != 1 )
    {
      this.info.put("err" , 2); // 页码超出
    }
    else
    {
      this.info.put("err" , 1); // 列表为空
      this.info.put("pagecount", 0);
      this.info.put("rowscount", 0);
    }

    return list;
  }

  public Map getPage()
    throws HongsException
  {
    this.info.put("page", this.page);
    this.info.put("pags", this.pags);
    this.info.put("rows", this.rows);

    // 列表为空则不用再计算了
    if (this.info.containsKey("pagecount")
    ||  this.info.containsKey("rowscount"))
    {
      return this.info;
    }

    // 指定链数则不用查全部了
    int limit;
    if (this.pags > 0)
    {
      limit = page - (pags / 2);
      if (limit < 1) limit = 1 ;
      limit = pags + limit - 1 ;
      limit = rows * limit + 1 ;
    }
    else
    {
      limit = 0;
    }

    // 查询总行数
    String     sql;
    Object[]   params;
    FetchCase  caze2 = this.caze.clone().limit(limit);
    if(clnJoin(caze2))
    {
      sql    =  "SELECT COUNT(!*) AS __count__ FROM ("
             + caze2.getSQL( )+") AS __table__" ;
      params = caze2.getParams( );
    }
    else
    {
      caze2.setSelect( "COUNT(!*) AS __count__");
      sql    = caze2.getSQL(/**/);
      params = caze2.getParams( );
    }

    // 计算总行数及总页数
    Map row = this.db.fetchOne(sql, params);
    if (row.isEmpty() == false)
    {
      int rc = Integer.parseInt(row.get("__count__").toString());
      int pc = (int)Math.ceil((float)rc / this.rows);
      boolean uc = this.pags > 0 &&  rc >= limit; // 不确定的总数
      this.info.put("rowscount", rc);
      this.info.put("pagecount", pc);
      this.info.put("uncertain", uc);
    }

    return this.info;
  }

  private boolean clnJoin(FetchCase caze) {
    boolean hasg = caze.hasGroupBy();
    caze.setOrderBy("");
    caze.setSelect ("");

    for (FetchCase caze2 : caze.joinList) {
      if( clnJoin( caze2 )) {
          hasg  =  true  ;
      }
    }

    return hasg;
  }

}
