package app.hongs.serv.medium.handle;

import app.hongs.Core;
import app.hongs.HongsException;
import app.hongs.action.ActionDriver;
import app.hongs.action.ActionHelper;
import app.hongs.db.DB;
import app.hongs.util.Tool;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 频道请求过滤
 * @author Hongs
 */
public class SectionFilter extends ActionDriver implements Filter {

    @Override
    public void doFilter(Core core, ActionHelper hlpr, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest  req = hlpr.getRequest( );
        HttpServletResponse rsp = hlpr.getResponse();
        String url = ActionDriver.getCurrPath( req );

        // 从路径 /medium/section/ID/PN 中分解出ID和页码
        String[] arr = url.split( "/" );
        String   sid = arr.length > 3 ? arr[3] : "" ;
        String   pno = arr.length > 4 ? arr[4] : "" ;
        if ( "".equals(sid) ) sid = "0";
        if ( "".equals(pno) ) pno = "1";
        Map dat = hlpr.getRequestData();

        /**
         * 没有特别的参数
         * 有缓存且未过期
         * 则直接返回缓存
         */
        if (arr.length <= 5 && dat.isEmpty()) {
            File page = new File(Core.BASE_PATH
                      +"/"+ arr[1] +"/"+ arr[2]
                      +"/"+ Tool.splitPath(sid)
                      +"/"+ pno +".html");
            if ( page.exists()) {
                chain.doFilter(req , rsp);
                return;
            }
        }

        String tpl;
        do {
            File fil;

            fil = new File(Core.BASE_PATH + "/medium/action/section/@"+sid+".jsp");
            if (fil.exists()) {
                tpl = "@" + sid;
                break;
            }

            // 获取模板
            try {
                Map row = DB.getInstance("medium").getTable("section")
                        .fetchCase()
                        .select( "temp" )
                        .where ("id = ?", sid)
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
                throw ex.toUnchecked( );
            }

            fil = new File(Core.BASE_PATH + "/medium/action/section/%"+tpl+".jsp");
            if (fil.exists()) {
                tpl = "%" + tpl;
                break;
            }

            tpl = "default";
        } while (false);

        // 转交给模板处理
        dat.put("id", sid);
        dat.put("pn", pno);
        url = "/medium/action/section/"+tpl+".jsp?id="+sid+"&pn="+pno;
        req.getRequestDispatcher(url).forward(req, rsp);
    }

}
