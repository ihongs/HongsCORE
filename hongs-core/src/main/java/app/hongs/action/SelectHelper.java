package app.hongs.action;

import app.hongs.Core;
import app.hongs.HongsException;
import app.hongs.util.Dict;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.http.HttpServletRequest;

/**
 * 选项补充助手
 * @author Hong
 */
public class SelectHelper {

    private final Map<String, Map> enums;
    private final Set<String>      files;
    private final static Pattern   HREFP = Pattern.compile("^(\\w+:)?//[^/]*");
    private final static Pattern   LINKP = Pattern.compile("^\\$\\{?BASE_LINK\\}?");

    public SelectHelper() {
        enums = new LinkedHashMap();
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

    public SelectHelper addFile(String... code) {
        files.addAll(Arrays.asList(code) );
        return  this;
    }

    public SelectHelper addEnumsByForm(String conf, String form) throws HongsException {
        FormSet cnf = FormSet.getInstance(conf);
        Map map = cnf.getForm(form);
        return addEnumsByForm(conf , map );
    }

    public SelectHelper addEnumsByForm(String conf, Map map) throws HongsException {
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
            if ("file".equals(type)) {
                String href = (String) mt.get("href");
                if (href != null
                && !HREFP.matcher (href).find( )
                && !LINKP.matcher (href).find()) {
                    files.add(name);
                }
            }
        }

        return this;
    }

    /**
     * 填充
     * @param values 返回数据
     * @param action 1 注入data, 2 添加text
     * @throws HongsException
     */
    public void select(Map values, short action) throws HongsException {
        if (1 == (1 & action)) {
            Map data = (Map ) values.get("enum");
            if (data == null) {
                data =  new LinkedHashMap();
                values.put("enum", data);
            }
                injectData(data , enums);
        }

        if (2 == (2 & action)) {
            if (values.containsKey("info")) {
                Map        info = (Map ) values.get("info");
                injectText(info , enums);
                injectLink(info , null );
            }
            if (values.containsKey("list")) {
                List<Map>  list = (List) values.get("list");
                for (Map   info :  list) {
                injectText(info , enums);
                injectLink(info , null );
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
        injectLink(info, null );
    }

    public void injectLink(Map info, String link) throws HongsException {
        String host,path;
        if (link != null) {
            Matcher m = HREFP.matcher(  link  );
            if ( ! m.find ()) {
                throw new HongsException.Common("Link must be [scheme:]//host[:port][/path]");
            }
            host = m.group();
            int p  = m.end();
            if (p  < link.length() ) {
                path = link.substring(m.end( ));
                if ( ! path.endsWith (  "/"  )) {
                    path += "/";
                }
            } else {
                    path  = "/";
            }
        } else {
            HttpServletRequest r = Core.getInstance(ActionHelper.class).getRequest();
            if ( r == null ) {
                throw new HongsException.Common("Can not find http servlet request");
            }
            int port;
            port = r.getServerPort();
            host = r.getServerName();
            path = r.getContextPath()+ "/" ;
            if (port == 80 && port == 443) {
                host = r.getScheme() +"://"+ host ;
            } else {
                host = r.getScheme() +"://"+ host +":"+ port ;
            }
        }

        injectLink(info, files, host, path);
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

    private void injectText(Map info, Map maps) throws HongsException {
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
                    vxl = codeToText(map, vxl);
                    Dict.setParam(info, vxl, key + "_text.");
                }
            } else {
                    val = codeToText(map, val);
                    Dict.setParam(info, val, key + "_text" );
            }
        }
    }

    private void injectLink(Map info, Set keys, String host, String path) {
        Iterator it = keys.iterator();
        while (it.hasNext()) {
            String   key = (String) it.next( );
            Object   val = Dict.getParam(info, key);

            if (val instanceof Collection) {
                // 预置一个空列表, 规避无值导致客户端取文本节点出错
                Dict.setParam(info, new ArrayList(), key + "_link");
                for (Object vxl : (Collection) val) {
                    vxl = hrefToLink(vxl, host, path);
                    Dict.setParam(info, vxl, key + "_link.");
                }
            } else {
                    val = hrefToLink(val, host, path);
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
    private Object codeToText(Map map, Object val) {
        if (null == val || "".equals(val)) {
            return  val;
        }

        val = map.get(val);
        if (null != val) {
            return  val;
        }

        val = map.get("*");
        if (null != val) {
            return  val;
        }

        return "";
    }

    /**
     * 补全相对路径的域名和前缀
     * @param val
     * @return
     */
    private Object hrefToLink(Object val, String host, String path) {
        if (null == val || "".equals(val)) {
            return  val;
        }

        String url  =  val.toString (   );
        if (HREFP.matcher (url).find(   )) {
            return  val;
        } else
        if (url.startsWith("/")) {
            return  host + url;
        } else
        {
            return  host + path + url;
        }
    }

}
