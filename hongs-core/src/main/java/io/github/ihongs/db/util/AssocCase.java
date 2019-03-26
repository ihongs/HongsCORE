package io.github.ihongs.db.util;

import io.github.ihongs.Cnst;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.HongsException;
import io.github.ihongs.HongsExemption;
import io.github.ihongs.db.Model;
import io.github.ihongs.db.Table;
import io.github.ihongs.util.Dict;
import io.github.ihongs.util.Synt;
import io.github.ihongs.util.Tool;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 关联用例
 *
 * <p>此类可将外部请求数据转换为库表查询用例, 用法如下:</p>
 * <pre>
 *  AssocCase ac = new AssocCase(fc)
 *      .allow(AssocCase.LISTABLE,
 *          "f1", "a1:f2", "a2:CONCAT('$', .f3)", // 查询别名:字段名
 *          "#b :d",                              // 命名层级:表别名
 *          "f1", "a1:f2", "a2:CONCAT('$',d.f3)",
 *          "#x_",
 *          "f1", "a1:f2", "a2:CONCAT(x.f3,'%')")
 *      .parse(queryData);
 * </pre>
 * <p>
 * allow 所传值可用 Table.getParams() 或 FormSet.getForm().get("@")
 * 所得到的 Map 里面的 listable,sortable,srchable,findable 等来设置,
 * 同时提供了 allow(Table), allow(Model) 两个快捷方法自动读取并设置;
 * </p>
 *
 * @author Hongs
 */
public class AssocCase {

    /**
     * 可列举字段, 用于 FetchCase 的 Option
     */
    public  static final String  LISTABLE = "LISTABLE";
    /**
     * 可搜索字段, 用于 FetchCase 的 Option
     */
    public  static final String  SRCHABLE = "SRCHABLE";
    /**
     * 可排序字段, 用于 FetchCase 的 Option, 未设置则取 LISTABLE
     */
    public  static final String  SORTABLE = "SORTABLE";
    /**
     * 可过滤字段, 用于 FetchCase 的 Option, 未设置则取 LISTABLE
     */
    public  static final String  FINDABLE = "FINDABLE";
    /**
     * 搜索的字段, 用于 FetchCase 的 Option, 未设置则取 SRCHABLE
     */
    public  static final String  WORDABLE = "WORDABLE";
    /**
     * 可区间查询, 用于 FetchCase 的 Option, 未设置则取 FINDABLE
     */
    public  static final String  RATEABLE = "RATEABLE";
    /**
     * 可存储字段, 用于 FetchCase 的 Option, 未设置则取 LISTABLE
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

    private static final Pattern ANPT = Pattern.compile("^[\\w\\.]+\\s*(:|$)");
    private static final Pattern CNPT = Pattern.compile("^[\\w]+$");

    private static final Map<String, String> RELS = new HashMap();
    private static final Set<String  /**/  > NOLS = new HashSet();
    private static final Set<String  /**/  > FUNC = new HashSet();
    static {
        RELS.put(Cnst.EQ_REL, "=" );
        RELS.put(Cnst.NE_REL, "!=");
        RELS.put(Cnst.GT_REL, ">" );
        RELS.put(Cnst.GE_REL, ">=");
        RELS.put(Cnst.LT_REL, "<" );
        RELS.put(Cnst.LE_REL, "<=");
        RELS.put(Cnst.IN_REL, "IN");
        RELS.put(Cnst.NI_REL, "NOT IN");
        RELS.put(Cnst.CQ_REL, "LIKE");
        RELS.put(Cnst.NC_REL, "NOT LIKE");

        NOLS.add(Cnst.AI_REL);
        NOLS.add(Cnst.SI_REL);
        NOLS.add(Cnst.SE_REL);
        NOLS.add(Cnst.WT_REL);

        FUNC.add(Cnst.PN_KEY);
        FUNC.add(Cnst.GN_KEY);
        FUNC.add(Cnst.RN_KEY);
        FUNC.add(Cnst.OB_KEY);
        FUNC.add(Cnst.RB_KEY);
        FUNC.add(Cnst.WD_KEY);
        FUNC.add(Cnst.OR_KEY);
        FUNC.add(Cnst.AR_KEY);
    }

    private final FetchCase        that;
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
     * 清除后非 LISTABLE,SRCHABLE 即可继承 LISTABLE;
     * </pre>
     * <pre>
     * <b>字段的首字符:</b>
     *   - 相对 LISTABLE 删减字段,
     *   + 相对 LISTABLE 增加字段,
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
             * 表名前缀可以用点结尾, 没有则补点，下划线转点
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

            m = ANPT.matcher(f);
            if (m.find()) {
                if (":".equals(m.group(1))) {
                    k = f.substring(0, m.end() -1).trim();
                    f = f.substring(   m.end()   ).trim();
                } else {
                    k = f;
                }

                // 补全前缀
                if (kp != null && CNPT.matcher(k).matches()) {
                    k = kp +/**/ k /**/;
                }
                if (fp != null && CNPT.matcher(f).matches()) {
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
        String[] ks = new String[] {"listable", "sortable", "srchable", "findable"};
        for(String k : ks) {
            Object s = fc.get( k );
            if (null == s) {
                continue ;
            }
            allow(k.toUpperCase( ), (String[]) Synt.toArray(s));
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
        parse( that, xd);
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
        parse( caze, xd);
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

        if (! caze.hasField()) {
            field(caze, Synt.toTerms(rd.remove(Cnst.RB_KEY)));
        }
        if (! caze.hasOrder()) {
            order(caze, Synt.toTerms(rd.remove(Cnst.OB_KEY)));
        }

        query(caze, Synt.toWords(rd.remove(Cnst.WD_KEY)));

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

        for(String  fn : ob) {
            boolean desc = fn.startsWith("-");
            if (desc) {
                fn = fn.substring(1);
            }
            if (! af.containsKey(fn)) {
                continue;
            }
            fn = af.get( fn );
            if (desc && !fn.endsWith(" DESC")) {
                fn += " DESC";
            }
            caze.assort( fn );
        }
    }

    private void query(FetchCase caze, Set<String> wd) {
        if (wd == null || wd.isEmpty()) return;

        Map<String, String> af = allow(WORDABLE);

        query ( caze, wd, af.values ());
    }

    private void query(FetchCase caze, Collection<String> wd, Collection<String> wf) {
        if (wf == null || wf.isEmpty()) return;

        int  i = 0;
        int  l = wd.size( ) * wf.size( );
        Object[]      ab = new Object[l];
        Set<String>   xd = new HashSet();
        StringBuilder sb = new StringBuilder();

        // 转义待查词, 避开通配符, 以防止歧义
        for(String  fw : wd) {
            xd.add("%" + Tool.escape(fw , "/%_[]" , "/") + "%");
        }

        for(String  fn : wf) {
            sb.append("(");
            for(String wb : xd) {
                ab[ i++ ] = wb;
                sb.append(fn).append(" LIKE ? ESCAPE '/' AND ");
            }
            sb.setLength(sb.length() - 5);
            sb.append(") OR " );
        }   sb.setLength(sb.length() - 4);

        if (wf.size() == 1) {
            caze.filter(    sb.toString()    , ab);
        } else {
            caze.filter("("+sb.toString()+")", ab);
        }
    }

    private void where(FetchCase caze, Map rd) {
        if (rd == null || rd.isEmpty()) return;

        Map<String, String> af = allow(FINDABLE);
        Map<String, String> rf = allow(RATEABLE);
        Map<String, String> sf = allow(SRCHABLE);

        for(Map.Entry<String, String> et : af.entrySet()) {
            String kn = et.getKey(  );
            String fn = et.getValue();
            Object fv = Dict.getParam(rd , kn);

            if (null == fv || "".equals( fv )) {
                continue; // 忽略空串, 但可以用 xx.eq= 查询空串
            }

            if (fv instanceof Map) {
                Map fm = new HashMap();
                fm.putAll(( Map ) fv );

                // 处理关系符号
                for(Map.Entry<String, String> el : RELS.entrySet()) {
                    String rl = el.getKey(  );
                    String rn = el.getValue();
                    Object rv = fm.remove(rl);
                    if (rv == null) continue ;

                    if (Cnst.IN_REL.equals(rl)
                    ||  Cnst.NI_REL.equals(rl)) {
                        caze.filter(fn+" "+rn+" (?)", rv);
                        continue;
                    } else
                    if (Cnst.GT_REL.equals(rl)
                    ||  Cnst.GE_REL.equals(rl)) {
                        // 区间为空串表示无限
                        if ("".equals(rv)
                        && !rf.containsKey(fn)) {
                            continue;
                        }
                    } else
                    if (Cnst.LT_REL.equals(rl)
                    ||  Cnst.LE_REL.equals(rl)) {
                        // 区间为空串表示无限
                        if ("".equals(rv)
                        && !rf.containsKey(fn)) {
                            continue;
                        }
                    } else
                    if (Cnst.CQ_REL.equals(rl)
                    ||  Cnst.NC_REL.equals(rl)) {
                        // 包含为空串表示不限
                        if ("".equals(rv)
                        && !sf.containsKey(fn)) {
                            continue;
                        }

                        // 组织成 LIKE 查询串
                        Set<String> ws = Synt.toWords(rv);
                        Set<String> ns = new  HashSet(01);
                        ns.add(fn);
                        query(caze, ws , ns);
                        continue;
                    }

                    if (!(rv instanceof Map)
                    &&  !(rv instanceof Set)
                    &&  !(rv instanceof Collection)) {
                        caze.filter(fn+" "+rn+ " ?" , rv);
                    } else {
                        CoreLogger.trace(AssocCase.class.getName()+": Can not set "+fn+" "+rn+" Collection");
                    }
                }

                // 空值判断语句
                if (fm.containsKey(Cnst.IS_REL)) {
                    String rv = Synt.asString(fm.remove(Cnst.IS_REL));
                    if ("NULL".equalsIgnoreCase(rv)) {
                        caze.filter(fn + " IS NULL");
                    } else
                    if ("FILL".equalsIgnoreCase(rv)) {
                        caze.filter(fn + " IS NOT NULL");
                    }
                }

                // 数值区间查询
                Set ir = null;
                if (fm.containsKey(Cnst.RN_REL)) {
                    ir = Synt.setOf(fm.remove(Cnst.RN_REL));
                } else
                if (fm.containsKey(Cnst.ON_REL)) {
                    ir = Synt.asSet(fm.remove(Cnst.ON_REL));
                }
                if (ir != null && !ir.isEmpty()) {
                    StringBuilder sb = new StringBuilder( );
                    List sp = new ArrayList();
                    int i = 0;
                    for(Object v : ir) {
                        Object[] a = Synt.toRange(v);
                        if (a != null) {
                            StringBuilder sd = new StringBuilder();
                            if (a[0] != null) {
//                              if (sd.length() > 0) {
//                                  sd.append(" AND ");
//                              }
                                if ((boolean) a[2] ) {
                                    sd.append(fn).append(" >= ?");
                                    sp.add  ( a[0] );
                                } else {
                                    sd.append(fn).append( " > ?");
                                    sp.add  ( a[0] );
                                }
                            }
                            if (a[1] != null) {
                                if (sd.length() > 0) {
                                    sd.append(" AND ");
                                }
                                if ((boolean) a[3] ) {
                                    sd.append(fn).append(" <= ?");
                                    sp.add  ( a[1] );
                                } else {
                                    sd.append(fn).append( " < ?");
                                    sp.add  ( a[1] );
                                }
                            }
                            if (sd.length() > 0) {
                                sb.append("(").append(sd).append(") OR ");
                                i ++;
                            }
                        }
                    }
                    if (sb.length() > 0) {
                        sb.setLength(sb.length() - 4);
                        if (i == 1) {
                            caze.filter(    sb.toString()    , sp.toArray());
                        } else {
                            caze.filter("("+sb.toString()+")", sp.toArray());
                        }
                    }
                }

                // 清除不支持的
                for(String rl : NOLS) {
                    if (fm.remove(rl) != null) {
                        CoreLogger.trace(AssocCase.class.getName()+": Can not support "+fn+"."+rl);
                    }
                }

                // 清除功能参数
                for(String rl : FUNC) {
                    if (fm.remove(rl) != null) {
                        CoreLogger.trace(AssocCase.class.getName()+": Can not dispose "+fn+"."+rl);
                    }
                }

                // 清除空字符串, 如想要 IN ('') 可使用 xx.in=
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
        group(caze, Synt.asSet(rd.get(Cnst.OR_KEY)), "OR" );
        group(caze, Synt.asSet(rd.get(Cnst.AR_KEY)), "AND");
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
                String wh = FetchCase.PRE_WHERE.matcher(caxe.wheres).replaceFirst("");
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
            Map xf  = allowDiffs (af);
            if (xf != null) {
                return xf ;
            }
        }

        // 继承树, 列举和搜索为根
        if (af == null) {
        if (on != null) switch (on) {
            case LISTABLE:
            case SRCHABLE:
                af =  new HashMap();
                break;
            case WORDABLE:
                af = allow(SRCHABLE);
                if (af == null) {
                    af =  new HashMap();
                }
                break;
            case RATEABLE:
                af = allow(FINDABLE);
                if (af == null) {
                    af =  new HashMap();
                }
                break;
            default:
                af = allow(LISTABLE);
                if (af == null) {
                    af =  new HashMap();
                }
            } else {
                af = allow(LISTABLE);
                if (af == null) {
                    af =  new HashMap();
                }
            }
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
        String n = that.getName( );
        Map al = new LinkedHashMap();

        for(Object ot : af.entrySet()) {
            Map.Entry et = (Map.Entry) ot;
            String k = (String) et.getKey(  );
            String f = (String) et.getValue();
            if (CNPT.matcher(f).matches()) {
                f = "`"+n+"`.`"+ f +"`";
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
            if (CNPT.matcher(f).matches()) {
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

        return this;
    }

    /**
     * 从库表设置许可字段
     * @param table
     * @return
     */
    public AssocCase allow(Table table) {
        String cs;
        Map ps = table.getParams();
        cs = (String) ps.get("listable");
        if (cs != null) allow(LISTABLE, cs.trim().split("\\s*,\\s*"));
        cs = (String) ps.get("srchable");
        if (cs != null) allow(SRCHABLE, cs.trim().split("\\s*,\\s*"));
        cs = (String) ps.get("sortable");
        if (cs != null) allow(SORTABLE, cs.trim().split("\\s*,\\s*"));
        cs = (String) ps.get("findable");
        if (cs != null) allow(FINDABLE, cs.trim().split("\\s*,\\s*"));
        cs = (String) ps.get("wordable");
        if (cs != null) allow(WORDABLE, cs.trim().split("\\s*,\\s*"));
        cs = (String) ps.get("rateable");
        if (cs != null) allow(RATEABLE, cs.trim().split("\\s*,\\s*"));
        cs = (String) ps.get("saveable");
        if (cs != null) allow(SAVEABLE, cs.trim().split("\\s*,\\s*"));
        else {
            /**
             * 此处未将 SRCHTABLE 设为当前所有字段
             * 模糊搜索比较特殊
             * 文本类型字段才行
             * 并不便于自动指派
             */
            Map af = new LinkedHashMap();
            String name = Synt.defoult(
                   that.getName( ) , table.name , table.tableName);
            allow(table, table, table.getAssocs(), name, null, af);
            bufs.put(LISTABLE, af);
        }

        return this;
    }

    private void allow(Table table, Table assoc, Map ac, String tn, String qn, Map al) {
        /**
         * 三个变量: 层级名(qn), 名前缀(ax), 表前缀(tx)
         * 第一层时, 无需加名前缀, 无关联表前缀也不用加
         * 第二层时, 需将表名作为名前缀, 下级需带层级名
         */

        String  tx , ax  ;
        tx = "`"+tn+"`." ;

        if (null ==  qn  ) {
            qn = "";
            ax = "";
        } else
        if ("".equals(qn)) {
            qn = tn;
            ax = qn + ".";
        } else {
            qn = qn + "."+ tn ;
            ax = qn + ".";
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
        } catch (HongsException e ) {
            throw e.toExemption(  );
        }

        if (ac == null || ac.isEmpty()) {
            return;
        }

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

            tn = (String) et.getKey(  );
            ac = (Map) tc.get("assocs");

            // 获取真实的表名, 构建关联表实例
            String rn;
            rn = (String) tc.get("tableName");
            if (rn == null || "".equals (rn)) {
                rn = (String) tc.get ("name");
            }

            try {
                assoc = table.db.getTable(rn);
            } catch (HongsException e ) {
                throw e.toExemption(  );
            }
            if (null == assoc) {
                throw new HongsExemption(0x1039,
                    "Can not get table '"+ rn +"' in DB '"+ table.db.name +"'"
                );
            }

            allow(table, assoc, ac, tn, qn, al);
        }
    }

    /*
    public static void main(String[] args) {
        FetchCase fc = new FetchCase()
                .from("abc", "a")
                .join("def", "d", "id = :d_id")
                .join("xyz", "x", "a_id = :id");
        AssocCase ac = new AssocCase(fc)
                .allow(AssocCase.LISTABLE,
                        "f1", "a1:f2", "a2:CONCAT('$', .f3)",
                        "#b :d",
                        "f1", "a1:f2", "a2:CONCAT('$',d.f3)",
                        "#x_",
                        "f1", "a1:f2", "a2:CONCAT(x.f3,'%')")
                .parse(Synt.mapOf(
                        "rb", "f1,a2,b.a2",
                        "ob", "f2,a2,x_a2",
                        "x_a1", "123"
                ));
        io.github.ihongs.util.Data.dumps(ac.opts);
        System.err.println(fc.toString( ));
    }
    */

}
