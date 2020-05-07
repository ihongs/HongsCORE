package io.github.ihongs.action.serv;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.HongsCause;
import io.github.ihongs.HongsException;
import io.github.ihongs.HongsExemption;
import io.github.ihongs.action.ActionDriver;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.cmdlet.CmdletHelper;
import io.github.ihongs.cmdlet.CmdletRunner;
import io.github.ihongs.dh.MergeMore;
import io.github.ihongs.util.Dawn;
import io.github.ihongs.util.Synt;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 通用聚合动作
 * 可一次调用多个动作
 * 批量执行后返回数据
 * 请求格式举例:
 * a.KEY=ACTION&d.KEY=PARAMS&foo=bar
 * PARAMS 为 JSON 或 URLEncoded 格式
 * 其他参数为共用参数
 * @author Hongs
 */
@Action("common/more")
public class MoreAction {

    @Action("__main__")
    public void more(ActionHelper helper) {
        helper.reply(new HashMap());

        HttpServletRequest  req = helper.getRequest( );
        HttpServletResponse rsp = helper.getResponse();
        Map    re0 = helper.getRequestData( );
        Map    rs0 = helper.getResponseData();
        Core  core = Core.getInstance();
        Wrap  wrap = new Wrap( helper );
        String act ;

            act = Core.ACTION_NAME.get();
            core.put(ActionHelper.class.getName(),  wrap );
        try {
            /* 旧的平行逻辑 */
            mass(wrap, req, rsp, re0, rs0);

            /* 新的关联逻辑 */
            more(wrap, req, rsp, re0, rs0, null, null, 0 );
        } finally {
            Core.ACTION_NAME.set ( act );
            core.put(ActionHelper.class.getName(), helper);
        }

        helper.reply(rs0);
    }

    private void mass(ActionHelper wrap, HttpServletRequest req, HttpServletResponse rsp, Map re0, Map rs0) {
        Map<String, String> acs = Synt.asMap(re0.remove("a"));
        Map<String, Object> das = Synt.asMap(re0.remove("d"));
        if (acs == null) {
            return;
        //  acs = new HashMap();
        }
        if (das == null) {
            das = new HashMap();
        }

        String uri;
        String key;
        Map    re1;
        Map    rs1;

        for(Map.Entry<String, String> et : acs.entrySet()) {
            key = et.getKey(  );
            uri = et.getValue();
            uri = uri + Cnst.ACT_EXT;
            re1 = new HashMap( re0 );
            re1.putAll( data ( das.get (key) ) );

            Core.ACTION_NAME.set ( uri );
            wrap.setRequestData  ( re1 );
            wrap.setAttribute(Cnst.ACTION_ATTR, null);
            wrap.setAttribute(Cnst.ORIGIN_ATTR, null);
            wrap.reply((Map) null );
            eval( wrap, uri , req , rsp);
            rs1 = wrap.getResponseData();
            rs0 . put ( key , rs1 );

            if (rs1 != null
            && !rs0.containsKey("ok")
            && !Synt.declare(rs1.get("ok"), true)) {
                rs0.put("ok" , false);
                if (rs1.containsKey("ern")) {
                    rs0.put("ern", rs1.get("ern"));
                }
                if (rs1.containsKey("err")) {
                    rs0.put("err", rs1.get("err"));
                }
                if (rs1.containsKey("msg")) {
                    rs0.put("msg", rs1.get("msg"));
                }
            }
        }
    }

    private void more(ActionHelper wrap, HttpServletRequest req, HttpServletResponse rsp, Map re0, Map rs0, MergeMore meg, String sub, int lev) {
        String  uri;
        String  key;
        String  col;
        Map     re1;
        Map     rs1;

        try {
            uri = (String) re0.remove("at");
            key = (String) re0.remove("on");
            re1 = (Map   ) re0.remove("in");
        }
        catch (ClassCastException ex) {
            return;
        }

        if (uri != null) {
            if (meg != null) {
                if (key != null) {
                    int  p  = key.indexOf  (':');
                    if ( p >= 0) {
                        col = key.substring(1+p);
                        key = key.substring(0,p);
                    } else {
                        col = Cnst.ID_KEY;
                    }
                } else {
                        col = Cnst.ID_KEY;
                        key = sub +"_"+ Cnst.ID_KEY;
                }

                // 映射参数
                Map<Object,List> map ;
                map = meg.mapped(key);
                if (map.isEmpty()) {
                    return;
                }

                // 请求参数
                if (re1 == null) {
                    re1  = new HashMap( );
                }
                re1.put(col,map.keySet());

                // 执行请求
                Core.ACTION_NAME.set ( uri );
                wrap.setRequestData  ( re1 );
                wrap.setAttribute(Cnst.ACTION_ATTR, null);
                wrap.setAttribute(Cnst.ORIGIN_ATTR, null);
                wrap.reply((Map) null );
                eval( wrap, uri , req , rsp);
                rs1 = wrap.getResponseData();

                // 获取列表
               List list  = (List) rs1.get("list");
                if (list ==  null) {
                Map info  = (Map ) rs1.get("info");
                if (info !=  null) {
                    list  =  Synt. listOf ( info );
                } else {
                    return;
                }}

                // 进行关联
                meg.append(list, map, col, sub);

                // 补全空白
                List sup = new ArrayList(0);
                for (Map.Entry<Object,List> lr : map.entrySet()) {
                    List<Map> lst = lr.getValue();
                    for (Map  row : lst) {
                        row.put(sub,sup);
                    }
                }

                // 下级关联
                meg = new MergeMore(list);
            } else {
                // 请求参数
                if (re1 == null) {
                    re1  = new HashMap( );
                }

                // 执行请求
                Core.ACTION_NAME.set ( uri );
                wrap.setRequestData  ( re1 );
                wrap.setAttribute(Cnst.ACTION_ATTR, null);
                wrap.setAttribute(Cnst.ORIGIN_ATTR, null);
                wrap.reply((Map) null );
                eval( wrap, uri , req , rsp);
                rs1 = wrap.getResponseData();

                // 获取列表
               List list  = (List) rs1.get("list");
                if (list ==  null) {
                Map info  = (Map ) rs1.get("info");
                if (info !=  null) {
                    list  =  Synt. listOf ( info );
                } else {
                    return;
                }}

                // 下级关联
                meg = new MergeMore(list);

                if (sub != null) {
                    rs0.put(sub, rs1);
                } else {
                    rs0.putAll ( rs1);
                }
            }
        } else
        if (lev == 0 ) {
            rs1 = rs0;
        } else
        {
            return;
        }

        /* 下级数据 */

        lev ++;
        for(Object ot : re0.entrySet()) {
            Map.Entry  et = (Map.Entry) ot;
            Object k = et.getKey  ();
            Object v = et.getValue();
            if (k instanceof String && v instanceof Map) {
                sub = (String) k;
                re1 = (Map   ) v;
                more(wrap, req, rsp, re1, rs1, meg, sub, lev);
            }
        }
    }

    @Action("eval")
    public void eval(ActionHelper helper) throws HongsException {
        CoreConfig          cnf = CoreConfig.getInstance();
        HttpServletRequest  req = helper.getRequest( );
        HttpServletResponse rsp = helper.getResponse();

        // 许可及IP白名单
        boolean sw  = cnf.getProperty( "core.eval.more.enable" , false);
        String  ia  = cnf.getProperty( "core.eval.more.allows" );
        String  ip  = ActionDriver.getClientAddr (req);
        Set     ias = Synt.toTerms( ia );
        if (ias == null || ias.isEmpty()) {
            ias =  new  HashSet();
            ias.add(       "::1"       );
            ias.add(    "127.0.0.1"    );
            ias.add( "0:0:0:0:0:0:0:1" );
        }
        if (! sw ) {
            throw new HongsException(400, "Illegal request!");
        }
        if (! ias.contains(ip) ) {
            throw new HongsException(400, "Illegal request.");
        }

        Map    map = helper.getRequestData();
        String act = Core.ACTION_NAME.get( );
        String uri = (String)map.get("act" );

        // 从参数提取参数
        helper.setRequestData(data(map.get("request")));
        helper.setContextData(data(map.get("context")));
        helper.setSessionData(data(map.get("session")));
        helper.setCookiesData(data(map.get("cookies")));

        try {
            Core.ACTION_NAME.set(uri);
            eval(helper, uri,req,rsp);
        } finally {
            Core.ACTION_NAME.set(act);
        }
    }

    @Action("exec")
    public void exec(ActionHelper helper) throws HongsException {
        CoreConfig          cnf = CoreConfig.getInstance();
        HttpServletRequest  req = helper.getRequest( );
        HttpServletResponse rsp = helper.getResponse();

        // 许可及IP白名单
        boolean sw  = cnf.getProperty( "core.exec.more.enable" , false);
        String  ia  = cnf.getProperty( "core.exec.more.allows" );
        String  ip  = ActionDriver.getClientAddr (req);
        Set     ias = Synt.toTerms( ia );
        if (ias == null || ias.isEmpty()) {
            ias =  new  HashSet();
            ias.add(       "::1"       );
            ias.add(    "127.0.0.1"    );
            ias.add( "0:0:0:0:0:0:0:1" );
        }
        if (! sw ) {
            throw new HongsException(400, "Illegal request!");
        }
        if (! ias.contains(ip) ) {
            throw new HongsException(400, "Illegal request.");
        }

        Map    map = helper.getRequestData();
        String act = Core.ACTION_NAME.get( );
        String cmd = (String)map.get("cmd" );

        try {
            Core.ACTION_NAME.set(cmd);
            exec(helper, cmd,req,rsp);
        } finally {
            Core.ACTION_NAME.set(act);
        }
    }

    private void eval(ActionHelper helper, String act,
            HttpServletRequest req, HttpServletResponse rsp) {
        try {
            req.getRequestDispatcher("/" + act + Cnst.ACT_EXT).include(req, rsp);
        } catch (ServletException ex) {
            if (ex.getCause() instanceof HongsCause) {
                HongsCause  ez = ( HongsCause ) ex.getCause( );
                String en = Integer.toHexString(ez.getErrno());
                Map map = new HashMap();
                map.put("ok" ,  false );
                map.put("ern", "Ex"+en);
                map.put("err", ez.getMessage());
                map.put("msg", ez.getLocalizedMessage());
                helper.reply(map);
            } else {
                Map map = new HashMap();
                map.put("ok" ,  false );
                map.put("ern", "Er500");
                map.put("err", ex.getMessage());
                map.put("msg", ex.getLocalizedMessage());
                helper.reply(map);
            }
        } catch (IOException ex) {
                Map map = new HashMap();
                map.put("ok" ,  false );
                map.put("ern", "Er500");
                map.put("err", ex.getMessage());
                map.put("msg", ex.getLocalizedMessage());
                helper.reply(map);
        }
    }

    private void exec(ActionHelper helper, String cmd,
            HttpServletRequest req, HttpServletResponse rsp) {
        // 组织参数
        String[] args;
        List opts = Synt.asList(helper.getRequestData().get("args"));
        if (opts == null || opts.isEmpty()) {
            args = new String[1 /* no args */ ];
        } else {
            args = new String[1 + opts.size() ];
                int  i = 1 ;
            for(Object opt : opts) {
                args[i ++] = Synt.asString(opt);
            }
        }
        args[0] = cmd;

        try {
            PrintStream out = new PrintStream(rsp.getOutputStream(), true);
            rsp.setContentType("text/plain" );
            rsp.setCharacterEncoding("utf-8");

            try {
                CmdletHelper.ENV.set((byte)1);
                CmdletHelper.ERR.set(out);
                CmdletHelper.OUT.set(out);

                CmdletRunner.exec( args );
            }
            catch ( Error | Exception e ) {
                out.print( "ERROR: "+ e.getMessage() );
            }
            finally {
                CmdletHelper.OUT.remove();
                CmdletHelper.ERR.remove();
                CmdletHelper.ENV.remove();
            }
        }
        catch (IOException e) {
            throw new HongsExemption( e );
        }
    }

    private Map data(Object obj) {
        if (obj == null || "".equals(obj)) {
            return new HashMap();
        }
        if (obj instanceof Map ) {
            return ( Map ) obj ;
        }
        String str = Synt.declare(obj, "");
        Map map;
        if (str.startsWith("{") && str.endsWith("}")) {
            map = (  Map  ) Dawn.toObject(str);
        } else {
            map = ActionHelper.parseQuery(str);
        }
        return map;
    }

    private static class Wrap extends ActionHelper {

        public Wrap(ActionHelper helper) {
            super(helper.getRequest(), helper.getResponse());
        }

        @Override
        public void responed() {
            // Nothing to do
        }

    }

}
