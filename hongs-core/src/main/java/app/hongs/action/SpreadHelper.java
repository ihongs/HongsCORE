package app.hongs.action;

import app.hongs.Cnst;
import app.hongs.HongsException;
import app.hongs.dh.MergeMore;
import app.hongs.util.Synt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 扩展查询助手
 * @author Hongs
 */
public class SpreadHelper {

    private final Map<String, Map> items;

    public SpreadHelper() {
        items = new LinkedHashMap();
    }

    public SpreadHelper addItem(String code, Map<String, String> opts) {
        items.put(code, opts);
        return this;
    }

    public SpreadHelper addItem(String code, String[][] arrs) {
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
        return addItem(code, opts);
    }

    public SpreadHelper addItem(String code, String...  args) {
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
        return addItem(code, opts);
    }

    public SpreadHelper addItemsByForm(String conf, String form) throws HongsException {
        FormSet cnf = FormSet.getInstance(conf);
        Map map  = cnf.getForm(form);

        FormSet dfs = FormSet.getInstance("default");
        Map tps  = dfs.getEnum("__types__");

        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry et = (Map.Entry)it.next();
            Map       mt = (Map ) et.getValue();
            String  name = (String) et.getKey();
            String  type = (String) mt.get("__type__");
                    type = (String) tps.get(type); // 类型别名转换
            if (! "link".equals ( type )) {
                continue;
            }
            if (! mt.containsKey("conf")) {
                mt.put("conf", conf);
            }
            items.put(name, mt);
        }

        return this;
    }

    public void spread(Map  data) throws HongsException {
        if (data.containsKey("list")) {
            List   list  = (List) data.get("list");
            spread(list);
        }
        if (data.containsKey("info")) {
            Map    info  = (Map ) data.get("info");
            List   list  =  new ArrayList();
                   list.add(info);
            spread(list);
        }
    }

    public void spread(List list) throws HongsException {
        ActionHelper ah = ActionHelper.newInstance();
        MergeMore    mm = new MergeMore(list);

        for(Map.Entry et : items.entrySet()) {
            String fn = (String) et.getKey();
            Map    mt = (Map ) et.getValue();

            // 建立映射
            Map<Object, List> ms = mm.mapped(fn);
            if (ms.isEmpty() ) {
                continue;
            }

            // 查询路径
            String at = (String) mt.get("data-at");
            String ak = (String) mt.get("data-ak");
            if (null == ak || "".equals(ak)) {
                ak = fn.replace("_id$", "");
            }
            if (null == at || "".equals(at)) {
                String c = (String) mt.get("conf");
                String f = (String) mt.get("form");
                if (null == c || "".equals(c)) c = ""; // 缺省情况有问题
                if (null == f || "".equals(f)) f = ak;
                at  =  c + "/" + f +  "/retrieve" ;
            }

            // 查询字段
            Set rb = new HashSet();
            String vk = (String) mt.get("data-vk");
            String tk = (String) mt.get("data-tk");
            if (null != vk && !"".equals(vk )
            &&  null != tk && !"".equals(tk)) {
                rb.add( vk);
                rb.add( tk);
            } else {
                rb.add("-");
            }

            // 查询结构
            Map rd = new HashMap( );
            rd.put(   "!fork", "1");
            rd.put(Cnst.RN_KEY, 0 );
            rd.put(Cnst.RB_KEY, rb);
            rd.put(Cnst.ID_KEY, ms.keySet());

            // 获取结果
            /* Get */ ah.setRequestData(rd);
            new ActionRunner(at, ah).doInvoke();
            Map  sd = ah.getResponseData( );
            List ls = (List) sd.get("list");

            // 整合数据
            boolean rp = Synt.declare(mt.get("__repeated__"), true);
            if (rp) {
                mm.append(ls, ms, fn, ak);
            } else {
                mm.extend(ls, ms, fn, ak);
            }
        }
    }

}
