package io.github.ihongs.action.serv;

import io.github.ihongs.Cnst;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.action.ActionDriver;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.util.Synt;
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
 * 应用接口类
 *
 * <p>
 * 可将输入和输出数据按既定规则进行转换,
 * 内部采用 INCLUDE 将数据转 ActsAction.
 * </p>
 *
 * <h3>web.xml配置:</h3>
 * <pre>
 * &lt;servlet&gt;
 *   &lt;servlet-name&gt;ApisAction&lt;/servlet-name&gt;
 *   &lt;servlet-class&gt;io.github.ihongs.action.ApisAction&lt;/servlet-class&gt;
 * &lt;/servlet&gt;
 * &lt;servlet-mapping&gt;
 *   &lt;servlet-name&gt;ApisAction&lt;/servlet-name&gt;
 *   &lt;url-pattern&gt;*.api&lt;/url-pattern&gt;
 * &lt;/servlet-mapping&gt;
 * </pre>
 *
 * @author Hongs
 */
public class ApisDriver
  extends  ActionDriver
{
    private String dataKey;
    private String modeKey;

    @Override
    public void init(ServletConfig conf) throws ServletException {
        super.init(conf);

        CoreConfig cc = CoreConfig.getInstance( );
        dataKey  = cc.getProperty("core.api.data", "__data__"); // 请求数据
        modeKey  = cc.getProperty("core.api.mode", "__mode__"); // 封装模式
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse rsp)
            throws ServletException, IOException {
        String act = ActionDriver.getRecentPath(req);
        if (act == null || act.length() == 0) {
            rsp.sendError(HttpServletResponse.SC_NOT_FOUND, "URI can not be empty" );
            return;
        }
        int    dot;
        dot  = act.lastIndexOf( "." );
        act  = act.subSequence(0,dot)+Cnst.ACT_EXT;

        ActionHelper hlpr = ActionDriver.getActualCore(req).got(ActionHelper.class);
        Map reqs  =  hlpr.getRequestData();
        Object _mod = reqs.remove(modeKey);
        Object _dat = reqs.remove(dataKey);

        // 数据转换策略
        Set mode  = null;
        if (_mod != null) {
            try {
                mode = trnsMode(_mod);
            } catch (ClassCastException e) {
                hlpr.error(400, "Can not parse value for "+ modeKey );
                return;
            }
        }

        // 请求数据封装
        Map data  = null;
        if (_dat != null) {
            try {
                data = trnsData(_dat);
            } catch (ClassCastException e) {
                hlpr.error(400, "Can not parse value for "+ dataKey );
                return;
            }
        }

        // 额外请求数据
        if (data != null) {
            reqs.putAll(data);
        }

        // 转发动作处理, 获取响应数据
        req.getRequestDispatcher(act).include( req , rsp );
        Map resp  = hlpr.getResponseData();
        if (resp == null) {
            return;
        }

        // 整理响应数据
        if (mode != null) {
            if ( !  mode.isEmpty( ) )  {
                convData(resp, mode );
            }
            if (mode.contains("wrap")) {
                wrapData(resp  /**/ );
            }
            if (mode.contains("scok")) {
                rsp.setStatus( HttpServletResponse.SC_OK );
            }
        }
    }

    private static Set trnsMode(Object obj) {
        if (obj == null || "".equals(obj) ) {
            return null ;
        }
        return Synt.toSet(obj);
    }

    private static Map trnsData(Object obj) {
        if (obj == null || "".equals(obj) ) {
            return null ;
        }
        return Synt.toMap(obj);
    }

    /**
     * 将非通用键值放到 data 下
     * 通用键有: ok,ern,err,msg
     * @param resp
     * @throws javax.servlet.ServletException
     */
    public static void wrapData(Map resp) throws ServletException {
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
            if ( ! API_RSP.contains( k ) ) {
                data.put(k, et.getValue());
                it.remove();
            }
        }
    }

    /**
     * 将响应数据按规则进行转换
     * 规则有: all2str,num2str,null2str,bool2num,bool2str,date2num,date2sec,flat.map,flat_map
     * @param resp
     * @param conv
     */
    public static void convData(Map resp, Set<String> conv) {
        // 如果没指定转换方法则不需要处理
        Set cnvs = new HashSet (API_CNV);
            cnvs.retainAll(conv);
        if (cnvs.isEmpty()) {
            return;
        }

        Conv cnvr = new Conv();
        boolean     all =  conv.contains( "all2str");
        cnvr.all  = all ?  new  Conv2Str( ) : new Conv2Obj( ) ;
        cnvr.num  = all || conv.contains( "num2str") ? new Conv2Str(/**/) : cnvr.all;
        cnvr.nul  = all || conv.contains("null2str") ? new ConvNull2Str() : cnvr.all;
        cnvr.bool = conv.contains("bool2num") ? new ConvBool2Num()
                  :(conv.contains("bool2str") ? new ConvBool2Str() : new Conv2Obj());
        cnvr.date = conv.contains("date2num") ? new ConvDate2Num()
                  :(conv.contains("date2sec") ? new ConvDate2Sec() : new Conv2Obj());
        cnvr.flat = conv.contains("flat.map") ? new Flat4Map(cnvr, ".")
                  :(conv.contains("flat_map") ? new Flat4Map(cnvr, "_")
                  : null);
        resp.putAll(Synt.filter(resp , cnvr));
    }

    private static final Set API_RSP = new HashSet();
    static {
        API_RSP.add("ok");
        API_RSP.add("ern");
        API_RSP.add("err");
        API_RSP.add("msg");
        API_RSP.add("data");
    }

    private static final Set API_CNV = new HashSet();
    static {
        API_CNV.add("all2str");
        API_CNV.add("num2str");
        API_CNV.add("null2str");
        API_CNV.add("bool2str");
        API_CNV.add("bool2num");
        API_CNV.add("date2num");
        API_CNV.add("date2sec");
        API_CNV.add("flat.map");
        API_CNV.add("flat_map");
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
                return Synt.asString((Number) o);
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
