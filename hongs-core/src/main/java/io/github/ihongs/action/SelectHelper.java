package io.github.ihongs.action;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.dh.MergeMore;
import io.github.ihongs.util.Dict;
import io.github.ihongs.util.Dist;
import io.github.ihongs.util.Synt;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Date;
import java.util.regex.Pattern;

/**
 * 选项补充助手
 * 可针对表单的 enum,form,fork,file,date 类字段进行数据补全和关联
 * @author Hong
 */
public class SelectHelper {

    public  final static  byte TEXT =  1;
    public  final static  byte TIME =  2;
    public  final static  byte LINK =  4;
    public  final static  byte FORK =  8;
    public  final static  byte FALL = 64;
    public  final static  byte ENFO = 16;
    public  final static  byte INFO = 32;

    private final static  Pattern  HOSTP = Pattern.compile("^(\\w+:)?//");
    private final static  Pattern  SERVP = Pattern.compile("^\\$\\{?SER");
    private final static  Pattern  DEFTP = Pattern.compile( "^=[~@#$%]" );

    private final Map<String, Object> infos;
    private final Map<String, Map> enums;
    private final Map<String, Map> forms;
    private final Map<String, Map> forks;
    private final Set<String     > files;
    private final Set<String     > dates;

    private String _href = null;
    private String _path = null;
    private Set    _cols = null;
    private Set    _ords = null;

    public SelectHelper() {
        infos = new LinkedHashMap();
        enums = new LinkedHashMap();
        forms = new LinkedHashMap();
        forks = new LinkedHashMap();
        files = new LinkedHashSet();
        dates = new LinkedHashSet();
    }

    /**
     * 添加默认取值
     * @param name
     * @param val
     * @return
     */
    public SelectHelper addInfo(String name, Object val) {
        infos.put(name, val );
        return this;
    }

    /**
     * 添加枚举选项
     * @param name
     * @param vals {value_code: value}
     * @return
     */
    public SelectHelper addEnum(String name, Map vals) {
        enums.put(name, vals);
        return this;
    }

    /**
     * 添加子表字段
     * @param name
     * @param fies {field_name: param}
     * @return
     */
    public SelectHelper addForm(String name, Map fies) {
        forms.put(name, fies);
        return this;
    }

    /**
     * 添加关联参数
     * @param name
     * @param pars {param_name: value}
     * @return
     */
    public SelectHelper addFork(String name, Map pars) {
        forks.put(name, pars);
        return this;
    }

    /**
     * 添加文件路径字段, 会将值补全为完整的链接
     * @param name
     * @return
     */
    public SelectHelper addFile(String... name) {
        files.addAll(Arrays.asList(name));
        return  this;
    }

    /**
     * 添加日期时间字段, 会将值转换为标准时间戳
     * @param name
     * @return
     */
    public SelectHelper addDate(String... name) {
        dates.addAll(Arrays.asList(name));
        return  this;
    }

    /**
     * 设置文件链接前缀
     * @param href 格式 scheme://domain[:port]
     * @param path 格式 /service/context/path
     * @return
     */
    public SelectHelper setLink(String href, String path) {
        _href = href;
        _path = path;
        return  this;
    }

    /**
     * 设置全局排序字段, 可以从中提取下级排序表
     * @param ob
     * @return
     */
    public SelectHelper setSortsInForm(Set ob ) {
      if (ob != null && ob.isEmpty()) {
          ob  = null;
      }
        _ords =  ob ;
        return  this;
    }

    /**
     * 设置表单可选字段, 需先于 addItemsByForm
     * @param rb
     * @return
     */
    public SelectHelper setItemsInForm(Set rb ) {
      if (rb != null && rb.isEmpty()) {
          rb  = null;
      }
        _cols =  rb ;
        return  this;
    }

    public SelectHelper addItemsByForm(Map fs ) throws HongsException {
        String conf = Dict.getValue( fs, "default", "@", "conf");
        String form = Dict.getValue( fs, "unknown", "@", "form");
        return addItemsByForm( conf, form, fs );
    }

    public SelectHelper addItemsByForm(String conf, String form) throws HongsException {
        Map fs = FormSet.getInstance(conf /**/)
                        .getForm    (form /**/);
        return addItemsByForm( conf, form, fs );
    }

    public SelectHelper addItemsByForm(String conf, String form, Map fs) throws HongsException {
        Map ts = FormSet.getInstance("default")
                        .getEnum ( "__types__");
        Iterator it = fs.entrySet().iterator( );

        while (it.hasNext()) {
            Map.Entry et = (Map.Entry)it.next();
            Map       mt = (Map ) et.getValue();
            String  name = (String) et.getKey();

            if (null != _cols && !_cols.contains(name)) {
                continue ;
            }

            Object  defo =          mt.get("default" );
            String  type = (String) mt.get("__type__");
                    type = (String) ts.get(   type   ); // 类型别名转换

            if (null != defo) {
                if (defo instanceof String) {
                DEF: {
                    String defs = (String) defo;
                    String typa = (String) mt.get("type"); // 细分类型
                    if (defs.startsWith("@" )) {
                    if (defs.startsWith("@@")) {
                        defs = defs.substring(1);
                    }  else {
                        break DEF;
                    }} else
                    if (defs.startsWith("=" )) {
                    if (defs.startsWith("==")) {
                        defs = defs.substring(1);
                    }  else {
                        break DEF;
                    }}  defo = infoAsType ( defs , typa ); // 校准类型
                    infos.put(name , defo);
                }} else {
                    infos.put(name , defo);
                }
            }

            if (null != type) {
                switch (type) {
                case "enum" : {
                    String xonf = (String) mt.get("conf");
                    String xame = (String) mt.get("enum");
                    if (null == xonf || "".equals( xonf )) xonf = conf;
                    if (null == xame || "".equals( xame )) xame = name;
                    Map xnum = FormSet.getInstance(xonf).getEnum(xame); // getEnumTranslated
                    enums.put(name , xnum);
                } break;
                case "form" : {
                    String xonf = (String) mt.get("conf");
                    String xame = (String) mt.get("form");
                    if (null == xonf || "".equals( xonf )) xonf = conf;
                    if (null == xame || "".equals( xame )) xame = name;
                    Map xnum = FormSet.getInstance(xonf).getForm(xame); // getFormTranslated
                    forms.put(name , xnum);
                } break;
                case "fork" : {
                    Map xnum = new HashMap(mt);
                    if (! mt.containsKey("data-at" )
                    &&  ! mt.containsKey("data-al")) {
                    if (! mt.containsKey("form")) {
                        xnum.put("form" , name.replace("_id", "")); // 去除其后缀
                    }
                    if (! mt.containsKey("conf")) {
                        xnum.put("conf" , conf);
                    }
                    }
                    forks.put(name , xnum);
                } break;
                case "file" : {
                    String href = (String) mt.get("href");
                    if (href != null
                    && !HOSTP.matcher(href).find( )
                    && !SERVP.matcher(href).find()) {
                        files.add(name);
                    }
                } break;
                case "date" : {
                    String typa = (String) mt.get("type");
                    if (! "time"     .equals(typa )
                    &&  ! "timestamp".equals(typa)) {
                        dates.add(name);
                    }
                } break;
                }
            }
        }

        return this;
    }

    /**
     * 数据填充
     * @param values 返回数据
     * @param action 填充规则, 使用常量 ENUM,TEXT 等, 可用或运算传多个值
     */
    public void select(Map values, byte action) {
        boolean withText = TEXT == (TEXT & action);
        boolean withTime = TIME == (TIME & action);
        boolean withLink = LINK == (LINK & action);
        boolean withFork = FORK == (FORK & action);
        boolean withEnfo = ENFO == (ENFO & action);
        boolean withInfo = INFO == (INFO & action);
        boolean fallDown = FALL == (FALL & action);

        // 附带枚举数据
        if (withEnfo) {
            /**/ Map  data = (Map ) values.get("enfo");
            if (data == null) {
                data  = new LinkedHashMap();
                values.put( "enfo" , data );
            }   injectEnfo(  data  , enums);
        }

        // 附带默认数据
        if (withInfo) {
            /**/ Map  data = (Map ) values.get("info");
            if (data == null) {
                data  = new LinkedHashMap();
                values.put( "info" , data );
                injectInfo(  data  , infos);
                withInfo  =  false ;
            }
        }

        // 补全额外数据
        if (withText || withTime || withLink || withInfo) {
            /**/ Map  info = (Map ) values.get("info");
            List<Map> list = (List) values.get("list");
            if (info != null) {
                if (withInfo) injectDefs(info , infos);
                if (withText) injectText(info , enums);
                if (withTime) injectTime(info , dates);
                if (withLink) injectLink(info , files);
            }
            if (list != null) for ( Map  item : list ) {
                if (withInfo) injectDefs(item , infos);
                if (withText) injectText(item , enums);
                if (withTime) injectTime(item , dates);
                if (withLink) injectLink(item , files);
            }
        }

        // 补全关联数据
        if (withFork) {
            /**/ Map  info = (Map ) values.get("info");
            List<Map> list = (List) values.get("list");
            if (info != null) injectFork(info ,action);
            if (list != null) injectFork(list ,action);
        }

        // 填充下级表单
        if (fallDown) {
            inject(values, action);

            // 为规避因循环依赖导致故障
            // 限制填充规则为仅向下一层
            // 2021/03/27: 无需加此限制, 应该先验结构
            // inject(values , (byte) (action - FALL));
        }
    }

    /**
     * 深度补充
     * @param values 返回数据
     * @param action 填充规则, 使用常量 ENUM,TEXT 等, 可用或运算传多个值
     */
    public void inject(Map values, byte action) {
        List<Map> list = new ArrayList(1);
        if (values.containsKey("info")) {
            list.add   ((Map ) values.get("info"));
        }
        if (values.containsKey("list")) {
            list.addAll((List) values.get("list"));
        }

        Map xnum = null;
        if (ENFO == ( ENFO & action ) ) {
            xnum = (Map) values.get("enfo");
            if (xnum == null ) {
                xnum  = new LinkedHashMap();
                values.put( "enfo" , xnum );
            }
        }

        // 数据映射整理
        Map<String, List> maps = new HashMap();
        for(String fn : forms.keySet()) {
            maps.put( fn, new LinkedList(  ) );
        }
        for( Map info : list ) {
        for(String fn : forms.keySet()) {
            Object fv = info.get( fn );
            if (fv != null && ! "".equals(fv)) { // 需规避误把空值存成了空串
            if (fv instanceof Object [] ) {
                maps.get(fn).addAll(Arrays.asList((Object[]) fv));
            } else
            if (fv instanceof Collection) {
                maps.get(fn).addAll((Collection) fv);
            } else
            {
                maps.get(fn).add(fv);
            }}
        }}

        // 向下递归补充
        for(Map.Entry<String, Map> et : forms.entrySet()) {
            String  fn = et.getKey(  );
            Map fields = et.getValue();
            Map valuez = new HashMap();
            Map anum   = new HashMap();
            valuez.put( "enfo", anum );
            valuez.put( "list", maps.get(fn) );

            try {
                new SelectHelper()
                    . addItemsByForm( fields )
                    . select( valuez, action );
            } catch (HongsException e) {
                throw e.toExemption( );
            }

            // 将枚举向上并
            if (xnum != null && !anum.isEmpty())
            for(Object ot2 : anum.entrySet( )) {
                Map.Entry et2 = (Map.Entry) ot2;
                Object fn2 = et2.getKey(  );
                Object fv2 = et2.getValue();
                Dict.put( xnum, fv2, fn , fn2 );
            }
        }
    }

    public void injectEnfo(Map enfo) {
        injectEnfo(enfo, enums);
    }

    public void injectInfo(Map info) {
        injectInfo(info, infos);
    }

    public void injectDefs(Map info) {
        injectDefs(info, infos);
    }

    public void injectText(Map info) {
        injectText(info, enums);
    }

    public void injectTime(Map info) {
        injectLink(info, dates);
    }

    public void injectLink(Map info) {
        injectLink(info, files);
    }

    public void injectFork(Map info , byte ad ) {
        injectFork(Synt.listOf(info), /**/ ad );
    }

    public void injectFork(List<Map> list, byte ad ) {
        if ( forks.isEmpty() ) return ;

        ActionHelper ah = ActionHelper.newInstance();
        MergeMore    mm = new MergeMore( list );
        Map          cd = new HashMap();
        Map          rd = new HashMap();
        Set          ab = new HashSet();
        Set          rb = new HashSet();
        Map<String , Set> sb = new HashMap(); // 子级 rb
        Map<String , Set> ob = new HashMap(); // 子级 ob

        ah.setContextData(cd);
        ah.setRequestData(rd);

        // 传递 ab 参数
        if (TEXT==(TEXT & ad)) {
            ab.add( "_text" );
        }
        if (TIME==(TIME & ad)) {
            ab.add( "_time" );
        }
        if (LINK==(LINK & ad)) {
            ab.add( "_link" );
        }
        if (FORK==(FORK & ad)) {
            ab.add( "_fork" );
        }

        // 预先提取所有下级
        Set ts = new HashSet();
        for(Map.Entry et : forks.entrySet()) {
            Map    mt = (Map) et.getValue( );
            String fn = (String) et.getKey();

            String fk = (String) mt.get("data-fk"); // 关联外键
            String ak = (String) mt.get("data-ak"); // 数据放入此下

            if (ak == null || ak.isEmpty()) {
            if (fk == null || fk.isEmpty()) {
                if (fn.endsWith("_id")) {
                    int  ln = fn.length()-3;
                    ak = fn.substring(0,ln);
                } else {
                    ak = fn + "_fork";
                }
            } else {
                    ak = fn ;
            }}

            ts.add( ak );
        }

        // 子级 rb 参数
        if (_cols != null) {
            String t , f ;
            for(Object o : _cols ) {
                f = Synt.asString(o);
                int p  = f.indexOf(".");
                if (p != -1) {
                    t  = f.substring(0,p);
                    f  = f.substring(1+p);
                    if (!ts.contains( t )) continue;
                    Set nb  = sb.get( t );
                    if (nb == null) {
                        nb  = new LinkedHashSet ( );
                        sb.put(t, nb);
                    }   nb.add(f    );
                }
            }
        }

        // 子级 ob 参数
        if (_ords != null) {
            String t , f ;
            for(Object o : _ords ) {
                f = Synt.asString(o);
                int p  = f.indexOf(".");
                if (p != -1) {
                    t  = f.substring(0,p);
                    f  = f.substring(1+p);
                    if (!ts.contains( t )) continue;
                    Set nb  = ob.get( t );
                    if (nb == null) {
                        nb  = new LinkedHashSet ( );
                        ob.put(t, nb);
                    }   nb.add(f    );
                }
            }
        }

        for(Map.Entry et : forks.entrySet()) {
            Map    mt = (Map) et.getValue( );
            String fn = (String) et.getKey();

            String fk = (String) mt.get("data-fk"); // 关联外键

            // 建立映射, 清除空值可避免不必要的查询
            Map<Object, List> ms = mm.mapped( fk != null ? fk : fn );
            ms.remove(  ""  );
            ms.remove( null );
            if (ms.isEmpty( )) {
                continue;
            }

            String at = (String) mt.get("data-at"); // 关联动作路径
            String ak = (String) mt.get("data-ak"); // 数据放入此下
            String vk = (String) mt.get("data-vk"); // 关联字段
            String tk = (String) mt.get("data-tk"); // 名称字段

            if (at == null || at.isEmpty()) {
                String c = (String) mt.get("conf");
                String f = (String) mt.get("form");
                at  =  c +"/"+ f +"/search";
            }
            if (ak == null || ak.isEmpty()) {
            if (fk == null || fk.isEmpty()) {
                if (fn.endsWith("_id")) {
                    int  ln = fn.length()-3;
                    ak = fn.substring(0,ln);
                } else {
                    ak = fn + "_fork";
                }
            } else {
                    ak = fn ;
            }}
            if (vk == null || vk.isEmpty()) {
                vk =  Cnst.ID_KEY;
            }
            if (tk == null || tk.isEmpty()) {
                tk = "name" ;
            }

            rd.clear();
            rb.clear();
            rb.add(vk);
            rb.add(tk);

            // 附加参数
            int ps = at.indexOf  ('?');
            if (ps > -1) {
              String aq;
                aq = at.substring(1 + ps).trim();
                at = at.substring(0 , ps).trim();
                if (aq.startsWith("{") && aq.endsWith("}")) {
                    rd.putAll( (Map) Dist . toObject (aq ));
                } else {
                    rd.putAll(ActionHelper.parseQuery(aq ));
                }
            }

            // 关联参数
            rd.put(vk, ms.keySet());
            rd.put(Cnst.RN_KEY, 0 );
            Set xb;
            xb = Synt.toTerms(rd.get(Cnst.AB_KEY));
            if (xb == null) {
                xb  = ab;
            }   rd.put(Cnst.AB_KEY, xb);
            xb = Synt.toTerms(rd.get(Cnst.RB_KEY));
            if (xb == null) {
                xb  = rb;
            }   rd.put(Cnst.RB_KEY, xb);

            // 请求参数
            Map rq  = Synt.toMap(mt.get("data-rd"));
            if (rq != null && !rq.isEmpty()) {
                rd.putAll( rq );
            }

            // 查询字段
            // 若内部有许可的且外部有指定的
            // 则取二者的交集作为查询的字段
            Set rp  = Synt.toSet(mt.get("data-rb"));
            if (rp != null && !rp.isEmpty()) {
                Set nb  = sb.get(ak);
                if (nb != null) {
                    nb.retainAll(rp);
                    if ( ! nb.isEmpty ( ) )
                    rd.put(Cnst.RB_KEY, nb);
                }
            }

            // 排序字段
            // 若内部有许可的且外部有指定的
            // 则取二者的交集作为排序的字段
            Set op  = Synt.toSet(mt.get("data-ob"));
            if (op != null && !op.isEmpty()) {
                Set nb  = ob.get(ak);
                if (nb != null) {
                    nb.retainAll(op);
                    if ( ! nb.isEmpty ( ) )
                    rd.put(Cnst.OB_KEY, nb);
                }
            }

            // 获取结果
            // 关联出错应在测试期发现并解决
            // 没有 ab 就没必要调用注解过滤
            try {
                ActionRunner ar = ActionRunner.newInstance( ah, at );
                if (xb.isEmpty ()) {
                    ar.doInvoke();
                } else {
                    ar.doAction();
                }
            } catch (HongsException e) {
                throw e.toExemption( );
            }

            // 整合数据
            Map sd  = ah.getResponseData( /**/ );
            List<Map> ls = (List) sd.get("list");
            if (ls == null) {
                continue;
            }
            if (Synt.declare(mt.get("__repeated__"), false) == false) {
                // 预置数据
                for (Map.Entry<Object, List> lr : ms.entrySet()) {
                    List<Map> lst = lr. getValue ( );
                    for (Map  row : lst) {
                        row.put(ak, new  HashMap ());
                    }
                }

                mm.extend(ls, ms, vk, ak);
            } else
            if (Synt.declare(mt.get("ordered"), false) == false) {
                // 预置数据
                for (Map.Entry<Object, List> lr : ms.entrySet()) {
                    List<Map> lst = lr. getValue ( );
                    for (Map  row : lst) {
                        row.put(ak, new ArrayList());
                    }
                }

                mm.append(ls, ms, vk, ak);
            } else
            {
                Set <Map>          ros = new  HashSet (list.size());
                List<Collection[]> fas = new ArrayList(list.size());

                // 预置数据
                for (Map.Entry<Object, List> lr : ms.entrySet()) {
                    List<Map> lst = lr.getValue();
                    for (Map  row : lst) {
                        if (ros.contains(row)) {
                            continue;
                        }
                        ros.add(row);
                        row.put(ak, new ArrayList());
                        fas.add(new Collection[] {
                            Synt.asColl(row.get(fn)),
                            Synt.asColl(row.get(ak))
                        });
                    }
                }
                ros = null;

                mm.append(ls, ms, vk, ak);

                // 重新排序
                for (Collection[] fa : fas) {
                     Collection fv = fa [0];
                     Collection av = fa [1];
                     if (av.isEmpty()) continue;
                     Map am = new HashMap();
                     for(Map r : (Collection<Map>) av) {
                         am.put( r.get(vk), r );
                     }
                     av.clear(); // 清空准备按顺序重新写入
                     for(Object v : fv) {
                         Object r = am.get( v );
                         if (r == null) {
                             r  = new HashMap();
                         }
                         av.add(r);
                     }
                }
                fas = null;
            }
        }
    }

    private void injectEnfo(Map enfo, Map maps) {
        Iterator it = maps.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry et = (Map.Entry) it.next();
            String   key = (String)  et.getKey();
            Map      map = (Map)   et.getValue();
            List     lst = new ArrayList(map.size());

            Iterator i = map.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry e = (Map.Entry) i.next();
                Object k = e.getKey  (  );
                Object v = e.getValue(  );
                List a = new ArrayList(2);
                a.add( k );
                a.add( v );
                lst.add(a);
            }

            enfo.put(key, lst);
        }
    }

    private void injectInfo(Map info, Map defs) {
        Iterator it = defs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry et = (Map.Entry) it.next();
            String   key = (String)  et.getKey();
            Object   def =         et.getValue();

            Dict.setValue(info, def, key);
        }
    }

    private void injectDefs(Map info, Map defs) {
        Iterator it = defs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry et = (Map.Entry) it.next();
            String   key = (String)  et.getKey();
            Object   def =         et.getValue();
            Object   val = Dict.get(info, Synt.LOOP.NEXT, key);

            if (val == Synt.LOOP.NEXT) {
                continue;
            }

            if (val == null || val.equals("")) {
                Dict.setValue(info, def, key);
            }
        }
    }

    private void injectText(Map info, Map maps) {
        Iterator it = maps.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry et = (Map.Entry) it.next();
            String   key = (String)  et.getKey();
            Map      map = (Map)   et.getValue();
            Object   val = Dict.get(info, Synt.LOOP.NEXT, key);

            if (val == Synt.LOOP.NEXT) {
                continue;
            }

            if (val instanceof Collection) {
                Collection col = (Collection ) val;
                Collection cxl = new ArrayList(col.size());
                Dict.setValue( info, cxl, key +  "_text" );
                for (Object vxl : col) {
                    vxl = codeToText(vxl, map);
                    cxl.add(vxl);
                }
            } else {
                    val = codeToText(val, map);
                Dict.setValue( info, val, key +  "_text" );
            }
        }
    }

    private void injectTime(Map info, Set keys) {
        Iterator it = keys.iterator();
        while (it.hasNext()) {
            String  key = (String) it.next();
            Object  val = Dict.get(info, Synt.LOOP.NEXT, key);

            if (val == Synt.LOOP.NEXT) {
                continue;
            }

            if (val instanceof Collection) {
                Collection col = (Collection ) val;
                Collection cxl = new ArrayList(col.size());
                Dict.setValue( info, cxl, key +  "_time" );
                for (Object vxl : col) {
                    vxl = dataToTime(vxl);
                    cxl.add(vxl);
                }
            } else {
                    val = dataToTime(val);
                Dict.setValue( info, val, key +  "_time" );
            }
        }
    }

    private void injectLink(Map info, Set keys) {
        if (_href == null) {
            _href  = Core.SERVER_HREF.get ();
        }
        if (_path == null) {
            _path  = Core.SERVER_PATH.get ();
        }

        Iterator it = keys.iterator();
        while (it.hasNext()) {
            String  key = (String) it.next();
            Object  val = Dict.get(info, Synt.LOOP.NEXT, key);

            if (val == Synt.LOOP.NEXT) {
                continue;
            }

            if (val instanceof Collection) {
                Collection col = (Collection ) val;
                Collection cxl = new ArrayList(col.size());
                Dict.setValue( info, cxl, key +  "_link" );
                for (Object vxl : col) {
                    vxl = hrefToLink(vxl);
                    cxl.add(vxl);
                }
            } else {
                    val = hrefToLink(val);
                Dict.setValue( info, val, key +  "_link" );
            }
        }
    }

    /**
     * 将枚举代号转换为对应文本
     * 空值和空串不处理
     * 横杠代表未知选项
     * @param map
     * @param val
     * @return
     */
    private Object codeToText(Object val, Map map) {
        if (null == val || "".equals(val)) {
            return  val;
        }

        Object vxl;
        val = Synt.asString(val); // 规避取值为非字串
        vxl = map.get(val);
        if (null != vxl) {
            return  vxl;
        }
        vxl = map.get("-");
        if (null != vxl) {
            return  vxl;
        }

        return val;
    }

    /**
     * 将字段值转换为对应时间戳
     * @param val
     * @return
     */
    private Object dataToTime(Object val ) {
        if (null == val || "".equals(val)) {
            return  val;
        }

        if (val instanceof Date) {
            return ((Date) val ).getTime();
        } else
        if (val instanceof Long) {
            return  val;
        } else
        {
            return  0  ;
        }
    }

    /**
     * 补全相对路径的域名和前缀
     * @param val
     * @return
     */
    private Object hrefToLink(Object val ) {
        if (null == val || "".equals(val)) {
            return  val;
        }

        String url  =  val.toString (   );
        if (HOSTP.matcher (url).find(   )) {
            return  val;
        } else
        if (url.startsWith("/")) {
            return _href + url;
        } else
        {
            return _href +_path +"/"+ url;
        }
    }

    private Object infoAsType(String val, String type) {
        if (   "int".equals(type)) {
            return Synt.declare(val, 0 );
        } else
        if (  "long".equals(type)) {
            return Synt.declare(val, 0L);
        } else
        if ( "float".equals(type)) {
            return Synt.declare(val, 0F);
        } else
        if ("double".equals(type)
        ||  "number".equals(type)) {
            return Synt.declare(val, 0D);
        } else
        if ( "short".equals(type)) {
            return Synt.declare(val, (short) 0);
        } else
        if (  "byte".equals(type)) {
            return Synt.declare(val, (byte ) 0);
        } else
        {
            return val ;
        }
    }

}
