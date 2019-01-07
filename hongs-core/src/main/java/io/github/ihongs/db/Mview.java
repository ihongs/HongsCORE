package io.github.ihongs.db;

import io.github.ihongs.Cnst;
import io.github.ihongs.CoreLocale;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.action.NaviMap;
import io.github.ihongs.util.Synt;
import java.sql.Types;
import java.util.Iterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
/*
import java.util.List;
import java.util.regex.Pattern;
*/

/**
 * 模型视图
 * @author Hongs
 */
public class Mview extends Model {

    private CoreLocale  locale = null;
    private Map         fields = null;
    private Set<String> lscols = null;
    private String      txkey  = null;
    private String      title  = null;

    /**
     * 构造方法
     *
     * 同 Model(Table)
     *
     * @param table
     * @throws HongsException
     */
    public Mview(Table table) throws HongsException {
        super(table);
    }

    /**
     * 构造方法
     *
     * 同 Mtree(Table)
     * 当临时动态从 Model 取 Mtree 时可使用此构造方法
     * 然后可以使用 getTitle,getFields 等提取表单信息
     *
     * 特别注意:
     * 如果 model 中覆盖了 add,put,del 和 filter 等等
     * 调用 Mview 中相同方法并不会使用此 model 的方法
     *
     * @param model
     * @throws HongsException
     */
    public Mview(Model model) throws HongsException {
        this(model.table);
    }

    /**
     * 获取主键字段名
     * @return
     * @throws HongsException
     */
    public String getIdKey()
    throws HongsException {
        return table.primaryKey;
    }

    /**
     * 获取名称字段名
     * @return
     * @throws HongsException
     */
    public String getTxKey()
    throws HongsException {
        if (null != txkey) {
            return  txkey;
        }

        Map<String, Map   > flds = getFields();

        /**
         * 有名称字段直接返回该字段即可
         */
        if (flds.containsKey("name")) {
            return "name";
        }

        Map<String, String> typs = FormSet.getInstance()
                                 . getEnum ("__types__");

        /**
         * 寻找第一个非隐藏的字符串字段
         */
        for(String name : lscols) {
            Map    item = flds.get(name);
            if ( item == null ) continue;
            String type = (String) item.get("__type__" );
            if ( type == null ) continue;
            String kind = typs.get(type);

            if ("string".equals(kind)
            && !"hidden".equals(type)
            && !Cnst.ID_KEY.equals(type)) {
                txkey = name;
                return  name;
            }
        }

        txkey  =  "";
        return txkey;
    }

    /**
     * 获取表单模型名称
     * @return
     * @throws HongsException
     */
    public String getTitle()
    throws HongsException {
        if (null != title) {
            return  title;
        }

        do {
            Map item;

            // 先从表单取名字
            item = getForm( );
            if (item != null  && item.containsKey(   "@"    )) {
                item  = ( Map  ) item.get(   "@"    );
            if (item != null  && item.containsKey("__text__")) {
                title = (String) item.get("__text__");
                break;
            }
            }

            // 再从菜单取名字
            item = getMenu( );
            if (item != null  && item.containsKey(  "text"  )) {
                title = (String) item.get(  "text"  );
                title = getLocale( ).translate(title);
                break;
            }

            // 最后配置取名字
            title = "fore.form."+ db.name +"."+ table.name +"@";
        } while (false);

        return getLocale().translate(title);

        /*
        String sql = "SHOW TABLE STATUS WHERE name = ?";
        List<Map<String, Object>> rows = db.fetchAll(sql, table.tableName);
        String dsp = (String)rows.get(0).get("Comment");
        if (null == dsp || "".equals(dsp)) {
            dsp = table.name;
        }
        return dsp;
        */
    }

    /**
     * 获取当前语言资源
     * @return
     */
    public final CoreLocale getLocale() {
        if (locale != null) return locale;
        locale = CoreLocale.getInstance().clone();
        locale.fill( db.name );
        locale.fill( db.name + "/" + table.name );
        return locale;
    }

    /**
     * 获取全部字段配置
     * 混合了表和表单的
     * @return
     * @throws HongsException
     */
    public Map getFields()
    throws HongsException {
        if (null != fields) {
            return  fields;
        }

        fields = getForm( );
        if (fields == null) {
            fields  =  new  LinkedHashMap( );
        } else {
            fields  =  new  LinkedHashMap( fields );
        }

        Map<String, Object> params = (Map) fields.get ("@");
        if (params == null) {
            params = new HashMap();
        }

        //** 追加表以及关联的字段 **/

        if (! Synt.declare(params.get("dont.append.fields"), false)) {
            addTableFields();
        }
        if (! Synt.declare(params.get("dont.append.assocs"), false)) {
            addAssocFields();
        }

        //** 预处理分类字段类型等 **/

        Map cases = FormSet.getInstance( ).getEnum("__cases__");
        Set names = fields.keySet( /***/ );
        names = new LinkedHashSet( names );
        names.remove("@");
        Object able;

        Set<String> listTypz = new HashSet();
        Set<String> sortTypz = new HashSet();
        Set<String> srchTypz = new HashSet();
        Set<String> fitrTypz = new HashSet();
        Set<String> listColz = new LinkedHashSet();
        Set<String> sortColz = new LinkedHashSet();
        Set<String> srchColz = new LinkedHashSet();
        Set<String> fitrColz = new LinkedHashSet();

        able = params.get("listable");
        if ("?".equals(able)) {
            listTypz = Synt.toSet(cases.get("listable"));
        } else
        if ("*".equals(able)) {
            listColz = names;
        } else
        {
            listColz = Synt.toSet(able);
        }

        able = params.get("sortable");
        if ("?".equals(able)) {
            sortTypz = Synt.toSet(cases.get("sortable"));
        } else
        if ("*".equals(able)) {
            sortColz = names;
        } else
        {
            sortColz = Synt.toSet(able);
        }

        able = params.get("srchable");
        if ("?".equals(able)) {
            srchTypz = Synt.toSet(cases.get("srchable"));
        } else
        if ("*".equals(able)) {
            srchColz = names;
        } else
        {
            srchColz = Synt.toSet(able);
        }

        able = params.get("findable");
        if ("?".equals(able)) {
            fitrTypz = Synt.toSet(cases.get("findable"));
        } else
        if ("*".equals(able)) {
            fitrColz = names;
        } else
        {
            fitrColz = Synt.toSet(able);
        }

        //** 检查列举、排序等参数 **/

        for(Map.Entry<String, Map> ent
            : ( ( Map<String, Map> ) fields ).entrySet( )) {
            Map field = ent.getValue();
            String fn = ent.getKey(  );
            String ft = Synt.declare(field.get("__type__"), "string");

            // 表单主体配置信息字段需排除掉
            if ("@".equals(fn)) {
                continue;
            }

            // 多值字段不能搜索、排序、列举
            if (Synt.declare(field.get("__repeated__"), false)) {
                continue;
            }

            if (field.containsKey("listable")) {
                if (Synt.declare(field.get("listable"), false)) {
                    listColz.add(fn);
                }
            } else {
                if (listTypz.contains(ft)) {
                    listColz.add(fn);
                    field.put("listable", "yes");
                } else
                if (listColz.contains(fn)) {
                    field.put("listable", "yes");
                }
            }

            if (field.containsKey("sortable")) {
                if (Synt.declare(field.get("sortable"), false)) {
                    sortColz.add(fn);
                }
            } else {
                if (sortTypz.contains(ft)) {
                    sortColz.add(fn);
                    field.put("sortable", "yes");
                } else
                if (sortColz.contains(fn)) {
                    field.put("sortable", "yes");
                }
            }

            if (field.containsKey("srchable")) {
                if (Synt.declare(field.get("srchable"), false)) {
                    srchColz.add(fn);
                }
            } else {
                if (srchTypz.contains(ft)) {
                    srchColz.add(fn);
                    field.put("srchable", "yes");
                } else
                if (srchColz.contains(fn)) {
                    field.put("srchable", "yes");
                }
            }

            if (field.containsKey("findable")) {
                if (Synt.declare(field.get("findable"), false)) {
                    fitrColz.add(fn);
                }
            } else {
                if (fitrTypz.contains(ft)) {
                    fitrColz.add(fn);
                    field.put("findable", "yes");
                } else
                if (fitrColz.contains(fn)) {
                    field.put("findable", "yes");
                }
            }
        }

        //** 填充对应的模型属性值 **/

        Map ps = table.getParams();
        if (! ps.containsKey("listable") && ! listColz.isEmpty()) {
            ps.put("listable", implode(listColz));
        }
        if (! ps.containsKey("sortable") && ! sortColz.isEmpty()) {
            ps.put("sortable", implode(sortColz));
        }
        if (! ps.containsKey("srchable") && ! srchColz.isEmpty()) {
            ps.put("srchable", implode(srchColz));
        }
        if (! ps.containsKey("findable") && ! fitrColz.isEmpty()) {
            ps.put("findable", implode(fitrColz));
        }

        lscols = listColz;

        return fields;
    }

    private String implode(Set<String> fs) {
        StringBuilder sb = new StringBuilder();
        for ( String  fn : fs ) {
            sb.append(fn).append(",");
        }
        sb.setLength (sb.length() -1);
        return sb.toString();
    }

    private void addTableFields() throws HongsException {
        getLocale();

        /*
        String sql = "SHOW FULL FIELDS FROM `"+table.tableName+"`";
        List<Map<String, Object>> rows = db.fetchAll(sql);
        for (Map<String, Object>  row  : rows) {
            String text = (String)row.get("Comment");
            String name = (String)row.get( "Field" );
            String type = (String)row.get( "Type"  );
        */
        Map<String, Map> cols = table.getFields();
        for(Map.Entry<String, Map> ent : cols.entrySet()) {
            Map     col  = ent.getValue();
            String  name = ent.getKey(  );
            Integer type = (Integer) col.get("type");
            Boolean rqrd = (Boolean) col.get("required");

            Map field = (Map) fields.get(name);
            if (field == null) {
                field =  new HashMap( );
                fields.put(name, field);
            }

            if (!field.containsKey("__required__") || "".equals(field.get("__required__"))) {
//              field.put("__required__", "NO".equals(row.get("Null")) ? "yes" : "");
                field.put("__required__", rqrd ? "yes" : "");
            }

            if (!field.containsKey("__text__") || "".equals(field.get("__text__"))) {
                String text = "core.form."+ db.name +"."+ table.name +"."+ name;
                if (null != locale.getProperty(text)) {
                    text  = locale.translate  (text);
                    field.put("__text__", text);
                } else {
                    field.put("__text__", name);
                }
            }

            if (!field.containsKey("__type__") || "".equals(field.get("__type__"))) {
                if ( name.equals(table.primaryKey) || name.endsWith("_id") ) {
                    field.put("__type__", "hidden");
                } else
                if (type == Types.DATE) {
                    field.put("__type__", "date");
                } else
                if (type == Types.TIME) {
                    field.put("__type__", "time");
                } else
                if (type == Types.TIMESTAMP) {
                    field.put("__type__", "datetime");
                } else
                if (type == Types.LONGVARCHAR || type == Types.LONGNVARCHAR) {
                    field.put("__type__", "textarea");
                } else
                if (type == Types.INTEGER || type == Types.TINYINT || type == Types.BIGINT || type == Types.SMALLINT
                ||  type == Types.NUMERIC || type == Types.DECIMAL || type == Types.DOUBLE || type == Types.FLOAT) {
                    field.put("__type__", "number");
                } else
                {
                    field.put("__type__", "string");
                }
                /*
                if (Pattern.compile("(decimal|numeric|integer|tinyint|smallint|float|double).*", Pattern.CASE_INSENSITIVE).matcher(type).matches()) {
                    field.put("__type__", "number");
                } else
                if (Pattern.compile("(datetime|timestamp).*", Pattern.CASE_INSENSITIVE).matcher(type).matches()) {
                    field.put("__type__", "datetime");
                } else
                if (Pattern.compile("(date)"  , Pattern.CASE_INSENSITIVE).matcher(type).matches()) {
                    field.put("__type__", "date");
                } else
                if (Pattern.compile("(time)"  , Pattern.CASE_INSENSITIVE).matcher(type).matches()) {
                    field.put("__type__", "time");
                } else
                if (Pattern.compile("(text).*", Pattern.CASE_INSENSITIVE).matcher(type).matches()) {
                    field.put("__type__", "textarea");
                }
                */
            }
        }
    }

    private void addAssocFields() throws HongsException {
        if (null == table.assocs) {
            return;
        }

        getInstance(this); // 确保当前对象在模型对象库中

        Iterator it = table.assocs.entrySet().iterator();
        while (  it.hasNext( )  ) {
            Map.Entry et = (Map.Entry)it.next();
            Map       vd = (Map ) et.getValue();
            String  type = (String) vd.get("type");

            String  name, text, vk, tk, ak, ek, tn;

            if ("BLS_TO".equals(type)) {
                ak   = (String) vd.get("name");
                tn   = (String) vd.get("tableName" );
                vk   = (String) vd.get("foreignKey");
                tn   = tn != null ? tn : ak;

                Model  hm = db.getModel(tn);
                Mview  hb = getInstance(hm);

                tk   = hb.getTxKey();
                text = hb.getTitle();
                name = vk;
            } else
            if ("HAS_ONE".equals(type)) {
                ak   = (String) vd.get("name");
                tn   = (String) vd.get("tableName" );
                vk   = (String) vd.get("foreignKey");
                tn   = tn != null ? tn : ak;

                Model  hm = db.getModel(tn);
                Mview  hb = getInstance(hm);

                tk   = hb.getTxKey();
                text = hb.getTitle();
                name = ak + "." + vk;
            } else
            if ("HAS_MANY".equals(type)) {
                ak   = (String) vd.get("name");
                tn   = (String) vd.get("tableName" );
                vk   = (String) vd.get("foreignKey");
                tn   = tn != null ? tn : ak;

                Model  hm = db.getModel(tn);
                Mview  hb = getInstance(hm);

                tk   = hb.getTxKey();
                text = hb.getTitle();
                name = ak + ".."+ vk;
            } else
            if ("HAS_MORE".equals(type)) {
                Map xd = (Map) vd.get("assocs");
                if (xd == null) {
                    continue;
                }

                Map ad = (Map) xd.values().toArray()[0];
                ak   = (String) vd.get("name");
                ek   = (String) ad.get("name");
                tn   = (String) ad.get("tableName" );
                vk   = (String) vd.get("foreignKey");
                tn   = tn != null ? tn : ek;

                Model  hm = db.getModel(tn);
                Mview  hb = getInstance(hm);

                tk   = hb.getTxKey();
                text = hb.getTitle();
                name = ak + ".."+ vk;
            } else {
                continue;
            }

            Map field = (Map) fields.get(name);
            if (field == null) {
                field =  new HashMap( );
                fields.put(name, field);

                field =  new HashMap( );
                fields.put(name, field);
                field.put("__type__","fork");
                field.put("__text__", text );
                field.put("data-ak", ak);
                field.put("data-vk", vk);
                field.put("data-tk", tk);
            }
        }
    }

    private Map getForm() throws HongsException {
        try {
            FormSet form = FormSet.hasConfFile(db.name+"/"+table.name)
                         ? FormSet.getInstance(db.name+"/"+table.name)
                         : FormSet.getInstance(db.name);
            return  form.getFormTranslated(/**/table.name/**/);
        } catch (HongsException ex) {
            if (ex.getErrno() != 0x10e8
            &&  ex.getErrno() != 0x10ea) {
                throw ex;
            }
            return  null;
        }
    }

    private Map getMenu() throws HongsException {
        try {
            NaviMap navi = NaviMap.hasConfFile(db.name+"/"+table.name)
                         ? NaviMap.getInstance(db.name+"/"+table.name)
                         : NaviMap.getInstance(db.name);
            return  navi.getMenu(db.name +"/"+ table.name+"/");
        } catch (HongsException ex) {
            if (ex.getErrno() != 0x10e0) {
                throw ex;
            }
            return  null;
        }
    }

    /**
     * 此方法用于获取和构建视图模型唯一对象
     * 将会在获取关联字段的额外属性时被使用
     *
     * @param model
     * @return
     * @throws HongsException
     */
    public static Mview getInstance(Model model) throws HongsException {
        Map    core  = model.db.modelObjects;
        String name  = model.table.name;
        Object minst = core.get( name );
        Mview  mview ;

        if (minst != null && minst instanceof Mview) {
            return ( Mview ) minst ;
        }

        name  = name +":Mview";
        minst = core.get(name);

        if (minst != null && minst instanceof Mview) {
            return ( Mview ) minst ;
        }

        /**
         * 不在开始检查 model 的类型,
         * 是为了总是将 mview 放入到模型库中管理,
         * 可以避免当前 model 关联的模型再关联回来时重复构造.
         */
        if (model instanceof  Mview) {
            mview = ( Mview ) model ;
        } else {
            mview = new Mview(model);
        }
        core.put(name , mview);

        return mview;
    }

}
