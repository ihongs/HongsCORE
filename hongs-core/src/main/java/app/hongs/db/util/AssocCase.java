package app.hongs.db.util;

import app.hongs.Cnst;
import app.hongs.CoreLogger;
import app.hongs.HongsException;
import app.hongs.db.Model;
import app.hongs.db.Table;
import app.hongs.util.Dict;
import app.hongs.util.Synt;
import app.hongs.util.Tool;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 关联用例
 *
 * <p>此类可将外部请求数据转换为库表查询用例, 用法如下:</p>
 * <pre>
 *  FetchCase secondCase = new FetchCase(firstCase)
 *      .allow( table )
 *      .parse(request);
 *
 *  allow(table) 可换成 allow(params)
 *  params 可用 Table.getParams 或 FormSet.getForm().get("@") 得到,
 *  得到的 Map 中的 listable,sortable,findable,filtable 等用于设置.
 * </pre>
 *
 * @author Hongs
 */
public class AssocCase {

    /**
     * 可列举字段, 用于 FetchCase 的 Option
     */
    public  static final String  LISTABLE = "LISTABLE";
    /**
     * 可排序字段, 用于 FetchCase 的 Option, 未设置则取 LISTABLE
     */
    public  static final String  SORTABLE = "SORTABLE";
    /**
     * 可模糊搜索, 用于 FetchCase 的 Option
     */
    public  static final String  FINDABLE = "FINDABLE";
    /**
     * 可过滤字段, 用于 FetchCase 的 Option, 未设置则取 LISTABLE
     */
    public  static final String  FILTABLE = "FILTABLE";
    /**
     * 可存储字段, 用于 FetchCase 的 Option, 为设置则取 LISTABLE
     */
    public  static final String  SAVEABLE = "SAVEABLE";
    /**
     * 禁止某项 allow 设置
     */
    public  static final String  DENY = "__DENY__";
    /**
     * 清空某项 allow 设置
     */
    public  static final String  WIPE = "__WIPE__";

    private static final Pattern anPt = Pattern.compile("^[\\w\\.]+\\s*(:|$)");
    private static final Pattern cnPt = Pattern.compile("^[\\w]+$");

    private static final Set<String  /**/  > func = new HashSet();
    private static final Map<String, String> rels = new HashMap();
    static {
        func.add(Cnst.PN_KEY);
        func.add(Cnst.GN_KEY);
        func.add(Cnst.RN_KEY);
        func.add(Cnst.OB_KEY);
        func.add(Cnst.RB_KEY);
        func.add(Cnst.WD_KEY);
        func.add(Cnst.OR_KEY);
        func.add(Cnst.AR_KEY);
        rels.put(Cnst.EQ_REL, "=" );
        rels.put(Cnst.NE_REL, "!=");
        rels.put(Cnst.GT_REL, ">" );
        rels.put(Cnst.GE_REL, ">=");
        rels.put(Cnst.LT_REL, "<" );
        rels.put(Cnst.LE_REL, "<=");
        rels.put(Cnst.IN_REL, "IN");
        rels.put(Cnst.NI_REL, "NOT IN");
    }

    private final FetchCase  /**/  that;
    private final Map<String, Map> opts;
    private final Map<String, Map> bufs;

    /**
     * 构造方法
     * @param caze 模板查询对象
     */
    public AssocCase(FetchCase caze) {
        if (caze == null) {
            throw new NullPointerException(AssocCase.class.getName()+": temp can not be null");
        }
        this.that = caze;
        this.opts = new HashMap();
        this.bufs = new HashMap();
    }

    /**
     * 自定义许可字段
     * <pre>
     * fs 仅给一个常量 WIPE 表示清除 an 的 allow 设置,
     * fs 仅给一个常量 DENY 表示禁用 an 的 allow 设置,
     * fs 不给值或给空串同样表示禁用 an 的 allow 设置,
     * 清除后非 LISTABLE,FINDABLE 即可继承 LISTABLE;
     * </pre>
     * <pre>
     * <b>字段的首字符:</b>
     *   + 相对 LISTABLE 增加字段,
     *   - 相对 LISTABLE 删减字段,
     *   # 定义后续字段的别名前缀和表名,
     *     如 "#foo:bar" 定义别名前缀为 "foo." 表名前缀为 "bar.",
     *     表名省略时等同于别名, 别名以 "_" 结尾时则不再追加 ".";
     * </pre>
     * @param an
     * @param fs
     * @return
     */
    public AssocCase allow(String an, String... fs) {
        if (fs.length == 1) {
            if (WIPE.equals(fs[0])) {
                bufs.remove(an);
                opts.remove(an);
                return this;
            } else
            if (DENY.equals(fs[0])) {
                bufs.remove(an);
                opts.put(an, new LinkedHashMap( ) );
                return this;
            }
        }

        Map af = new LinkedHashMap( );
        opts.put(an,af);
        bufs.remove(an);

        Matcher m;
        String  k;
        String  kp = null; // 别名前缀
        String  fp = null; // 字段前缀

        for( String f : fs ) {
            f = f.trim();
            if (f.isEmpty()) {
                continue;
            }

            /**
             * #别名前缀:当前表名
             * 发现此行时即当设置
             * 后面遇到的干净别名、字段均会附加上设置的前缀
             * 别名前缀可以用点或下划线结尾, 没有的自动补点
             * 表名前缀可以用点结尾, 每有则补点，下划线转点
             */
            if (f.startsWith("#")) {
                f = f.substring(1);
                int p = f.indexOf(":");
                if (p > -1) {
                    kp = f.substring(0, p).trim();
                    fp = f.substring(1+ p).trim();
                } else {
                    kp = fp = f.trim();
                }

                if ( fp.endsWith("_")) {
                     fp = fp.substring(0, fp.length() - 1);
                     fp = "`" + fp +"`.";
                } else
                if (!fp.endsWith(".")) {
                     fp = "`" + fp +"`.";
                }

                if (!kp.endsWith("." )
                &&  !kp.endsWith("_")) {
                     kp = kp + ".";
                }

                continue;
            }

            m = anPt.matcher(f);
            if (m.find()) {
                if (":".equals(m.group(1))) {
                    k = f.substring(0, m.end() -1).trim();
                    f = f.substring(   m.end()   ).trim();
                } else {
                    k = f;
                }

                // 补全前缀
                if (kp != null && cnPt.matcher(k).matches()) {
                    k = kp +/**/ k /**/;
                }
                if (fp != null && cnPt.matcher(f).matches()) {
                    f = fp +"`"+ f +"`";
                }

                af.put( k, f);
            } else {
                af.put( f, f);
            }
        }

        return this;
    }

    /**
     * 从表单设置许可字段
     * 默认逗号分隔, 语句较复杂时, 可用分号分隔, 但不支持混用
     * 可用 table.getParams() 或 forms.get("@"), 后者可以调用 FormSet.getForm(String).get(String)
     * @param fc
     * @return
     */
    public AssocCase allow(Map fc) {
        String[] ks = new String[] {"listable", "sortable", "findable", "filtable", "saveable"};
        for(String k : ks) {
            String s = fc.get(k).toString( ).trim( );
            if ("".equals(s) ) {
                continue ;
            }
            if (s.indexOf(';') != -1) {
                allow(k.toUpperCase(), s.split(";"));
            } else {
                allow(k.toUpperCase(), s.split(","));
            }
        }
        return this;
    }

    /**
     * 解析查询数据
     * @param rd
     * @return
     */
    public AssocCase parse(Map rd) {
        Map xd = new LinkedHashMap (rd);
        AssocCase.this.parse( that, xd );
        return this;
    }

    /**
     * 转换查询数据
     * 此处会克隆一份 FetchCase
     * 故可以在设置好后反复调用
     * @param rd
     * @return
     */
    public FetchCase trans(Map rd) {
        FetchCase caze = that.clone(  );
        Map xd = new LinkedHashMap (rd);
        AssocCase.this.parse(caze , xd);
        return caze;
    }

    /**
     * 获取存储数据
     * 取出可列举的字段对应的值
     * 以便用于创建和更新等操作
     * @param rd
     * @return
     */
    public Map<String, Object> saves(Map rd) {
        Map<String, String> af = allow(SAVEABLE);
        Map sd = new HashMap();

        for(Map.Entry<String, String> et : af.entrySet()) {
            String fc = et.getValue();
            String fn = et.getKey(  );
            Object fv = rd.get ( fn );
            if (fv != null) {
                sd.put(fc, fv);
            }
        }

        return  sd;
    }

    //** 内部工具方法 **/

    private void parse(FetchCase caze, Map rd) {
        if (rd == null || rd.isEmpty()) return;

        field(caze, Synt.asTerms(rd.remove(Cnst.RB_KEY)));
        order(caze, Synt.asTerms(rd.remove(Cnst.OB_KEY)));
        query(caze, Synt.asWords(rd.remove(Cnst.WD_KEY)));
        where(caze, rd);
    }

    private void field(FetchCase caze, Set<String> rb) {
        if (rb == null || rb.isEmpty()) return;

        Map<String,     String > af = allow(LISTABLE);
        Map<String, Set<String>> cf = new HashMap(  );
        Set<String> ic = new LinkedHashSet();
        Set<String> ec = new LinkedHashSet();
        Set<String> xc ;

        // 整理出层级结构, 方便处理通配符
        for(String  fn : af.keySet()) {
            String  k  ;
            int p = fn.lastIndexOf(".");
            if (p > -1) {
                k = fn.substring(0 , p)+".*";
            } else {
                k = "*";
            }

            Set<String> fs = cf.get ( k );
            if (fs == null  ) {
                fs  = new LinkedHashSet();
                cf.put(k, fs);
            }

            fs.add(fn);
        }

        for(String  fn : rb) {
            if (fn.startsWith("-") ) {
                fn = fn.substring(1);
                xc = ec;
            } else {
                xc = ic;
            }

            if (cf.containsKey(fn) ) {
                xc.addAll(cf.get(fn));
            } else
            if (af.containsKey(fn) ) {
                xc.add(fn);

                // 排除时, 先在包含中增加全部
                if (xc == ec) {
                    int p  = fn.lastIndexOf(".");
                    if (p != -1) {
                        fn = fn.substring(0 , p)+".*";
                    } else {
                        fn = "*";
                    }
                    ic.addAll(cf.get(fn));
                }
            }
        }

        // 默认没给就是全部
        if (ic.isEmpty() == true ) {
            ic.addAll(af.keySet());
        }

        // 取差集排排除字段
        if (ec.isEmpty() == false) {
            ic.removeAll(ec);
        }

        for(String  fn : ic) {
            String  fv = af.get(fn);
            caze.select(fv +" AS `"+ fn +"`");
        }
    }

    private void order(FetchCase caze, Set<String> ob) {
        if (ob == null || ob.isEmpty()) return;

        Map<String, String> af = allow(SORTABLE);

        for(String fn : ob) {
            boolean desc = fn.startsWith("-");
            if (desc) {
                fn = fn.substring(1);
            }
            if (! af.containsKey(fn)) {
                continue;
            }
            fn = af.get (fn);
            if (desc && !fn.endsWith(" DESC")) {
                fn += " DESC";
            }
            caze.orderBy(fn);
        }
    }

    private void query(FetchCase caze, Set<String> wd) {
        if (wd == null || wd.isEmpty()) return;

        Map<String, String> af = allow(FINDABLE);
        int  i = 0;
        int  l = wd.size( ) * af.size( );
        Object[]      ab = new Object[l];
        Set<String>   xd = new HashSet();
        StringBuilder sb = new StringBuilder();

        // 转义待查词, 避开通配符, 以防止歧义
        for(String  wb : wd) {
            xd.add("%" + Tool.escape( wb , "/%_[]" , "/") + "%" );
        }

        for(Map.Entry<String, String> et : af.entrySet()) {
            String fn = et.getValue();
            sb.append("(");
            for(String wb : xd) {
                ab[ i++ ] = wb;
                sb.append(fn).append( " LIKE ? ESCAPE '/' AND " );
            }
            sb.setLength(sb.length() - 5);
            sb.append(") OR " );
        }

        if (l > 0) {
            sb.setLength(sb.length() - 4);
            caze.filter ("(" + sb.toString() + ")" , ab );
        }
    }

    private void where(FetchCase caze, Map rd) {
        if (rd == null || rd.isEmpty()) return;

        Map<String, String> af = allow(FILTABLE);

        for(Map.Entry<String, String> et : af.entrySet()) {
            String kn = et.getKey(  );
            String fn = et.getValue();
            Object fv = Dict.getParam(rd , kn);

            if (null == fv || "".equals( fv )) {
                continue; // 忽略空串, 但可以用 xxx.!eq= 查询空串
            }

            if (fv instanceof Map) {
                Map fm = new HashMap();
                fm.putAll(( Map ) fv );

                // 处理关系符号
                for(Map.Entry<String, String> el : rels.entrySet()) {
                    String rl = el.getKey(  );
                    Object rv = fm.remove(rl);
                    if (rv == null) continue ;
                    String rn = el.getValue();
                    if (Cnst.IN_REL.equals(rl)
                    ||  Cnst.NI_REL.equals(rl)) {
                        caze.filter(fn+" "+rn+" (?)", rv);
                    } else
                    if (!(rv instanceof Map)
                    &&  !(rv instanceof Set)
                    &&  !(rv instanceof Collection)) {
                        caze.filter(fn+" "+rn+ " ?" , rv);
                    } else {
                        CoreLogger.trace(AssocCase.class.getName()+": Can not set "+fn+" "+rn+" Collection");
                    }
                }

                // 清除功能参数
                for(String rl : func) {
                    fm.remove(rl);
                }
                    fm.remove("");

                // 如果还有剩余, 就当做 IN 来处理
                if (!fm.isEmpty()) {
                    fv = new LinkedHashSet( fm.values(  ) );
                    caze.filter(fn+" IN (?)", fv);
                }
            } else
            if (fv instanceof Set) {
                Set vs = (Set) fv;
                    vs.remove("");
                if(!vs.isEmpty( )) {
                    caze.filter(fn+" IN (?)", fv);
                }
            } else
            if (fv instanceof Collection) {
                Set vs = new LinkedHashSet((Collection) fv);
                    vs.remove("");
                if(!vs.isEmpty( )) {
                    caze.filter(fn+" IN (?)", fv);
                }
            } else {
                    caze.filter(fn+  " = ?" , fv);
            }
        }

        // 分组查询, 满足复杂的组合查询条件
        group(caze, Synt.declare(rd.get(Cnst.OR_KEY), Set.class), "OR" );
        group(caze, Synt.declare(rd.get(Cnst.AR_KEY), Set.class), "AND");
    }

    private void group(FetchCase caze, Set<Map> ar, String rn) {
        if (ar == null || ar.isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        FetchCase   caxe = new FetchCase(    );

        for (Map rd : (Set<Map>) ar) {
            // 将查询用例里的条件清空
            // 然后将分组数据转为条件
            // 参数无需清空
            caxe.wheres.setLength(0);
            where(caxe, rd);

            if (caxe.wheres.length() > 0) {
                String wh = FetchCase.preWhere.matcher(caxe.wheres).replaceFirst("");
                sb.append('(').append(wh).append(')')
                  .append(' ').append(rn).append(' ');
            }
        }

        if (sb.length() > 0) {
            sb.setLength( sb.length()-rn.length()-2 );
            caze.wheres .append(   " AND "   )
                  .append('(').append(sb).append(')');
            caze.wparams.addAll(caxe.wparams );
        }
    }

    private Map allow(String on) {
        Map af  = bufs.get ( on );
        if (af != null ) {
            return af;
        }

        af = allowCheck(on);
        bufs.put(on, af);
        return af;
    }

    private Map allowCheck(String on) {
        Map af = opts.get(on);

        // 相对查询字段增加, 删减
        if (af != null && !LISTABLE.equals(on)) {
            Map xf = allowDiffs(af);
            if (xf != null) {
                return xf ;
            }
        }

        // 搜索字段不能从列举继承
        if (af == null && !LISTABLE.equals(on)
                       && !FINDABLE.equals(on)) {
            af =  allow(LISTABLE);
        }
        if (af == null) {
            af =  new  HashMap( );
        }

        if (SAVEABLE.equals(on) ) {
            return allowSaves(af);
        } else {
            return allowTrans(af);
        }
    }

    private Map allowDiffs(Map af) {
        Map ic = new LinkedHashMap();
        Set ec = new HashSet();

        for(Object o : af.entrySet()) {
            Map.Entry e = (Map.Entry) o;
            String k = (String) e.getKey(  );
            String f = (String) e.getValue();
            if (k.startsWith("+")) {
                ic.put(k.substring(1), f);
            } else
            if (k.startsWith("-")) {
                ec.add(k.substring(1));
            }
        }

        if (ic.isEmpty() && ec.isEmpty()) {
            return null;
        }

        Map xf = new LinkedHashMap(allow(LISTABLE));

        for(Object o : xf.entrySet()) {
            Map.Entry e = (Map.Entry) o;
            String k = (String) e.getKey(  );
            String f = (String) e.getValue();
            xf.put(k, f);
        }
        for(Object k : ec) {
            xf.remove(k);
        }

        return xf;
    }

    private Map allowTrans(Map af) {
        Map al = new LinkedHashMap();

        for(Object ot : af.entrySet()) {
            Map.Entry et = (Map.Entry) ot;
            String k = (String) et.getKey(  );
            String f = (String) et.getValue();
            if (cnPt.matcher(f).matches()) {
                f = ".`"+ f +"`";
            }
                al.put(k, f);
        }

        return  al;
    }

    private Map allowSaves(Map af) {
        Map al = new LinkedHashMap();

        for(Object ot : af.entrySet()) {
            Map.Entry et = (Map.Entry) ot;
            String k = (String) et.getKey(  );
            String f = (String) et.getValue();
            if (cnPt.matcher(f).matches()) {
                al.put(k, f);
            }
        }

        return  al;
    }

    //** Model,Table 的快捷支持 **/

    /**
     * 从模型设置许可字段
     * @param model
     * @return
     */
    public AssocCase allow(Model model) {
        allow(model.table);

        if (model.listable != null) {
            allow(LISTABLE, model.listable);
        }
        if (model.sortable != null) {
            allow(SORTABLE, model.sortable);
        }
        if (model.findable != null) {
            allow(FINDABLE, model.findable);
        }
        if (model.filtable != null) {
            allow(FILTABLE, model.filtable);
        }

        return this;
    }

    /**
     * 从库表设置许可字段
     * @param table
     * @return
     */
    public AssocCase allow(Table table) {
        Map af = new LinkedHashMap();

        /**
         * 此处未将 FINDTABLE 设为当前所有字段
         * 模糊搜索比较特殊
         * 文本类型字段才行
         * 并不便于自动指派
         */
        bufs.put(LISTABLE, af);
        bufs.put(SORTABLE, af);
        bufs.put(FILTABLE, af);

        allow(table, table, table.getAssocs(), null, null, af);

        return this;
    }

    private Map allow(Table table, Table assoc, Map ac, String tn, String qn, Map al) {
        String  ax , tx  ;

        /**
         * 三个变量: 层级名(qn), 名前缀(ax), 表前缀(tx)
         * 第一层时, 无需加名前缀, 无关联表前缀也不用加
         * 第二层时, 需将表名作为名前缀, 下级需带层级名
         */
        if (null ==  qn  ) {
            qn = "";
            ax = "";
            tx = ".";
        } else
        if ("".equals(qn)) {
            qn = tn;
            ax = qn + ".";
            tx = "`"+ tn +"`.";
        } else {
            qn = qn + "."+ tn ;
            ax = qn + ".";
            tx = "`"+ tn +"`.";
        }

        try {
            Map fs = assoc.getFields( );
            for(Object n : fs.keySet()) {
                String k = (String) n;
                String f = (String) n;
                k = ax +/**/ k /**/;
                f = tx +"`"+ f +"`";
                al.put(k , f);
            }
        }
        catch (HongsException ex) {
            CoreLogger.error( ex);
        }

        if (ac != null && !ac.isEmpty()) {
            Iterator it = ac.entrySet().iterator(  );
            while (it.hasNext()) {
                Map.Entry et = (Map.Entry) it.next();
                Map       tc = (Map) et.getValue(  );
                String jn = (String) tc.get ("join");

                // 不是 JOIN 的不理会
                if (!"INNER".equals(jn) && !"LEFT".equals(jn)
                &&  !"RIGHT".equals(jn) && !"FULL".equals(jn)) {
                    continue;
                }

                try {
                    ac = (Map) tc.get("assocs");
                    tn = (String) et.getKey(  );
                    assoc = table.getAssocInst(tn);
                    allow(table, assoc, ac, tn,qn, al);
                }
                catch (HongsException ex) {
                    CoreLogger.error( ex);
                }
            }
        }

        return  al;
    }

    /*
    public static void main(String[] args) {
        FetchCase fc = new FetchCase()
                .from("abc", "a")
                .join("def", "d", ".id = :d_id")
                .join("xyz", "x", ".a_id = :id");
        AssocCase ac = new AssocCase(fc)
                .allow(AssocCase.LISTABLE,
                        "f1", "a1:f2", "a2:CONCAT('$', .f3)",
                        "#b :d",
                        "f1", "a1:f2", "a2:CONCAT('$',d.f3)",
                        "#x_",
                        "f1", "a1:f2", "a2:CONCAT(x.f3,'%')")
                .parse(Synt.asMap(
                        "rb", "f1,a2,b.a2",
                        "ob", "f2,a2,x_a2",
                        "x_a1", "123"
                ));
        app.hongs.util.Data.dumps(ac.opts);
        System.err.println(fc.toString( ));
    }
    */

}
