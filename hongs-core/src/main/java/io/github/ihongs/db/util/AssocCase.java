package io.github.ihongs.db.util;

import io.github.ihongs.Cnst;
import io.github.ihongs.CruxException;
import io.github.ihongs.CruxExemption;
import io.github.ihongs.db.Model;
import io.github.ihongs.db.Table;
import io.github.ihongs.dh.IFigure;
import io.github.ihongs.dh.JFigure;
import io.github.ihongs.util.Dict;
import io.github.ihongs.util.Syno;
import io.github.ihongs.util.Synt;
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
     * 可排序字段, 用于 FetchCase 的 Option, 未设置则取 LISTABLE
     */
    public  static final String  SORTABLE = "SORTABLE";
    /**
     * 可过滤字段, 用于 FetchCase 的 Option, 未设置则取 LISTABLE
     */
    public  static final String  FINDABLE = "FINDABLE";
    /**
     * 可匹配字段, 用于 FetchCase 的 Option
     */
    public  static final String  COMBABLE = "COMBABLE";
    /**
     * 可搜索字段, 用于 FetchCase 的 Option
     */
    public  static final String  SRCHABLE = "SRCHABLE";
    /**
     * 搜索的字段, 用于 FetchCase 的 Option, 未设置则取 SRCHABLE
     */
    public  static final String  RSCHABLE = "RSCHABLE";
    /**
     * 可区间查询, 用于 FetchCase 的 Option, 未设置则取 SORTABLE
     */
    public  static final String  RANKABLE = "RANKABLE";
    /**
     * 可存储字段, 用于 FetchCase 的 Option, 未设置则取 LISTABLE
     */
    public  static final String  SAVEABLE = "SAVEABLE";
    /**
     * 不可存字段, 用于 FetchCase 的 Option
     */
    public  static final String  UNSTORED = "UNSTORED";

    private static final Pattern ANPT = Pattern.compile("^[\\w\\.]+\\s*(:|$)");
    private static final Pattern CNPT = Pattern.compile("^[\\w]+$" );
    private static final Pattern SEPA = Pattern.compile("\\s*,\\s*");
    private static final Pattern WORD = Pattern.compile("[_%/\\[\\]]"); // SQL 通配符: _%/[]
    private static final Pattern WILD = Pattern.compile("[?*\\\\]"   ); // 通用通配符: ?*\

    private final FetchCase        that;
    private final Map<String, Map> opts;
    private final Map<String, Map> bufs;

    /**
     * 构造方法
     * @param caze 模板查询对象
     */
    public AssocCase(FetchCase caze) {
        if (caze == null) {
            throw new NullPointerException(AssocCase.class.getName()+": case can not be null");
        }
        this.that = caze;
        this.opts = new HashMap();
        this.bufs = new HashMap();
    }

    /**
     * 清除许可
     * @param an
     * @return
     */
    public AssocCase wipe(String an) {
        bufs.remove(an);
        opts.remove(an);
        return this;
    }

    /**
     * 禁用许可
     * @param an
     * @return
     */
    public AssocCase deny(String an) {
        Map em = new LinkedHashMap();
        bufs.remove(an);
        opts.put(an,em);
        return this;
    }

    private void allow(String f, Map af) {
        f = f.trim();
        if (f.isEmpty()) {
            return;
        }

        String kp = null; // 别名前缀
        String fp = null; // 字段前缀

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

            return;
        }

        Matcher m;
        String  k;

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
    public AssocCase allow(String an, Set<String> fs) {
        Map af = new LinkedHashMap();
        opts.put(an,af);
        bufs.remove(an);

        for(String f : fs) {
            allow( f , af);
        }

        return this;
    }
    public AssocCase allow(String an, String ...  fs) {
        Map af = new LinkedHashMap();
        opts.put(an,af);
        bufs.remove(an);

        for(String f : fs) {
            allow( f , af);
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
        String[] ks = new String[] {LISTABLE, FINDABLE, SORTABLE, COMBABLE, SRCHABLE, RSCHABLE, RANKABLE};
        for(String k : ks) {
            k = k.toLowerCase( );
            Object s = fc.get(k);
            if (null == s) {
                continue ;
            }
            allow( k , Synt.toSet(s) );
        }
        return this;
    }
    public AssocCase allow(JFigure fc) {
        allow(LISTABLE, fc.getListable());
        allow(FINDABLE, fc.getFindable());
        allow(SORTABLE, fc.getSortable());
        allow(COMBABLE, fc.getCombable());
        allow(SRCHABLE, fc.getSrchable());
        allow(RSCHABLE, fc.getRschable());
        allow(RANKABLE, fc.getRankable());
        return this;
    }
    public AssocCase allow(IFigure fc) {
        allow(LISTABLE, fc.getListable());
        allow(FINDABLE, fc.getFindable());
        allow(SORTABLE, fc.getSortable());
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
        Map<String, String> uf = allow(UNSTORED);
        Map sd = new HashMap ( rd.size());

        for(Map.Entry<String, String> et : af.entrySet()) {
            String fc = et.getValue();
            String fn = et.getKey(  );
            if (uf.containsKey ( fn )) {
                continue;
            }
            Object fv = rd.get ( fn );
            if (fv != null) {
                sd.put(fc, fv);
            }
        }

        return  sd;
    }

    //** 内部工具方法 **/

    protected void parse(FetchCase caze, Map rd) {
        if (rd == null || rd.isEmpty()) return;

        if (! caze.hasField()) {
            field(caze, Synt.toTerms(rd.remove(Cnst.RB_KEY)));
        }
        if (! caze.hasOrder()) {
            order(caze, Synt.toTerms(rd.remove(Cnst.OB_KEY)));
        }

        whose(caze, Synt.toWords(rd.remove(Cnst.WD_KEY)));

        where(caze, rd);
    }

    protected void field(FetchCase caze, Set<String> rb) {
        if (rb == null || rb.isEmpty()) return;
        Map<String, String > af = allow(LISTABLE);
        if (af == null || af.isEmpty()) return;

        Map<String, Set>  cf = new HashMap();
        Set<String> ic = new LinkedHashSet(); // 包含的字段
        Set<String> ec = new LinkedHashSet(); // 排除的字段
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

        for(String fn : rb) {
            if (fn.  endsWith("!") ) { // 新的排除后缀
                fn = fn.substring(0, fn.length() - 1);
                xc = ec;
            } else
            if (fn.startsWith("-") ) { // 旧的排除前缀
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

        // 取差集排除掉字段
        if (ec.isEmpty() == false) {
            ic.removeAll(ec);
        }

        for(String  fn : ic) {
            String  fv = af.get(fn);
            caze.select(fv +" AS `"+ fn +"`");
        }
    }

    protected void order(FetchCase caze, Set<String> ob) {
        if (ob == null || ob.isEmpty()) return;
        Map<String, String> af = allow(SORTABLE);
        if (af == null || af.isEmpty()) return;

        for(String fn : ob) {
            boolean  ds;
            if (fn.  endsWith("!") ) { // 新的逆序后缀
                fn = fn.substring(0, fn.length() - 1);
                ds = true ;
            } else
            if (fn.startsWith("-") ) { // 旧的逆序前缀
                fn = fn.substring(1);
                ds = true ;
            } else {
                ds = false;
            }

            if (! af.containsKey(fn)) {
                continue;
            }

            fn = af.get( fn );
            if (ds && !fn.endsWith(" DESC")) {
                fn += " DESC";
            }
            caze.assort( fn );
        }
    }

    protected void whose(FetchCase caze, Set<String> wd) {
        if (wd == null || wd.isEmpty()) return;
        Map<String, String> af = allow(RSCHABLE);
        if (af == null || af.isEmpty()) return;

        int  i = 0;
        int  l = wd.size( ) * af.size( );
        Object[]      ab = new Object[l];
        Set<String>   xd = new HashSet();
        StringBuilder sb = new StringBuilder();

        // 转义待查词, 避开通配符, 以防止歧义
        for(String  wb : wd) {
            xd.add("%" + Syno.escape(wb , "/%_[]" , "/") + "%");
        }

        for(String  fn : af.values()) {
            sb.append("(");
            for(String wb : xd) {
                ab[ i++ ] = wb;
                sb.append(fn).append(" LIKE ? ESCAPE '/' AND ");
            }
            sb.setLength(sb.length() - 5);
            sb.append(") OR " );
        }   sb.setLength(sb.length() - 4);

        if (af.size( ) > 1 ) {
            caze.filter ("("+sb.toString()+")", ab);
        } else {
            caze.filter (    sb.toString()    , ab);
        }
    }

    protected void where(FetchCase caze, Map rd) {
        if (rd == null || rd.isEmpty()) return;

        Map<String, String> af = allow(FINDABLE);
        Map<String, String> rf = allow(RANKABLE);
        Map<String, String> ef = allow(COMBABLE);
        Map<String, String> sf = allow(SRCHABLE);

        Set<String>  ks  =  new  LinkedHashSet();
        ks.addAll(af.keySet());
        ks.addAll(rf.keySet());
        ks.addAll(sf.keySet());

        for(String kn : ks) {
            Object vv = Dict.getParam( rd , kn );
            String fn ;
            Object vo ;

            if ( vv == null ) {
                continue;
            }

            Map  vm  = vv instanceof Map ? (Map) vv : null;

            // 常规条件
            if ( vm == null ) {
            fn = af.get( kn );
            if ( fn != null ) {

            if ( vv instanceof Collection
            ||   vv instanceof Object[] ) {
                Set vs = Synt.asSet( vv );
                    vs.remove("");
                if(!vs.isEmpty( )) {
                    caze.filter(fn+" IN (?)", vs);
                }
            } else {
                if(!vv.equals("")) {
                    caze.filter(fn+  " = ?" , vv);
                }
            }

            } // End fn
                continue;
            } // End vm

            /*
            // 条件关系
            // 因可更改符号含义, 可能造成严重漏洞, 故废弃
            vo = vm.get(Cnst.OR_REL);
            if (Cnst.OR_KEY.equals(vo )
            ||  Cnst.NR_KEY.equals(vo)) {
                List r2 = new ArrayList(vm.size() - 1);
                for(Object ot : vm.entrySet()) {
                    Map.Entry et = (Map.Entry) ot;
                    Object k2 = et.getKey  ();
                    if (! Cnst.OR_KEY.equals(k2)) {
                    Object v2 = et.getValue();
                        r2.add(Synt.mapOf(kn,Synt.mapOf(k2,v2)));
                    }
                }
                if ( !  r2.isEmpty(  )) {
                    where(caze,Synt.mapOf(vo,r2));
                }
                continue;
            }
            */

            // 一般条件
            fn = af.get( kn );
            if ( fn != null ) {

            vo = vm.get(Cnst.IS_REL);
            if ( vo != null ) {
                String   vn = Synt.asString(vo).toUpperCase();
                switch ( vn ) {
                    case "EMPTY":
                        caze.filter(fn+ " = ''");
                        break;
                    case "NOT-EMPTY":
                        caze.filter(fn+" != ''");
                        break;
                    case "NULL" :
                        caze.filter(fn+" IS NULL");
                        break;
                    case "NOT-NULL" :
                        caze.filter(fn+" IS NOT NULL");
                        break;
                    case "NONE" :
                        caze.filter("("+fn+" IS NULL OR "+fn+" = '')");
                        break;
                    case "NOT-NONE" :
                        caze.filter("("+fn+" IS NOT NULL AND "+fn+" != '')");
                        break;
                    default:
                        throw new UnsupportedOperationException("Unsupported `is`: "+vo);
                }
            }

            // IN/NI 可以拆字符串, 如 fn.in=1,2 同 fn.in.=1&fn.in.=2
            vo = vm.get(Cnst.IN_REL);
            if ( vo != null ) {
            Set  vs  = Synt.toSet (vo);
            if (!vs.isEmpty() ) {
                caze.filter(fn+/**/" IN (?)", vs );
            }}
            vo = vm.get(Cnst.NO_REL);
            if ( vo != null ) {
            Set  vs  = Synt.toSet (vo);
            if (!vs.isEmpty() ) {
                caze.filter(fn+" NOT IN (?)", vs );
            }}

            vo = vm.get(Cnst.EQ_REL);
            if ( vo != null && !"".equals(vo) ) {
                caze.filter(fn+ " = ?", alone(vo, kn, Cnst.EQ_REL));
            }
            vo = vm.get(Cnst.NE_REL);
            if ( vo != null && !"".equals(vo) ) {
                caze.filter(fn+" != ?", alone(vo, kn, Cnst.NE_REL));
            }

            } // End af

            // 区间查询
            fn = rf.get( kn );
            if ( fn != null ) {

            vo = vm.get(Cnst.GT_REL);
            if ( vo != null && !"".equals(vo) ) {
                caze.filter(fn+ " > ?", alone(vo, kn, Cnst.GT_REL));
            } else {
            vo = vm.get(Cnst.GE_REL);
            if ( vo != null && !"".equals(vo) ) {
                caze.filter(fn+" >= ?", alone(vo, kn, Cnst.GE_REL));
            }}

            vo = vm.get(Cnst.LT_REL);
            if ( vo != null && !"".equals(vo) ) {
                caze.filter(fn+ " < ?", alone(vo, kn, Cnst.LT_REL));
            } else {
            vo = vm.get(Cnst.LE_REL);
            if ( vo != null && !"".equals(vo) ) {
                caze.filter(fn+" <= ?", alone(vo, kn, Cnst.LE_REL));
            }}

            vo = vm.get(Cnst.AT_REL);
            if ( vo != null ) {
                Set ir = Synt.asSet(vo);
                if (ir != null && ! ir.isEmpty()) {
                    range(caze, ir, fn);
                }
            }

            } // End rf

            // 通配查询
            fn = ef.get( kn );
            if ( fn != null ) {

            vo = vm.get(Cnst.CO_REL);
            if ( vo != null && !"".equals(vo) ) {
                combs(caze, vo , fn , "LIKE");
            }
            vo = vm.get(Cnst.NC_REL);
            if ( vo != null && !"".equals(vo) ) {
                combs(caze, vo , fn , "NOT LIKE");
            }

            } // End ef

            // 模糊查询
            fn = sf.get( kn );
            if ( fn != null ) {

            vo = vm.get(Cnst.SE_REL);
            if ( vo != null && !"".equals(vo) ) {
                srchs(caze, vo , fn , "LIKE");
            }
            vo = vm.get(Cnst.NS_REL);
            if ( vo != null && !"".equals(vo) ) {
                srchs(caze, vo , fn , "NOT LIKE");
            }

            } // End sf

            // 2019/07/27 为避免歧义, 剩余值不再 IN
        }

        /**
         * 分组查询, 满足复杂的组合查询条件
         */

        where(caze, Synt.asSet(rd.get(Cnst.OR_KEY)), "OR" );
        where(caze, Synt.asSet(rd.get(Cnst.AR_KEY)), "AND");
        where(caze, Synt.asSet(rd.get(Cnst.NR_KEY)), "NOT");
    }

    protected void where(FetchCase caze, Set ar, String rn) {
        if (ar == null || ar.isEmpty()) return;

        String nr;
        if ("NOT".equals(rn)) {
            nr = "NOT ";
            rn = "AND" ;
        } else {
            nr = "" ;
        }

        StringBuilder sb = new StringBuilder();
        FetchCase   caxe = new FetchCase(    );

        for (Map rd : (Set<Map>) ar) {
            // 将查询用例里的条件清空
            // 然后将分组数据转为条件
            // 参数无需清空
            caxe.wheres.setLength(0);
            where(caxe, rd);

            if (caxe.wheres.length() > 0) {
                String wh = caxe.wheres.toString(   );
                sb.append('(').append(wh).append(')')
                  .append(' ').append(rn).append(' ');
            }
        }

        if (! sb.isEmpty() ) {
            sb.setLength(sb.length() - rn.length() - 2);
            if (caze.wheres.length() > 0) {
                caze.wheres.append(" AND ");
            }
            caze.wheres .append(nr).append('(')
                        .append(sb).append(')');
            caze.wparams.addAll( caxe.wparams );
        }
    }

    protected void range(FetchCase caze, Set ir, String fn) {
        StringBuilder sb = new StringBuilder( );
        List sp = new ArrayList();
        int i = 0;
        for(Object v : ir) {
            Object[] a = Synt.toRange(v);
            if (a != null) {
                StringBuilder sd = new StringBuilder();
                if (a[0] != null) {
//                  if (sd.length() > 0) {
//                      sd.append(" AND ");
//                  }
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
            if (i > 1 ) {
                caze.filter("("+sb.toString()+")", sp.toArray());
            } else {
                caze.filter(    sb.toString()    , sp.toArray());
            }
        }
    }

    protected void combs(FetchCase caze, Object fv, String fn, String rn) {
        String  fw = fv.toString();
        if (fw.length() <= 1) {
            return;
        }
        char fc = fw.charAt(0);
        fw = fw.substring( 1 );
        switch (fc) {
            case '^':
                fw = words(fw);
                fw = fw + "%" ;
                caze.filter(fn+" "+rn+" ? ESCAPE '/'", fw);
                break;
            case ':':
                fw = words(fw);
                fw = wilds(fw);
                caze.filter(fn+" "+rn+" ? ESCAPE '/'", fw);
                break;
            default:
                throw new CruxExemption(1050, "Wrong comb type for "+fn+" "+rn);
        }
    }

    protected void srchs(FetchCase caze, Object fv, String fn, String rn) {
        Set wd = Synt.toWords(fv);
        int  l = wd.size();
        int  i = 0 ;

        Object[]      ab = new Object[l];
        Set<String>   xd = new LinkedHashSet();
        StringBuilder sb = new StringBuilder();

        // 转义待查词, 避开通配符, 以防止歧义
        for(Object fo : wd) {
            String fw = fo.toString();
            xd.add("%"+words(fw)+"%");
        }

        sb.append("(");
        for(String wb : xd) {
            ab[ i++ ] = wb;
            sb.append(fn).append(" ").append(rn)
              .append(  " ? ESCAPE '/' AND "   );
        }
        sb.setLength(sb.length(  ) - 5);
        sb.append(")");

        caze.filter (sb.toString(), ab);
    }

    protected String words(String fw) {
        Matcher ma = WORD.matcher(fw);
        return  ma.replaceAll ("/$0");
    }

    protected String wilds(String fw) {
        Matcher ma = WILD.matcher(fw);
        StringBuffer sb = new StringBuffer();
        while (ma.find()) {
            String st = ma.group(0);
            switch ( st ) {
                case "\\": st = "/"; break;
                case "?" : st = "_"; break;
                case "*" : st = "%"; break;
            }
            ma.appendReplacement(sb, st);
        }   ma.appendTail (sb);
        return sb.toString(  );
    }

    protected Object alone(Object fv, String fn, String rn) {
        if (fv instanceof Map
        ||  fv instanceof Collection
        ||  fv instanceof Object [] ) {
            throw new CruxExemption(1050, "Wrong value type for "+fn+" "+rn);
        }
        return fv;
    }

    protected Map allow(String on) {
        Map af  = bufs.get ( on );
        if (af != null ) {
            return af;
        }

        af = allowCheck(on);
        bufs.put(on, af);
        return af;
    }

    protected Map allowCheck(String on) {
        Map af  = opts.get(on);

        // 相对查询字段增加, 删减
        if (af != null && !LISTABLE.equals(on)) {
            Map xf  = allowDiffs (af);
            if (xf != null) {
                return xf ;
            }
        }

        /**
         * 继承树, 列举和搜索为根
         * 存储需使用列举原始设置
         */
        if (on == null) {
            on  =  "" ;
        }
        if (af == null) {
            switch (on) {
            case LISTABLE:
            case SRCHABLE:
                af =  new HashMap(0);
                break;
            case SAVEABLE:
            case UNSTORED:
                af = opts.get (LISTABLE);
                if (af == null) {
                    af =  new HashMap(0);
                }
                break;
            case RSCHABLE:
                af = allow(SRCHABLE);
                if (af == null) {
                    af =  new HashMap(0);
                }
                break;
            case RANKABLE:
                af = allow(SORTABLE);
                if (af == null) {
                    af =  new HashMap(0);
                }
                break;
            default:
                af = allow(LISTABLE);
                if (af == null) {
                    af =  new HashMap(0);
                }
            }
        }

        if (SAVEABLE.equals(on)
        ||  UNSTORED.equals(on) ) {
            return allowSaves(af);
        } else {
            return allowTrans(af);
        }
    }

    protected Map allowDiffs(Map af) {
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

    protected Map allowTrans(Map af) {
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

    protected Map allowSaves(Map af) {
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
        if (cs != null) allow(LISTABLE, SEPA.split(cs.trim()));
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

        cs = (String) ps.get("sortable");
        if (cs != null) allow(SORTABLE, SEPA.split(cs.trim()));

        cs = (String) ps.get("findable");
        if (cs != null) allow(FINDABLE, SEPA.split(cs.trim()));

        cs = (String) ps.get("rankable");
        if (cs != null) allow(RANKABLE, SEPA.split(cs.trim()));

        cs = (String) ps.get("combable");
        if (cs != null) allow(COMBABLE, SEPA.split(cs.trim()));

        cs = (String) ps.get("srchable");
        if (cs != null) allow(SRCHABLE, SEPA.split(cs.trim()));
        cs = (String) ps.get("rschable");
        if (cs != null) allow(RSCHABLE, SEPA.split(cs.trim()));

        cs = (String) ps.get("saveable");
        if (cs != null) allow(SAVEABLE, SEPA.split(cs.trim()));
        cs = (String) ps.get("unstored");
        if (cs != null) allow(UNSTORED, SEPA.split(cs.trim()));

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
        } catch (CruxException e ) {
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
            } catch ( CruxException e) {
                throw e.toExemption( );
            }
            if (null == assoc) {
                throw new CruxExemption(1026,
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
