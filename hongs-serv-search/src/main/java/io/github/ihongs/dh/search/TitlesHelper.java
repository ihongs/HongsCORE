package io.github.ihongs.dh.search;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.ActionRunner;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.action.anno.Filter;
import io.github.ihongs.action.anno.FilterInvoker;
import io.github.ihongs.util.Dict;
import io.github.ihongs.util.Synt;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
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
public class TitlesHelper {

    public static final byte TEXT = 1;
    public static final byte FORK = 8;

    protected Map<String, Map<String, String>> enums = null;
    protected Map<String, Map<String, String>> forks = null;
    private   Set                              _cols = null;

    public TitlesHelper() {
        enums = new HashMap();
        forks = new HashMap();
    }

    public TitlesHelper addEnum(String code, Map<String, String> opts) {
        enums.put(code, opts);
        return this;
    }

    public TitlesHelper addFork(String code, Map<String, String> opts) {
        forks.put(code, opts);
        return this;
    }

    /**
     * 设置表单可选字段, 需先于 addItemsByForm
     * @param rb
     * @return
     */
    public TitlesHelper setItemsInForm(Set rb ) {
      if (rb != null && rb.isEmpty()) {
          rb  = null;
      }
        _cols =  rb ;
        return  this;
    }

    public TitlesHelper addItemsByForm(Map fs ) throws HongsException  {
        String conf = Dict.getValue( fs, "default", "@", "conf");
        String form = Dict.getValue( fs, "unknown", "@", "form");
        return addItemsByForm( conf, form, fs );
    }

    public TitlesHelper addItemsByForm(String conf, String form)
    throws HongsException {
        Map fs = FormSet.getInstance(conf /**/)
                        .getForm    (form /**/);
        return addItemsByForm( conf, form, fs );
    }

    public TitlesHelper addItemsByForm(String conf, String form, Map<String, Map> fs)
    throws HongsException {
        Map ts = FormSet.getInstance("default")
                        .getEnum ( "__types__");
        Iterator it = fs.entrySet().iterator( );

        while (it.hasNext()) {
            Map.Entry et = (Map.Entry)it.next();
            Map       mt = (Map ) et.getValue();
            String  name = (String) et.getKey();
            String  type = (String) mt.get("__type__");
                    type = (String) ts.get(   type   ); // 类型别名转换

            if (null != _cols && !_cols.contains(name)) {
                continue ;
            }

            if ("enum".equals(type)
            ||  "date".equals(type)
            ||"number".equals(type)
            ||"hidden".equals(type)) {
                String xc = Synt.defxult( (String) mt.get("conf") , conf);
                String xn = Synt.defxult( (String) mt.get("enum") , name);
                Map    fe ;
                try {
                       fe = FormSet.getInstance(xc).getEnumTranslated(xn);
                } catch ( HongsException ex) {
                if (ex.getErrno() == 0x10eb) {
                    continue;
                } else {
                    throw ex;
                }}
                enums.put(name, fe);
            } else
            if ("fork".equals(type)) {
                forks.put(name, mt);
            }
        }

        return this;
    }

    /**
     * 通过表单配置设置枚举数据(及关联关系)
     * @param info
     * @param md 1 绑定枚举, 2 绑定关联, 3 全绑定
     * @throws HongsException
     */
    public void addTitle(Map info, byte md) throws HongsException {
        addTitle(
            info,
            TEXT == (TEXT & md) ? enums : new HashMap(),
            FORK == (FORK & md) ? forks : new HashMap()
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
    protected void addTitle(Map info,
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
                lv  = (String) es.get("-"); // 未知选项
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
     * @param fc
     * @param fn
     * @throws HongsException
     */
    protected void addForks(List<Object[]> ls, Map fc, String fn) throws HongsException {
        String at = (String) fc.get("data-at");
        String vk = (String) fc.get("data-vk");
        String tk = (String) fc.get("data-tk");

        if (at == null || at.isEmpty()) {
            String c = (String) fc.get("conf");
            String f = (String) fc.get("form");
            at  =  c +"/"+ f +"/search";
        }
        if (vk == null || vk.isEmpty()) {
            vk  =  Cnst.ID_KEY;
        }
        if (tk == null || tk.isEmpty()) {
            tk  =  "name";
        }
        Object[] vk2 = Dict.splitKeys(vk);
        Object[] tk2 = Dict.splitKeys(tk);

        // 映射关系
        Map<Object, Object[]> lm = new HashMap();
        for(Object[] lx : ls) {
            lm.put(lx[0], lx);
        }
        Set li = lm.keySet( );

        // 查询结构
        Map cd = new HashMap();
        Map rd = new HashMap();
        Set rb = new HashSet();
        rd.put(Cnst.RB_KEY, rb );
        rd.put(Cnst.RN_KEY,1024);
        cd.put(Cnst.ORIGIN_ATTR, Core.ACTION_NAME.get( ) );
        rb.add(vk);
        rb.add(tk);

        // 获取结果
        ActionHelper ah = ActionHelper.newInstance( /**/ );
        ah.setContextData (cd);
        ah.setRequestData (rd);
        ActionRunner ar = ActionRunner.newInstance(ah, at);

        /**
         * Lucene 单个条件的数量无法超过 1024
         * 故需拆成片段
         * 分批进行查询
         */
       List l = new ArrayList(li);
        int k = l.size ();
        int j = 0 , i = 0;
        while  (j < k) {
            j = i + 1024 ;
            if (j > k) {
                j = k;
            }
            rd.put(Cnst.ID_KEY, l.subList(i, j) );
            /**/i = j;

            ar.doInvoke();

            // 整合数据
            Map sd  =  ah.getResponseData( /**/ );
            List <Map> lz = (List) sd.get("list");
            if (lz != null) for (Map ro : lz) {
                String lv = Dict.getValue(ro, "", vk2);
                String lt = Dict.getValue(ro, "", tk2);

                Object[ ]   lx  =  lm.get(lv);
                if (null != lx) {
                    lx[1] = lt;
                }
            }
        }
    }

    @Filter(Titler.class)
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Atitle {
        String   conf() default "default";
        String   form() default "";
        byte     adds() default 0 ;
    }

    /**
     * 选项补充处理器
     * <pre>
     * ab 参数含义:
     * _text 表示加选项文本
     * _fork 表示加关联数据
     * </pre>
     * @author Hong
     */
    public class Titler implements FilterInvoker {
        @Override
        public void invoke(ActionHelper helper, ActionRunner chains, Annotation anno)
        throws HongsException {
            Atitle   ann  = (Atitle) anno;
            String   conf = ann.conf();
            String   form = ann.form();
            byte     adds = ann.adds();

            if (adds == 0) {
                Set ab  = Synt.toTerms(
                    helper.getRequestData ( )
                          .get( Cnst.AB_KEY )
                );
                if (ab != null) {
                    if (ab.contains("_text")) {
                        adds += TEXT;
                    }

                    if (ab.contains("_fork")) {
                        adds += FORK;
                    }
                }
            }

            // 向下执行
            chains.doAction();
            if (adds == 0) {
                return;
            }
            Map  rsp  = helper.getResponseData();
            if ( rsp == null) {
                return;
            }
            Map  enf  = (Map) rsp.get ( "enfo" );
            if ( enf == null) {
                return;
            }

            // 识别路径
            if (form.length() == 0) {
                form = chains.getEntity();
            }
            if (conf.length() == 0) {
                conf = chains.getModule();
                // 照顾 Module Action 的配置规则. 2018/7/7 改为完全由外部预判
    //          if (FormSet.hasConfFile(conf+"/"+form)) {
    //              conf = conf+"/"+form ;
    //          }
            }

            // 填充数据
            try {
                Set rb = Synt.toTerms(
                    helper.getRequestData()
                          .get(Cnst.RB_KEY)
                );

                Map data  = (Map) helper.getAttribute("form:"+conf+"."+form);
                if (data == null) {
                    data  = FormSet.getInstance(conf).getForm(form);
                }


                TitlesHelper sel = new TitlesHelper();
                sel.setItemsInForm( rb );
                sel.addItemsByForm(conf, form, data );
                sel.addTitle(enf , adds);
            } catch (HongsException ex ) {
                int  ec  = ex.getErrno();
                if  (ec != 0x10e8 && ec != 0x10e9 && ec != 0x10ea ) {
                    throw  ex;
                }
            }

            // 返回数据
            helper.reply(rsp);
        }

    }


}
