package app.hongs.action;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.CoreLogger;
import app.hongs.HongsException;
import app.hongs.HongsExemption;
import app.hongs.dh.MergeMore;
import app.hongs.util.Data;
import app.hongs.util.Dict;
import app.hongs.util.Synt;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Collection;
import java.util.LinkedList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Date;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;

/**
 * 选项补充助手
 * 可针对表单的 enum,form,fork,file,date 类字段进行数据补全和关联
 * @author Hong
 */
public class SelectHelper {

    public  final static short ENUM = (short)  1;
    public  final static short TEXT = (short)  2;
    public  final static short FORM = (short)  4;
    public  final static short FORK = (short)  8;
    public  final static short LINK = (short) 16;
    public  final static short TIME = (short) 32;

    private final static  Pattern  HOSTP = Pattern.compile( "^(\\w+:)?//" );
    private final static  Pattern  FULLP = Pattern.compile("^\\$\\{?FULL_");

    private final Map<String, Map> enums;
    private final Map<String, Map> forms;
    private final Map<String, Map> forks;
    private final Set<String     > files;
    private final Set<String     > dates;

    private String _host = null;
    private String _path = null;

    public SelectHelper() {
        enums = new LinkedHashMap();
        forms = new LinkedHashMap();
        forks = new LinkedHashMap();
        files = new LinkedHashSet();
        dates = new LinkedHashSet();
    }

    public SelectHelper addEnum(String code, Map vals) {
        enums.put(code, vals);
        return this;
    }

    public SelectHelper addForm(String code, Map flds) {
        forms.put(code, flds);
        return this;
    }

    public SelectHelper addFork(String code, Map prms) {
        forks.put(code, prms);
        return this;
    }

    /**
     * 添加文件路径字段, 会将值补全为完整的链接
     * @param code
     * @return
     */
    public SelectHelper addFile(String... code) {
        files.addAll(Arrays.asList(code));
        return  this;
    }

    /**
     * 添加日期时间字段, 会将值转换为标准时间戳
     * @param code
     * @return
     */
    public SelectHelper addDate(String... code) {
        dates.addAll(Arrays.asList(code));
        return  this;
    }

    /**
     * 设置文件链接域名
     * @param host 格式 scheme://domain[:port]
     * @return
     */
    public SelectHelper setHost(String host) {
        _host = host;
        return  this;
    }

    /**
     * 设置文件链接路径
     * @param path 格式 /service/context/path/
     * @return
     */
    public SelectHelper setPath(String path) {
        _path = path;
        return  this;
    }

    public SelectHelper addItemsByForm(String conf, String form) throws HongsException {
        return addItemsByForm(FormSet.getInstance(conf).getForm(form));
    }

    public SelectHelper addItemsByForm(Map map) throws HongsException {
        Map ts = FormSet.getInstance("default").getEnum( "__types__" );
        String conf = Dict.getValue ( map , "default" , "@" , "conf" );
        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry et = (Map.Entry)it.next();
            Map       mt = (Map ) et.getValue();
            String  name = (String) et.getKey();
            String  type = (String) mt.get("__type__");
                    type = (String) ts.get(   type   ); // 类型别名转换

            if ("enum".equals(type)) {
                String xonf = (String) mt.get("conf");
                String xame = (String) mt.get("enum");
                if (null == xonf || "".equals( xonf )) xonf = conf;
                if (null == xame || "".equals( xame )) xame = name;
                Map xnum = FormSet.getInstance(xonf).getEnumTranslated(xame);
                enums.put(name , xnum);
            } else
            if ("form".equals(type)) {
                String xonf = (String) mt.get("conf");
                String xame = (String) mt.get("enum");
                if (null == xonf || "".equals( xonf)) xonf = conf;
                if (null == xame || "".equals( xame)) xame = name;
                Map xnum = FormSet.getInstance(xonf).getForm/*Origin*/(xame);
                forms.put(name , xnum);
            } else
            if ("fork".equals(type)) {
                Map xnum = new HashMap(mt);
                if (! mt.containsKey("data-at" )
                &&  ! mt.containsKey("data-al")) {
                if (! mt.containsKey("form")) {
                    xnum.put("form" , name.replace( "_id" , "" ));
                }
                if (! mt.containsKey("conf")) {
                    xnum.put("conf" , conf);
                }
                }
                forks.put(name , xnum);
            } else
            if ("file".equals(type)) {
                String href = (String) mt.get("href");
                if (href != null
                && !HOSTP.matcher(href).find( )
                && !FULLP.matcher(href).find()) {
                    files.add(name);
                }
            } else
            if ("date".equals(type)) {
                String typa = (String) mt.get("type");
                if (! "time"     .equals(typa )
                &&  ! "timestamp".equals(typa)) {
                    dates.add(name);
                }
            }
        }

        return this;
    }

    /**
     * 数据填充
     * @param values 返回数据
     * @param action 填充规则, 使用常量 ENUM,TEXT 等, 可用或运算传多个值
     */
    public void select(Map values, short action) {
        boolean withEnum = ENUM == (ENUM&action);
        boolean withText = TEXT == (TEXT&action);
        boolean withForm = FORM == (FORM&action);
        boolean withFork = FORK == (FORK&action);
        boolean withLink = LINK == (LINK&action);
        boolean withTime = TIME == (TIME&action);

        // 附带枚举数据
        if (withEnum) {
            /**/ Map  data = (Map ) values.get("enum");
            if (data == null) {
                data  = new LinkedHashMap();
                values.put( "enum" , data );
            }   injectData(  data  , enums);
        }

        // 补全额外数据
        if (withText || withTime || withLink) {
            /**/ Map  info = (Map ) values.get("info");
            List<Map> list = (List) values.get("list");
            if (info != null) {
                if (withText) injectText(info , enums);
                if (withTime) injectTime(info , dates);
                if (withLink) injectLink(info , files);
            }
            if (list != null) for ( Map  item : list ) {
                if (withText) injectText(item , enums);
                if (withTime) injectTime(item , dates);
                if (withLink) injectLink(item , files);
            }
        }

        // 补全关联数据
        if (withFork) {
            /**/ Map  info = (Map ) values.get("info");
            List<Map> list = (List) values.get("list");
            if (info != null) injectFork(info);
            if (list != null) injectFork(list);
        }

        // 填充下级表单
        // 为规避因循环依赖导致故障
        // 限制填充规则为仅向下一层
        if (withForm) {
            inject(values , ( short ) (action - FORM));
        }
    }

    /**
     * 深度补充
     * @param values 返回数据
     * @param action 填充规则, 使用常量 ENUM,TEXT 等, 可用或运算传多个值
     */
    public void inject(Map values, short action) {
        List<Map> list = new LinkedList();
        if (values.containsKey("info")) {
            list.add   ((Map ) values.get("info"));
        }
        if (values.containsKey("list")) {
            list.addAll((List) values.get("list"));
        }

        Map xnum = null;
        if (1 == (1 & action)) {
            xnum  = (Map) values.get("enum");
            if (xnum == null) {
                xnum  = new LinkedHashMap( );
                values.put("enum", xnum );
            }
        }

        // 数据映射整理
        Map<String, List> maps = new HashMap();
        for(String fn : forms.keySet()) {
            maps.put( fn , new LinkedList( ) );
        }
        for( Map info : list ) {
        for(String fn : forms.keySet()) {
            Object fv = info.get(fn);
            if (fv instanceof List ) {
                maps.get(fn).addAll((List) fv);
            } else {
                maps.get(fn).add(fv);
            }
        }}

        // 向下递归补充
        for(Map.Entry<String, Map> et : forms.entrySet()) {
            String  fn = et.getKey(  );
            Map fields = et.getValue();
            Map valuez = new HashMap();
            Map anum   = new HashMap();
            valuez.put( "enum", anum );
            valuez.put( "list", maps.get(fn) );

            try {
                new SelectHelper()
                    . addItemsByForm( fields )
                    . select( valuez, action );
            } catch (HongsException e) {
                throw e.toExemption( );
            }

            // 将枚举向上并
            if (xnum != null && !anum.isEmpty())
            for(Object ot2 : anum.entrySet( )) {
                Map.Entry et2 = (Map.Entry) ot2;
                Object fn2 = et2.getKey(  );
                Object fv2 = et2.getValue();
                xnum.put(fn +"."+ fn2, fv2);
            }
        }
    }

    public void injectData(Map data) {
        injectData(data, enums);
    }

    public void injectText(Map info) {
        injectText(info, enums);
    }

    public void injectTime(Map info) {
        injectLink(info, dates);
    }

    public void injectLink(Map info) {
        injectLink(info, files);
    }

    public void injectFork(Map info) {
        injectFork(Synt.listOf(info));
    }

    public void injectText(List<Map> list) {
        for (Map info : list) {
            injectText( info, enums );
        }
    }

    public void injectTime(List<Map> list) {
        for (Map info : list) {
            injectTime(info, dates );
        }
    }

    public void injectLink(List<Map> list) {
        for (Map info : list) {
            injectLink( info, files );
        }
    }

    public void injectFork(List<Map> list) {
        MergeMore    mm = new  MergeMore  (  list  );
        ActionHelper ah = ActionHelper.newInstance();
        ah.setContextData(Synt.mapOf(
            Cnst.ORIGIN_ATTR, Core.ACTION_NAME.get()
        ));

        for(Map.Entry et : forks.entrySet()) {
            Map    mt = (Map) et.getValue( );
            String fn = (String) et.getKey();

            String uk = (String) mt.get("data-uk"); // 外键字段
            if (null == uk || "".equals(uk)) {
                   uk = fn ;
            }

            // 建立映射
            Map<Object, List> ms = mm.mapped( uk );
            if (ms.isEmpty()) {
                continue;
            }

            String vk = (String) mt.get("data-vk"); // 关联字段
            String tk = (String) mt.get("data-tk"); // 名称字段
            String ak = (String) mt.get("data-ak"); // 数据放入此下
            String at = (String) mt.get("data-at"); // 关联动作路径
            if (null == ak || "".equals(ak)) {
                if (fn.endsWith("_id")) {
                    int  ln = fn.length()-3;
                    ak = fn.substring(0,ln);
                } else {
                    ak = fn + "_fork";
                }
            }
            if (null == at || "".equals(at)) {
                String c = (String) mt.get("conf");
                String f = (String) mt.get("form");
                at  =  c + "/" + f + "/search";
            }

            // 查询结构
            String ap = null;
            String aq = null;
            Map rd,sd ;
            Set rb;
            int ps;
            ps = at.indexOf('?');
            if (ps > -1) {
                aq = at.substring(1 + ps).trim();
                at = at.substring(0 , ps).trim();
            }
            ps = at.indexOf('!');
            if (ps > -1) {
                ap = at.substring(1 + ps).trim();
                at = at.substring(0 , ps).trim();
            }
            if (null != ap && !"".equals(ap)) {
                if (ActionRunner.getActions()
                            .containsKey(ap)) {
                    at = ap ; // 自动行为方法可能被定制开发
                } else {
                    ap = ap + Cnst.ACT_EXT; // 别忘了后缀名
                    ah.setAttribute(Cnst.ACTION_ATTR, ap );
                }
            }
            if (null != aq && !"".equals(aq)) {
                if (aq.startsWith("{") && aq.endsWith("}")) {
                    rd = (  Map  ) Data.toObject(aq);
                } else {
                    rd = ActionHelper.parseQuery(aq);
                }
                if (!rd.containsKey(Cnst.RB_KEY)) {
                    rd.put(Cnst.RB_KEY, "-" );
                }
                if (!rd.containsKey(Cnst.RN_KEY)) {
                    rd.put(Cnst.RN_KEY,  0  );
                }
            } else
            if (null != vk && !"".equals(vk )
            &&  null != tk && !"".equals(tk)) {
                rd = new HashMap();
                rb = new HashSet();
                rb.add( vk);
                rb.add( tk);
                rd.put(Cnst.RB_KEY, rb);
                rd.put(Cnst.RN_KEY, 0 );
            } else {
                rd = new HashMap();
                rb = new HashSet();
                rb.add("-");
                rd.put(Cnst.RB_KEY, rb);
                rd.put(Cnst.RN_KEY, 0 );
            }

            // 关联约束
            // 没有指定 vk 时与 id 进行关联
            if (null == vk || "".equals(vk)) {
                vk = Cnst . ID_KEY ;
            }
            rd.put(vk, ms.keySet());

            // 获取结果
            // 关联出错应在测试期发现并解决
            // 故仅记录到日志而无需对外抛出
            ah.setRequestData( rd );
            try {
                if (rd.containsKey(Cnst.AB_KEY)) {
                    new ActionRunner(ah, at).doAction();
                } else {
                    new ActionRunner(ah, at).doInvoke();
                }
            } catch (HongsException|HongsExemption ex ) {
                CoreLogger.error( ex );
                continue;
            }
            List<Map> ls;
            sd = ah.getResponseData( );
            ls = (List) sd.get("list");
            if (null == ls) {
                continue;
            }

            // 整合数据
            boolean rp = Synt.declare(mt.get("__repeated__"), false);
            if (rp) {
                mm.append(ls, ms, vk, ak);
            } else {
                mm.extend(ls, ms, vk, ak);
            }
        }
    }

    private void injectData(Map data, Map maps) {
        Iterator it = maps.entrySet( ).iterator( );
        while (it.hasNext()) {
            Map.Entry et = (Map.Entry) it.next ( );
            Map    map = (Map) et.getValue();
            Object key = /***/ et.getKey(  );
            List   lst = new ArrayList();

            Iterator i = map.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry e = (Map.Entry) i.next();
                Object k  = e.getKey(  );
                Object v  = e.getValue();
                List a = new ArrayList();
                a.add( k );
                a.add( v );
                lst.add(a);
            }

            data.put(key, lst);
        }
    }

    private void injectText(Map info, Map maps) {
        Iterator it = maps.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry et = (Map.Entry) it.next();
            String   key = (String)  et.getKey();
            Map      map = (Map)   et.getValue();
            Object   val = Dict.getParam(info, key);

            if (val instanceof Collection) {
                // 预置一个空列表, 规避无值导致客户端取文本节点出错
                Dict.setParam(info, new ArrayList(), key + "_text");
                for (Object vxl : (Collection) val) {
                    vxl = codeToText(vxl, map);
                    Dict.setParam(info, vxl, key + "_text.");
                }
            } else {
                    val = codeToText(val, map);
                    Dict.setParam(info, val, key + "_text" );
            }
        }
    }

    private void injectTime(Map info, Set keys) {
        Iterator it = keys.iterator();
        while (it.hasNext()) {
            String  key = (String) it.next();
            Object  val = Dict.getParam(info, key);

            if (val instanceof Collection) {
                // 预置一个空列表, 规避无值导致客户端取文本节点出错
                Dict.setParam(info, new ArrayList(), key + "_time");
                for (Object vxl : (Collection) val) {
                    vxl = dataToTime(vxl);
                    Dict.setParam(info, vxl, key + "_time.");
                }
            } else {
                    val = dataToTime(val);
                    Dict.setParam(info, val, key + "_time" );
            }
        }
    }

    private void injectLink(Map info, Set keys) {
        // 默认从当前请求中提取主机和路径前缀
        if (_host == null) _host = getHost();
        if (_path == null) _path = getPath();

        Iterator it = keys.iterator();
        while (it.hasNext()) {
            String  key = (String) it.next();
            Object  val = Dict.getParam(info, key);

            if (val instanceof Collection) {
                // 预置一个空列表, 规避无值导致客户端取文本节点出错
                Dict.setParam(info, new ArrayList(), key + "_link");
                for (Object vxl : (Collection) val) {
                    vxl = hrefToLink(vxl);
                    Dict.setParam(info, vxl, key + "_link.");
                }
            } else {
                    val = hrefToLink(val);
                    Dict.setParam(info, val, key + "_link" );
            }
        }
    }

    /**
     * 将枚举代号转换为对应文本
     * 空值和空串不处理
     * 星号总是代表其他
     * @param map
     * @param val
     * @return
     */
    private Object codeToText(Object val, Map map) {
        if (null == val || "".equals(val)) {
            return  val;
        }

        Object vxl;
        vxl = map.get(val);
        if (null != vxl) {
            return  vxl;
        }
        vxl = map.get("*");
        if (null != vxl) {
            return  vxl;
        }

        return val;
    }

    /**
     * 将字段值转换为对应时间戳
     * @param val
     * @return
     */
    private Object dataToTime(Object val ) {
        if (null == val || "".equals(val)) {
            return  val;
        }

        if (val instanceof Date) {
            return ((Date) val ).getTime();
        } else
        if (val instanceof Long) {
            return  val;
        } else
        {
            return  0  ;
        }
    }

    /**
     * 补全相对路径的域名和前缀
     * @param val
     * @return
     */
    private Object hrefToLink(Object val ) {
        if (null == val || "".equals(val)) {
            return  val;
        }

        String url  =  val.toString (   );
        if (HOSTP.matcher (url).find(   )) {
            return  val;
        } else
        if (url.startsWith("/")) {
            return _host + url;
        } else
        {
            return _host +_path + url;
        }
    }

    private static String getHost() {
        HttpServletRequest r = Core.getInstance(ActionHelper.class).getRequest();
        if ( r == null ) {
            throw new HongsExemption.Common("Can not find http servlet request");
        }

        int    port;
        String host;
        port = r.getServerPort();
        host = r.getServerName();
        if (port == 80 && port == 443) {
            return r.getScheme() +"://"+ host ;
        } else {
            return r.getScheme() +"://"+ host +":"+ port ;
        }
    }

    private static String getPath() {
        HttpServletRequest r = Core.getInstance(ActionHelper.class).getRequest();
        if ( r == null ) {
            throw new HongsExemption.Common("Can not find http servlet request");
        }

        return r.getContextPath() + "/";
    }

}
