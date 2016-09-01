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
 *      .trans(request);
 *  allow(table) 可换成 allow(form), form 可使用 FormSet.getForm 来得到
 * </pre>
 *
 * @author Hongs
 */
public class UniteCase {

    private final  FetchCase     temp;

    private static final String  fnPn = "[a-z][a-z0-9_]*";
    private static final Pattern cnPt
          = Pattern.compile("^"+ fnPn +"$",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern anPt
          = Pattern.compile("^(?:"+ fnPn +"\\.)*"+fnPn+"(?:\\:|$)",
            Pattern.CASE_INSENSITIVE);

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
    public  static final String  FILTABLE = "FILTABLE";
    /**
     * 可模糊搜索, 用于 FetchCase 的 Option
     */
    public  static final String  FINDABLE = "FINDABLE";

    /**
     * 构造方法
     * @param temp 模板查询对象
     */
    public UniteCase(FetchCase temp) {
        if (temp == null) {
            throw new NullPointerException(UniteCase.class.getName()+": temp can not be null");
        }
        this.temp = temp;
    }

    /**
     * 从模型设置许可字段
     * @param model
     * @return
     */
    public UniteCase allow(Model model) {
        allow(model.table);
        return this;
    }

    /**
     * 从库表设置许可字段
     * @param table
     * @return
     */
    public UniteCase allow(Table table) {
        Map af = allow(table, table, table.getAssocs(), null, null);
        temp.setOption(LISTABLE, af);
        temp.setOption(SORTABLE, af);
        temp.setOption(FILTABLE, af);
        temp.setOption(FINDABLE, af);
        return this;
    }

    /**
     * 自定义许可字段
     * @param an
     * @param fs
     * @return
     */
    public UniteCase allow(String an, String... fs) {
        Map af = new LinkedHashMap();
        Matcher m;
        String  k;
        for(String f : fs) {
            m = anPt.matcher(f);
            if (m.matches()) {
                if (m.group().endsWith(":")) {
                    k = f.substring(0, m.end() -1);
                    f = f.substring(   m.end()   );
                } else {
                    k = f;
                }
            } else {
                    k = f;
            }
            af.put( k , f );
        }
        temp.setOption(an, af);
        return this;
    }

    /**
     * 从表单设置许可字段
     * @param fc
     * @return
     */
    public UniteCase allow(Map fc) {
        String[] ks = new String[] {"listable", "sortable", "findable", "filtable"};
        for(String k : ks) {
            String s = Dict.getValue(fc, "", "@", k);
            if ("".equals(s)) continue;
            if (s.indexOf("'") != -1
            ||  s.indexOf('"') != -1
            ||  s.indexOf('(') != -1) {
                allow(k.toUpperCase(), s.split(","));
            } else {
                // TODO: 复杂字符串解析
            }
        }
        return this;
    }

    /**
     * 根据请求数据生成查询用例
     * 此处执行写当前 FetchCase
     * 故无法在设置好 allow 后反复 trans
     * @param rd
     * @return
     */
    public FetchCase trans(Map rd) {
        Map xd = new LinkedHashMap (rd);
        trans( temp, xd );
        return temp;
    }

    /**
     * 根据请求数据生成查询用例
     * 此处会克隆一份 FetchCase
     * 故可以在设置好 allow 后反复 tranz
     * @param rd
     * @return
     */
    public FetchCase tranz(Map rd) {
        FetchCase caze = temp.clone(  );
        Map xd = new LinkedHashMap (rd);
        trans( caze, xd );
        return caze;
    }

    //** 内部工具方法 **/

    private void trans(FetchCase caze, Map rd) {
        if (rd == null || rd.isEmpty()) return;

        field(caze, Synt.asTerms(rd.remove(Cnst.RB_KEY)));
        order(caze, Synt.asTerms(rd.remove(Cnst.OB_KEY)));
        query(caze, Synt.asTerms(rd.remove(Cnst.WD_KEY)));
        where(caze, rd);
    }

    private void field(FetchCase caze, Set<String> rb) {
        if (rb == null || rb.isEmpty()) return;

        Map<String, String> af = allow(caze, LISTABLE);
        Set<String> ic = new LinkedHashSet();
        Set<String> ec = new LinkedHashSet();
        Set<String> xc;

        for(String  fn : rb) {
            if (fn.startsWith("-") ) {
                fn = fn.substring(1);
                xc = ec;
            } else {
                xc = ic;
            }

            /**
             * 可以使用通配符来表示层级全部字段
             */

            if (fn.endsWith(".*" )) {
                fn = fn.substring(0, fn.length() - 1);
                for(String kn : af.keySet()) {
                    if (kn.startsWith( fn )) {
                        xc.add(kn);
                    }
                }
            } else
            if (fn.equals  ( "*" )) {
                for(String kn : af.keySet()) {
                    if (fn.indexOf('.') < 0) {
                        xc.add(kn);
                    }
                }
            } else
            if (af.containsKey(fn)) {
                xc.add(fn);
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

        Map<String, String> af = allow(caze, SORTABLE);

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

        Map<String, String> af = allow(caze, FINDABLE);
        int            i = 0 ;
        int            j = wd.size()*af.size();
        StringBuilder sb = new StringBuilder();
        Object[]      ab = new Object[   j   ];

        for(Map.Entry<String, String> et : af.entrySet()) {
            String fn = et.getValue();
            sb.append("(");
            for(String wb : wd) {
                sb.append(fn).append( " LIKE ? ESCAPE '/' AND " );
                ab[ i++ ] = "%"+Tool.escape(wb, "%_[]/", "/")+"%";
            }
            sb.setLength(sb.length() - 5);
            sb.append(") OR " );
        }
            sb.setLength(sb.length() - 4);

        caze.filter(sb.toString().substring(4) , ab);
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
                String wh = FetchCase.pw.matcher(caxe.wheres).replaceFirst("");
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

    private void where(FetchCase caze, Map rd) {
        if (rd == null || rd.isEmpty()) return;

        Map<String, String> af = allow(caze, FINDABLE);

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
                        CoreLogger.trace(UniteCase.class.getName()+": Can not set "+fn+" "+rn+" Collection");
                    }
                }

                // 清除功能参数
                for(String rl : func) {
                    fm.remove(rl);
                }

                // 如果还有剩余, 就当做 IN 来处理
                if (!fm.isEmpty()) {
                    fv = new LinkedHashSet( fm.values() );
                    caze.filter(fn+" IN (?)", fv);
                }
            } else
            if (fv instanceof Set) {
                caze.filter(fn+" IN (?)", fv);
            } else
            if (fv instanceof Collection) {
                fv = new LinkedHashSet(( Collection ) fv);
                caze.filter(fn+" IN (?)", fv);
            } else {
                caze.filter(fn+  " = ?" , fv);
            }
        }

        // 分组查询, 满足复杂的组合查询条件
        group(caze, Synt.declare(rd.get(Cnst.OR_KEY), Set.class), "OR" );
        group(caze, Synt.declare(rd.get(Cnst.AR_KEY), Set.class), "AND");
    }

    private Map allow(FetchCase caze, String on) {
        return  allow(caze, on, null, null);
    }

    private Map allow(FetchCase caze, String on, String qn, Map al) {
        String tn, tx, ax;

        if (null != caze.name && ! "".equals(caze.name)) {
            tn = caze.name /**/;
        } else {
            tn = caze.tableName;
        }

        /**
         * 三个变量: 层级名(qn), 名前缀(ax), 表前缀(tx)
         * 第一层时, 无需加名前缀, 无关联表前缀也不用加
         * 第二层时, 需将表名作为名前缀, 下级需带层级名
         */
        if (null ==  qn  ) {        // 第一层
            qn = "";
            ax = "";
        if (! joins(caze)) {        // 第一层且无关联可不加查询别名
            tx = "";
        } else {
            tx = "`" + tn + "`.";
        }
            al = new LinkedHashMap();
        } else
        if ("".equals(qn)) {        // 第二层
            qn = tn;
            ax = qn + ".";
            tx = "`" + tn + "`.";
        } else {                    // 其他层
            qn = qn + "." + tn  ;
            ax = qn + ".";
            tx = "`" + tn + "`.";
        }

        // 也可以在 FetchCase 中特别指定命名前缀
        if (null != caze.joinName && ! "".equals(caze.joinName) /*sub*/ ) {
            ax = caze.joinName + ".";
        }

        Map af = (Map) caze.getOption(   on   );
        if (af == null && ! LISTABLE.equals(on) && ! FINDABLE.equals(on)) {
            af = (Map) caze.getOption(LISTABLE);
        }

        if (af != null) // Check field :
        for(Object ot : af.entrySet()) {
            Map.Entry et = (Map.Entry) ot;
            String k = (String) et.getKey(  );
            if (cnPt.matcher(k).matches()) {
                k = ax +/**/ k /**/;
            }
            String f = (String) et.getValue();
            if (cnPt.matcher(f).matches()) {
                f = tx +"`"+ f +"`";
            }
            al.put(k, f);
        }

        for(FetchCase caxe : caze.getJoinSet() ) {
            if (caxe.joinType != FetchCase.NONE) {
                allow(caxe, on, qn, al);
            }
        }

        return  al;
    }

    private boolean joins(FetchCase caze) {
        for(FetchCase caxe : caze.getJoinSet() ) {
            if (caxe.joinType != FetchCase.NONE) {
                return true;
            }
        }
        return  false;
    }

    private Map allow(Table tabo, Table asso, Map ac, Map al, String qn){
        String tn, tx, ax;

        if (null != asso.name && ! "".equals(tabo.name)) {
            tn = asso.name /**/;
        } else {
            tn = asso.tableName;
        }

        // 类似于 allow(FetchCase, String, String, Map)
        if (null ==  qn  ) {
            qn = "";
            ax = "";
        if (! joins( ac )) {
            tx = "";
        } else {
            tx = "`" + tn + "`.";
        }
            al = new LinkedHashMap();
        } else
        if ("".equals(qn)) {
            qn = tn;
            ax = qn + ".";
            tx = "`" + tn + "`.";
        } else {
            qn = qn + "." + tn  ;
            ax = qn + ".";
            tx = "`" + tn + "`.";
        }

        try {
            Map fs = asso.getFields( );
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
            Iterator it = ac.entrySet( ).iterator();
            while (it.hasNext()) {
                Map.Entry et = (Map.Entry)it.next();
                Map    tc = (Map) et.getValue();
                String jn = (String) tc.get("join");

                // 不是 JOIN 的不理会
                if (! "INNER".equals(jn)
                &&  !  "LEFT".equals(jn)
                &&  ! "RIGHT".equals(jn)
                &&  !  "FULL".equals(jn)) {
                    continue;
                }

                try {
                    String tn2 = (String) et.getKey(   );
                    Table  at2 = tabo.getAssocInst(tn2);
                    Map    ac2 = (Map) tc.get ("assocs");
                    allow(tabo, at2, ac2, al, qn);
                }
                catch (HongsException ex) {
                    CoreLogger.error( ex);
                }
            }
        }

        return  al;
    }

    private boolean joins(Map ac) {
        if (ac != null && !ac.isEmpty()) {
            Iterator it = ac.entrySet( ).iterator();
            while (it.hasNext()) {
                Map.Entry et = (Map.Entry)it.next();
                Map    tc = (Map) et.getValue( );
                String jn = (String) tc.get("join");

                // 不是 JOIN 的不理会
                if (! "INNER".equals(jn)
                &&  !  "LEFT".equals(jn)
                &&  ! "RIGHT".equals(jn)
                &&  !  "FULL".equals(jn)) {
                    continue;
                }

                return  true;
            }
        }
        return  false;
    }

}
