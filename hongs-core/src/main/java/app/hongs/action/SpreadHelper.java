package app.hongs.action;

import app.hongs.Cnst;
import app.hongs.Core;
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
                    type = (String)tps.get(   type   ); // 类型别名转换
            if (! "fork".equals ( type )) {
                continue;
            }
            if (! mt.containsKey("data-at")) {
            if (! mt.containsKey("conf")) {
                mt.put("conf", conf);
            }
            if (! mt.containsKey("form")) {
                mt.put("form", name.replace("_id",""));
            }
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
        MergeMore    mm = new  MergeMore  (  list  );
        ActionHelper ah = ActionHelper.newInstance();
        ah.setContextData(Synt.mapOf(
            Cnst.ORIGIN_ATTR, Core.ACTION_NAME.get()
        ));

        for(Map.Entry et : items.entrySet()) {
            String fn = (String) et.getKey();
            Map    mt = (Map) et.getValue( );

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
                    ak = fn + "_data";
                }
            }
            if (null == at || "".equals(at)) {
                String c = (String) mt.get("conf");
                String f = (String) mt.get("form");
                at  =  c + "/" + f + "/search";
            }

            // 查询结构
            Map rd ; Set rb ;
            String ap = null;
            String aq = null;
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
            // 没有指定 vk 时与其 id 进行关联
            if (null == vk || "".equals(vk)) {
                vk = Cnst . ID_KEY ;
            }
            rd.put(vk, ms.keySet());

            // 获取结果
            ah.setRequestData(rd);
            if (rd.containsKey(Cnst.AB_KEY)) {
                new ActionRunner(ah, at).doAction();
            } else {
                new ActionRunner(ah, at).doInvoke();
            }
            Map sd  = ah.getResponseData(  );
            List<Map> ls = (List) sd.get("list");
            if (ls == null) {
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

}
