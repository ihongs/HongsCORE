package app.hongs.action.serv;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.CoreConfig;
import app.hongs.action.ActionDriver;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.util.Data;
import app.hongs.util.Synt;
import app.hongs.util.Tool;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 应用程序接口
 *
 * <p>
 * REST 适配器, 可将不同 Method 请求转发到原有的 Action 方法:<br/>
 * <pre>
 * GET      retrieve, list or info
 * PUT      update, save
 * POST     create, save
 * DELETE   delete
 * </pre>
 * </p>
 *
 * <h3>web.xml配置:</h3>
 * <pre>
 * &lt;servlet&gt;
 *   &lt;servlet-name&gt;ApisAction&lt;/servlet-name&gt;
 *   &lt;servlet-class&gt;app.hongs.action.ApisAction&lt;/servlet-class&gt;
 * &lt;/servlet&gt;
 * &lt;servlet-mapping&gt;
 *   &lt;servlet-name&gt;ApisAction&lt;/servlet-name&gt;
 *   &lt;url-pattern&gt;*.api&lt;/url-pattern&gt;
 * &lt;/servlet-mapping&gt;
 * </pre>
 *
 * @author Hongs
 */
public class ApisAction
  extends  ActionDriver
{
    private String dataKey;
    private String callKey;
    private String convKey;
    private String flatKey;
    private String wrapKey;
    private String scokKey;

    @Override
    public void init(ServletConfig conf) throws ServletException {
        super.init(conf);

        CoreConfig cc = CoreConfig.getInstance( );
        dataKey  = cc.getProperty("core.api.data", ".data");
        callKey  = cc.getProperty("core.api.call", ".call");
        convKey  = cc.getProperty("core.api.conv", ".conv");
        flatKey  = cc.getProperty("core.api.flat", ".flat");
        wrapKey  = cc.getProperty("core.api.wrap", ".wrap");
        scokKey  = cc.getProperty("core.api.scok", ".scok");
    }

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse rsp)
            throws IOException, ServletException {
        doAction(req, rsp, "retrieve", "list");
    }

    @Override
    public void doPut(HttpServletRequest req, HttpServletResponse rsp)
            throws IOException, ServletException {
        doAction(req, rsp, "update", "save");
    }

    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse rsp)
            throws IOException, ServletException {
        doAction(req, rsp, "create", "save");
    }

//  @Override
    public void doPatch(HttpServletRequest req, HttpServletResponse rsp)
            throws IOException, ServletException {
        doAction(req, rsp, "update", "save");
    }

    @Override
    public void doDelete(HttpServletRequest req, HttpServletResponse rsp)
            throws IOException, ServletException {
        doAction(req, rsp, "delete");
    }

    /**
     * 将请求转发到动作处理器
     * @param req
     * @param rsp
     * @param mts
     * @throws ServletException
     * @throws IOException
     */
    private void doAction(HttpServletRequest req, HttpServletResponse rsp, String... mts)
            throws ServletException, IOException {
        String act = ActionDriver.getCurrPath(req);
        if (act == null || act.length() == 0) {
            rsp.sendError(HttpServletResponse.SC_NOT_FOUND, "API URI can not be empty.");
            return;
        }

        ActionHelper hlpr = ActionDriver.getWorkCore(req).get(ActionHelper.class);

        // 提取 API 特有的参数
        String _dat  = req.getParameter(dataKey);
        String _cal  = req.getParameter(callKey);
        String _cnv  = req.getParameter(convKey);
        String _flt  = req.getParameter(flatKey);
        String _wap  = req.getParameter(wrapKey);
        String _sok  = req.getParameter(scokKey);

        // JSONP 回调函数名称登记, 为避免参数被占用, 总能用 .call
        if (!callKey.startsWith(".")) {
        String _cxl  = req.getParameter(".call");
        if (_cxl != null && _cxl.length( ) != 0) {
            _cal  = _cxl;
        }}
        if (_cal != null && _cal.length( ) != 0) {
            if (!_cal.matches ( "^[a-zA-Z_\\$][a-zA-Z0-9_]*$"  )  ) {
                hlpr.error400 ( "Illegal callback function name!" );
                return;
            }
            hlpr.setAttribute ( Cnst.BACK_ATTR ,  _cal);
        }

        // 将请求数据处理之后传递
        if (_dat != null && _dat.length( ) != 0) {
            Map data;
            try {
                data = Synt.declare(Data.toObject(_dat), Map.class);
            } catch (ClassCastException e) {
                hlpr.error400 ( "Can not parse value for '!data'" );
                return;
            }
            hlpr.getRequestData( )
                .putAll (  data  );
        }

        // 指定转换则启用对象模式
        if (_cnv != null && _cnv.length( ) != 0) {
            Core.getInstance().put(Cnst.OBJECT_MODE, true);
        }

        // 将请求转发到动作处理器
        act =       parseAct(act, mts);
        req.getRequestDispatcher( act)
                    .include(req, rsp);

        // 获取响应数据逐步格式化
        Map resp  = hlpr.getResponseData();
        if (resp == null) {
            return;
        }

        //** 数据转换策略 **/

        Set conv  = null;
        if (_cnv != null && _cnv.length( ) != 0) {
            try {
                conv = Synt.asTerms(_cnv );
            } catch (ClassCastException e) {
                hlpr.error400 ( "Can not parse value for '!conv'" );
                return;
            }
        } else
        if (_flt != null && _flt.length( ) != 0) {
            conv  = new HashSet(/***/);
        }

        if (conv != null) {
            convData(resp, conv, _flt);
        }

        //** 包裹返回数据 **/

        boolean wrap;
        try {
            wrap = Synt.declare(_wap, false);
        } catch (ClassCastException e) {
            hlpr.error400("Value for '!wrap' can not be case to boolean");
            return;
        }

        if (wrap) {
            wrapData(resp);
        }

        //** 状态总是 200 **/

        boolean scok;
        try {
            scok = Synt.declare(_sok, false);
        } catch (ClassCastException e) {
            hlpr.error400("Value for '!scok' can not be case to boolean");
            return;
        }

        if (scok) {
            rsp.setStatus( javax.servlet.http.HttpServletResponse.SC_OK );
        }
    }

    private String parseAct(String act, String... mts) {
        // 掐头去尾
        String acl = act.substring( 1 );
        int pos  = acl.lastIndexOf('.');
        if (pos != -1) {
            acl  = acl.substring(0,pos);
        }

        // 是否动作
        Map acx = ActionRunner.getActions();
        if (acx.containsKey( acl )) {
            return "/"+acl+".act";
        }

        String[]    ats = acl.split ( "/" );
        String        m = mts[0] ;
        String        n = ats[0] ;
        StringBuilder u = new StringBuilder();
        StringBuilder p = new StringBuilder();

        // 分解路径
        u.append(n);
        for (int i = 1; i < ats.length; i ++) {
            String  v   =   ats[i];
            if (v.startsWith("!")) {
                v = v.substring(1);

                /**
                 * 当这是最后一个参数时
                 * 将 info 加到 list 前
                 * 最后一个总是叫 id
                 * 其他外键则叫 x_id
                 */
                if (i == ats.length - 1) {
                    if ("retrieve".equals(m)) {
                        mts = new String[] {"retrieve", "info", "list"};
                    }
                    n  = /**/Cnst.ID_KEY;
                } else {
                    n += "_"+Cnst.ID_KEY;
                }

                p.append('&').append(n)
                 .append('=').append(v);
            } else {
                u.append('/').append(v);
            }
        }

        // 逐个对比
        n = u.toString( );
        for (String x : mts) {
            x = n + "/" + x ;
            if (acx.containsKey(x)) {
                x = "/" + x + ".act";
                if (0 < p.length()) {
                    x = x + p.replace(0, 1, "?");
                }
                return  x;
            }
        }

        /**
         * 但已知的动作中无法匹配时
         * 可能是使用了 AutoFilter 的原因
         * 可尝试检查当前动作或方法
         */
        if (0 < p.length()) {
            p = p.replace(0, 1, "?");
        }
        for( /**/ String mtd : mts ) {
        if (acl.endsWith("/" + mtd)) {
            return "/"+acl+".act"+ p;
        }}
        if ("update".equals(mts[0])) {
            return "/"+acl+"/update.act" + p;
        } else
        if ("create".equals(mts[0])) {
            return "/"+acl+"/create.act" + p;
        } else
        if ("delete".equals(mts[0])) {
            return "/"+acl+"/delete.act" + p;
        } else
        {
            return "/"+acl+"/retrieve.act"+p;
        }
    }

    private void wrapData(Map resp) throws ServletException {
        Map    data;
        Object doto = resp.get("data");
        if (doto == null) {
            data =  new HashMap( );
            resp.put("data", data);
        }  else {
            try {
                data = (Map) doto ;
            }   catch  (ClassCastException e) {
                throw new ServletException(e);
            }
        }
        Iterator it = resp.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry et = (Map.Entry) it.next();
            Object k = et.getKey();
            if ( ! _API_RSP.contains( k )) {
                data.put(k, et.getValue());
                it.remove();
            }
        }
    }

    private void convData(Map resp, Set<String> conv, String _evn) {
        Conv cnvr = new Conv();
        boolean     all =  conv.contains( "all2str");
        cnvr.all  = all ?  new  Conv2Str( ) : new Conv2Obj( ) ;
        cnvr.num  = all || conv.contains( "num2str") ? new Conv2Str(/**/) : cnvr.all;
        cnvr.nul  = all || conv.contains("null2str") ? new ConvNull2Str() : cnvr.all;
        cnvr.bool = conv.contains("bool2num") ? new ConvBool2Num()
                  :(conv.contains("bool2str") ? new ConvBool2Str() : new Conv2Obj());
        cnvr.date = conv.contains("date2num") ? new ConvDate2Num()
                  :(conv.contains("date2sec") ? new ConvDate2Sec() : new Conv2Obj());
        cnvr.flat = _evn != null && !"".equals(_evn) ? new Flat4Map(cnvr,_evn): null;
        resp.putAll(Synt.filter(resp , cnvr));
    }

    private static final Set _API_RSP = new HashSet();
    static {
        _API_RSP.add("ok");
        _API_RSP.add("ern");
        _API_RSP.add("err");
        _API_RSP.add("msg");
        _API_RSP.add("data");
    }

    private static class Conv implements Synt.Each {
        private Conv2Obj all;
        private Conv2Obj nul;
        private Conv2Obj num;
        private Conv2Obj bool;
        private Conv2Obj date;
        private Flat4Map flat = null;

        @Override
        public Object run(Object o, Object k, int i) {
            if (o == null) {
                return nul.conv(o);
            } else
            if (o instanceof Number ) {
                return num.conv(o);
            } else
            if (o instanceof Boolean) {
                o  =  bool.conv(o);
            } else
            if (o instanceof Date) {
                o  =  date.conv(o);
            } else

            // 向下递归, 拉平层级
            if (o instanceof Map ) {
                if (flat != null ) {
                  return flat.run ((Map ) o,  "" );
                }
                return Synt.filter((Map ) o, this);
            } else
            if (o instanceof Set ) {
                return Synt.filter((Set ) o, this);
            } else
            if (o instanceof List) {
                return Synt.filter((List) o, this);
            } else
            if (o instanceof Object[]) {
                return Synt.filter((Object[]) o, this);
            }

            return all.conv(o);
        }
    }

    private static class Flat4Map {
        private final Conv  conv;
        private final String dot;

        public Flat4Map(Conv conv, String dot) {
            this.conv = conv;
            this.dot  = dot ;
        }

        public Object run(Map o, String p) {
            Map x = new LinkedHashMap( );
            for(Object ot : o.entrySet() ) {
                Map.Entry et = (Map.Entry) ot;
                String k = et.getKey().toString();
                Object v = et.getValue();
                if (v instanceof Map) {
                    Map m = (Map) v;
                    m = ( Map ) this.run(m, p + k + dot);
                    x.putAll(m);
                } else {
                    x.put(p+ k, this.conv.run(v, k, -1));
                }
            }
            return x;
        }
    }

    private static class Conv2Obj {
        public Object conv(Object o) {
            return o;
        }
    }

    private static class Conv2Str extends Conv2Obj {
        @Override
        public Object conv(Object o) {
            if (o instanceof Number) {
                return Tool.toNumStr((Number) o);
            }
            return o.toString();
        }
    }

    private static class ConvNull2Str extends Conv2Obj {
        @Override
        public Object conv(Object o) {
            return "";
        }
    }

    private static class ConvBool2Str extends Conv2Obj {
        @Override
        public Object conv(Object o) {
            return ((Boolean) o) ? "1" : "";
        }
    }

    private static class ConvBool2Num extends Conv2Obj {
        @Override
        public Object conv(Object o) {
            return ((Boolean) o) ?  1  :  0;
        }
    }

    private static class ConvDate2Num extends Conv2Obj {
        @Override
        public Object conv(Object o) {
            return ((Date) o).getTime();
        }
    }

    private static class ConvDate2Sec extends Conv2Obj {
        @Override
        public Object conv(Object o) {
            return ((Date) o).getTime() / 1000;
        }
    }

}
