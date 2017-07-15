package app.hongs.dh.search;

import app.hongs.Cnst;
import app.hongs.CoreLogger;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.FormSet;
import app.hongs.util.Data;
import app.hongs.util.Synt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 命名助手
 * @author Hongs
 */
public class SearchTitler {

    protected final String conf;
    protected final String form;
    protected Map<String, Map<String, String>> enums = null;
    protected Map<String, Map<String, String>> forks = null;

    public SearchTitler(String conf, String form) {
        this.conf = conf;
        this.form = form;
    }

    /**
     * 通过表单配置设置枚举数据(及关联关系)
     * @param info
     * @param md 1 绑定枚举, 2 绑定关联, 3 全绑定
     * @throws HongsException
     */
    public void addTitle(Map info, byte md) throws HongsException {
        if (1 != (1 & md) && 2 != (2 & md)) {
            return;
        }

        if (enums == null || forks == null) {
            enums =  new HashMap();
            forks =  new HashMap();
            Map<String, Map> fields = FormSet.getInstance( conf ).getForm( form );
            Map<String, String> fts = FormSet.getInstance( ).getEnum("__types__");

            for(Map.Entry<String, Map> et:fields.entrySet()) {
                Map    fc = et.getValue();
                String fn = et.getKey(  );
                String ft = (String) fc.get("__type__");
                       ft = fts.get( ft );
                if (  "enum".equals( ft )
                ||    "date".equals( ft )
                ||  "number".equals( ft )) {
                    String xc = Synt.defxult(( String ) fc.get("conf"), conf);
                    String xn = Synt.defxult(( String ) fc.get("enum"),   fn);
                    Map    fe = FormSet.getInstance(xc).getEnumTranslated(xn);
                    enums.put(fn, fe);
                } else
                if (  "fork".equals( ft )) {
                    forks.put(fn, fc);
                }
            }
        }

        addTitle(
            info,
            1 == (1 & md) ? enums : new HashMap(),
            2 == (2 & md) ? forks : new HashMap()
        );
    }

    /**
     * 追加名称列
     * 此方法通过 addEnums,addForks 来执行具体的关联操作
     * 请预先使用 setEnums,setForks 或 setLinks 设置关联
     * @param info 为通过 counts 得到的 info
     * @param enums
     * @param forks
     * @throws HongsException
     */
    public void addTitle(Map info,
            Map<String, Map<String, String>> enums,
            Map<String, Map<String, String>> forks)
            throws HongsException {
        Iterator<Map.Entry> it = info.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry et = it.next( );
            Object lo = et.getValue();
            if (!(lo instanceof List)) {
                continue;
            }

            List<Object[]> ls = (List) lo;
            String fn = Synt.declare ( et.getKey(), "");

            if (enums != null && enums.containsKey(fn)) {
                addEnums(ls, enums.get(fn), fn);
            } else
            if (forks != null && forks.containsKey(fn)) {
                addForks(ls, forks.get(fn), fn);
            } else {
                // 没有对应的枚举表则用值来补全
                for(Object[ ] lx : ls) {
                    if (lx[1] == null) {
                        lx[1]  = lx[0];
                    }
                }
            }
        }
    }

    /**
     * 通过查找枚举信息来补全名称
     * @param ls
     * @param es
     * @param fn
     */
    protected void addEnums(List<Object[]> ls, Map es, String fn) {
        for (Object[] lx : ls) {
            String lv = (String) lx[0];
            if (lv != null) {
                lv  = (String) es.get(lv ); // 得到标签
            }
            if (lv == null) {
                lv  = (String) es.get("*"); // 其他类型
            }
            if (lv == null) {
                continue;
            }
            lx[1] = lv;
        }
    }

    /**
     * 通过调用关联动作来补全名称
     * @param ls
     * @param fs
     * @param fn
     * @throws HongsException
     */
    protected void addForks(List<Object[]> ls, Map fs, String fn) throws HongsException {
        String at = (String) fs.get("data-at");
        String vk = (String) fs.get("data-vk");
        String tk = (String) fs.get("data-tk");
        if (at == null || at.length() == 0
        ||  vk == null || vk.length() == 0
        ||  tk == null || tk.length() == 0 ) {
            CoreLogger.error("data-at, data-vk or data-tk can not be empty in field "+fn);
            return;
        }

        // 映射关系
        Map<String, List> lm = new HashMap();
        for(Object[] lx : ls) {
            String   lv = ( String ) lx[0];
            List<Object[]> lw = lm.get(lv);
            if (lw == null) {
                lw  = new ArrayList();
                lm.put(lv, lw);
            }
            lw.add(lx);
        }

        // 查询结构
        Map rd = new HashMap();
        Set rb = new HashSet();
        int ps = at.indexOf("?");
        if (ps > -1) {
            String aq;
            aq = at.substring(1 + ps).trim();
            at = at.substring(0 , ps).trim();
            if (!"".equals(aq)) {
                if (aq.startsWith("{") && aq.endsWith("}")) {
                    rd = (  Map  ) Data.toObject(aq);
                } else {
                    rd = ActionHelper.parseQuery(aq);
                }
            }
        }
        rb.add(vk);
        rb.add(tk);
        rd.put(Cnst.RN_KEY, 0 );
        rd.put(Cnst.RB_KEY, rb);
        rd.put(Cnst.ID_KEY, lm.keySet());

        // 获取结果
        ActionHelper ah = ActionHelper.newInstance();
        ah.setAttribute("IN_FORK", true);
        ah.setRequestData(rd);
        new ActionRunner (at, ah).doInvoke();
        Map sd  = ah.getResponseData(  );
        List<Map> lz = (List) sd.get("list");
        if (lz == null) {
            return;
        }

        // 整合数据
        for ( Map  ro : lz) {
            String lv = Synt.declare(ro.get(vk), "");
            String lt = Synt.declare(ro.get(tk), "");

            List<Object[]> lw = lm.get(lv);
            if  (  null != lw )
            for (Object[]  lx : lw) {
                   lx[1] = lt ;
            }
        }
    }

}
