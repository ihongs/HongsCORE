package app.hongs.action;

import app.hongs.Core;
import app.hongs.HongsException;
import app.hongs.HongsExpedient;
import app.hongs.util.Dict;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;

/**
 * 选项补充助手
 * @author Hong
 */
public class SelectHelper {

    private final static  Pattern  HOSTP = Pattern.compile( "^(\\w+:)?//" );
    private final static  Pattern  FULLP = Pattern.compile("^\\$\\{?FULL_");

    private final Map<String, Map> enums;
    private final Set<String>      times;
    private final Set<String>      files;

    private String _host = null;
    private String _path = null;

    public SelectHelper() {
        enums = new LinkedHashMap();
        times = new LinkedHashSet();
        files = new LinkedHashSet();
    }

    public SelectHelper addEnum(String code, Map<String, String> opts) {
        enums.put(code, opts);
        return this;
    }

    public SelectHelper addEnum(String code, String[][] arrs) {
        Map<String, String> opts = new LinkedHashMap();
        int i = 0;
        for(String[] arr : arrs) {
            if (arr.length == 1) {
                String j = String.valueOf(++i);
                opts.put(j /**/, arr[0]);
            } else {
                opts.put(arr[0], arr[1]);
            }
        }
        return addEnum(code, opts);
    }

    public SelectHelper addEnum(String code, String ... args) {
        Map<String, String> opts = new LinkedHashMap();
        int i = 0;
        for(String   arg : args) {
            String[] arr = arg.split("::" , 2); // 拆分
            if (arr.length == 1) {
                String j = String.valueOf(++i);
                opts.put(j /**/, arr[0]);
            } else {
                opts.put(arr[0], arr[1]);
            }
        }
        return addEnum(code, opts);
    }

    /**
     * 添加日期时间字段, 会将值转换为标准时间戳
     * @param code
     * @return
     */
    public SelectHelper addTime(String... code) {
        times.addAll(Arrays.asList(code));
        return  this;
    }

    /**
     * 添加文件路径字段, 会将对值补全为绝对链接
     * @param code
     * @return
     */
    public SelectHelper addFile(String... code) {
        files.addAll(Arrays.asList(code));
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
        FormSet cnf = FormSet.getInstance(conf);
        Map map = cnf.getForm(form);
        return addItemsByForm(conf , map );
    }

    public SelectHelper addItemsByForm(String conf, Map map) throws HongsException {
        FormSet dfs = FormSet.getInstance("default");
        Map tps = dfs.getEnum("__types__");

        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry et = (Map.Entry)it.next();
            Map       mt = (Map ) et.getValue();
            String  name = (String) et.getKey();
            String  type = (String) mt.get("__type__");
                    type = (String)tps.get(   type   ); // 类型别名转换

            if ("enum".equals(type)) {
                String xonf = (String) mt.get("conf");
                String xame = (String) mt.get("enum");
                if (null == xonf || "".equals( xonf )) xonf = conf;
                if (null == xame || "".equals( xame )) xame = name;
                Map xnum = FormSet.getInstance(xonf).getEnumTranslated(xame);
                enums.put(name , xnum);
            } else
            if ("date".equals(type)) {
                String typa = (String) mt.get("type");
                if (! "time"     .equals(typa )
                &&  ! "timestamp".equals(typa)) {
                    times.add(name);
                }
            } else
            if ("file".equals(type)) {
                String href = (String) mt.get("href");
                if (href != null
                && !HOSTP.matcher(href).find( )
                && !FULLP.matcher(href).find()) {
                    files.add(name);
                }
            }
        }

        return this;
    }

    /**
     * 填充
     * @param values 返回数据
     * @param action 1 注入data, 2 添加text, 4 添加time, 8 添加link
     * @throws HongsException
     */
    public void select(Map values, short action) throws HongsException {
        boolean withEnum = 1 == (1 & action);
        boolean withText = 2 == (2 & action);
        boolean withTime = 4 == (4 & action);
        boolean withLink = 8 == (8 & action);

        if (withEnum) {
            Map data = (Map ) values.get("enum");
            if (data == null) {
                data =  new LinkedHashMap();
                values.put("enum", data );
            }
                injectData( data , enums);
        }

        if (withText || withTime || withLink) {
            if (values.containsKey("info")) {
                     Map  info = (Map ) values.get("info");
                if (withText) injectText(info, enums);
                if (withTime) injectTime(info, times);
                if (withLink) injectLink(info, files);
            }

            if (values.containsKey("list")) {
                List<Map> list = (List) values.get("list");
                for (Map  info :  list) {
                if (withText) injectText(info, enums);
                if (withTime) injectTime(info, times);
                if (withLink) injectLink(info, files);
                }
            }
        }
    }

    public void injectData(Map data) throws HongsException {
        injectData(data, enums);
    }

    public void injectText(Map info) throws HongsException {
        injectText(info, enums);
    }

    public void injectLink(Map info) throws HongsException {
        injectLink(info, files);
    }

    private void injectData(Map data, Map maps) throws HongsException {
        Iterator it = maps.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry et = (Map.Entry) it.next();
            String   key = (String)  et.getKey();
            Map      map = (Map)   et.getValue();
            List     lst = new ArrayList();
//          Dict.setParam(data, lst, key );
            data.put(key, lst); // 不要放太深层级

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
            throw new HongsExpedient.Common("Can not find http servlet request");
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
            throw new HongsExpedient.Common("Can not find http servlet request");
        }

        return r.getContextPath() + "/";
    }

}
