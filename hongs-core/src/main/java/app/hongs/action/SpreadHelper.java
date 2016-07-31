package app.hongs.action;

import app.hongs.Cnst;
import app.hongs.HongsException;
import app.hongs.dh.MergeMore;
import app.hongs.util.Data;
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
        Map map = cnf.getForm(form);
        return addItemsByForm(conf , map );
    }

    public SpreadHelper addItemsByForm(String conf, Map map) throws HongsException {
        FormSet dfs = FormSet.getInstance("default");
        Map tps = dfs.getEnum("__types__");

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
        MergeMore mm = new MergeMore(list);
        ah.setAttribute( "in_fork" , true);

        for(Map.Entry et : items.entrySet()) {
            String fn = (String) et.getKey();
            Map    mt = (Map ) et.getValue();

            // 建立映射
            Map<Object, List> ms = mm.mapped( fn );
            if (ms.isEmpty() ) {
                continue;
            }

            String ak = (String) mt.get("data-ak"); // 数据放入此下
            String at = (String) mt.get("data-at"); // 关联动作路径
            String aq = (String) mt.get("data-aq"); // 关联查询参数
            String vk = (String) mt.get("data-vk"); // 关联字段
            String tk = (String) mt.get("data-tk"); // 名称字段

            // 查询路径
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

            // 查询结构
            Map rd; Set rb; aq = aq.trim(  );
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
            rd.put(Cnst.ID_KEY, ms.keySet());

            // 获取结果
            ah.setRequestData(rd);
            if (rd.containsKey(Cnst.MD_KEY)) {
                new ActionRunner(at, ah).doAction();
            } else {
                new ActionRunner(at, ah).doInvoke();
            }
            Map sd  = ah.getResponseData(  );
            List<Map> ls = (List) sd.get("list");
            if (ls == null) {
                continue;
            }

            // 整合数据
            boolean rp = Synt.declare(mt.get("__repeated__"), true);
            if (rp) {
                mm.append(ls, ms, vk, ak);
            } else {
                mm.extend(ls, ms, vk, ak);
            }
        }
    }

}
