package app.hongs.serv.medium.handle;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.HongsError;
import app.hongs.HongsException;
import app.hongs.action.ActionDriver;
import app.hongs.action.ActionHelper;
import app.hongs.db.DB;
import app.hongs.db.Table;
import app.hongs.util.Tool;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 文章请求过滤
 * @author Hongs
 */
public class ArticleFilter extends ActionDriver implements Filter {

    @Override
    public void doFilter(Core core, ActionHelper hlpr, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest  req = hlpr.getRequest( );
        HttpServletResponse rsp = hlpr.getResponse();
        String url = ActionDriver.getCurrPath( req );

        // 从路径 /medium/article/ID 中分解出ID
        String[] arr = url.split("/");
        String   aid = arr.length > 3 ? arr[3] : "" ;
        if ("".equals(aid)) aid = "0";
        Map dat;
        try {
            dat = hlpr.getRequestData();
        } catch (  HongsException ex  ) {
            dat = new  LinkedHashMap( );
        }

        // 文章浏览量
        String  whr ;
        boolean has = true;
        String  uid = (String) hlpr.getSessibute(Cnst.UID_SES);
        if (uid == null || "".equals(uid)) {
            has = false;
            uid = hlpr.getRequest().getSession().getId();
            whr = "sess_id = ?";
        } else {
            whr = "user_id = ?";
        }
        try {
            Table t = DB.getInstance("medium").getTable("browses");
            Map row = t.fetchCase()
                .where(whr, uid)
                .select("id")
                .one();
            if (row == null || row.isEmpty()) {
                // 记录浏览
                Map d = new HashMap();
                d.put(  "id" , Core.getUniqueId( ));
                d.put("ctime", new Date().getTime() / 1000);
                if (has) {
                    d.put("user_id", uid);
                } else {
                    d.put("sess_id", uid);
                }
                    d.put("link_id", aid);
                d.put( "link", "article");
                t.insert( d );

                // 更新统计
                t = DB.getInstance("medium").getTable("article");
                String sql = "UPDATE `"+t.tableName+"` SET `count_browses` = `count_browses` + 1 WHERE `id` = ?";
                t.db.updates(sql, aid);
            }
        } catch (HongsException ex) {
            System.out.println( ex);
            // Nothing todo.
        }

        /**
         * 没有特别的参数
         * 有缓存且未过期
         * 则直接返回缓存
         */
        if (arr.length <= 5 && dat.isEmpty()) {
            File page = new File(Core.BASE_PATH
                      +"/"+ arr[1] +"/"+ arr[2]
                      +"/"+ Tool.splitPath(aid)
                      +".html");
            if ( page.exists()) {
                chain.doFilter(req , rsp);
                return;
            }
        }

        String tpl;
        do {
            File fil;

            fil = new File(Core.BASE_PATH + "/medium/action/article/@"+aid+".jsp");
            if (fil.exists()) {
                tpl = "@" + aid;
                break;
            }

            // 获取模板
            try {
                Map row = DB.getInstance("medium").getTable("article")
                        .fetchCase()
                        .select( "temp" )
                        .where ("id = ?", aid)
                        .one();
                if (row != null && !row.isEmpty( )) {
                    tpl  = (String) row.get("temp");
                if (tpl == null ||  tpl.isEmpty( )) {
                    tpl  = "default";
                    break;
                }}  else {
                    tpl  = "default";
                    break;
                }
            } catch (HongsException ex) {
                throw new HongsError.Common(ex);
            }

            fil = new File(Core.BASE_PATH + "/medium/action/article/%"+tpl+".jsp");
            if (fil.exists()) {
                tpl = "%" + tpl;
                break;
            }

            tpl = "default";
        } while (false);

        // 转交给模板处理
        dat.put("id", aid);
        url = "/medium/action/article/"+tpl+".jsp?id="+aid;
        req.getRequestDispatcher(url).forward(req, rsp);
    }

}
