package app.hongs.db;

import app.hongs.Cnst;
import app.hongs.CoreLocale;
import app.hongs.HongsException;
import app.hongs.action.FormSet;
import app.hongs.action.NaviMap;
import app.hongs.util.Synt;
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

    private CoreLocale locale = null;
    private Map        fields = null;
    private Model      model  = null;
    private String     txkey  = null;
    private String     title  = null;

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
     * 注意:
     * 如果 model 中覆盖了 add,put,del和filter 等
     * 调用 Mview 中相同方法并不会使用此 model 的
     * 但会在调用 getFields 后设置 model.xxxxable
     *
     * @param model
     * @throws HongsException
     */
    public Mview(Model model) throws HongsException {
        this(model.table);
        this.model=model ;
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

        Map<String,  Map  > flds = getFields();
        Map<String, String> typs = FormSet.getInstance().getEnum("__types__");

        /**
         * 寻找第一个非隐藏的字符串字段
         */
        for(String name : listable) {
            Map    item = flds.get(name);
            if ( item == null ) continue;
            String type = (String) item.get ("__type__");
            String kind = typs.get(type);
            if ("string".equals(kind)
            && !"stored".equals(type)
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
            title = getLocale( ).translate(title);
        } while (false);

        return   title ;

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
            fields  =  new LinkedHashMap( );
        } else {
            fields  =  new LinkedHashMap( fields );
        }

        Map<String , Object> prms = (Map) fields.get("@");
        Set<String> listColz = new LinkedHashSet();
        Set<String> sortColz = new LinkedHashSet();
        Set<String> findColz = new LinkedHashSet();
        Set<String> siftColz = new LinkedHashSet();
        Set<String> listTypz = new HashSet();
        Set<String> sortTypz = new HashSet();
        Set<String> findTypz = new HashSet();
        Set<String> siftTypz = new HashSet();

        if (prms == null || ! Synt.declare(prms.get("auto.bind.listable"), true)) {
            listTypz = Synt.toSet(FormSet.getInstance().getEnum("__cases__").get("listable"));
        }
        if (prms == null || ! Synt.declare(prms.get("auto.bind.sortable"), true)) {
            sortTypz = Synt.toSet(FormSet.getInstance().getEnum("__cases__").get("sortable"));
        }
        if (prms == null || ! Synt.declare(prms.get("auto.bind.findable"), true)) {
            findTypz = Synt.toSet(FormSet.getInstance().getEnum("__cases__").get("findable"));
        }
        if (prms == null || ! Synt.declare(prms.get("auto.bind.siftable"), true)) {
            siftTypz = Synt.toSet(FormSet.getInstance().getEnum("__cases__").get("siftable"));
        }

        // 排序、搜索等字段也可以直接在主字段给出
        if (prms != null && prms.containsKey("listable")) {
            listColz = Synt.toSet ( prms.get("listable"));
            listTypz.clear();
        }
        if (prms != null && prms.containsKey("sortable")) {
            sortColz = Synt.toSet ( prms.get("sortable"));
            sortTypz.clear();
        }
        if (prms != null && prms.containsKey("findable")) {
            findColz = Synt.toSet ( prms.get("findable"));
            findTypz.clear();
        }
        if (prms != null && prms.containsKey("siftable")) {
            siftColz = Synt.toSet ( prms.get("siftable"));
            siftTypz.clear();
        }

        if (null == prms || ! Synt.declare(prms.get("auto.append.fields"), true)) {
            addTableFields();
        }
        if (null == prms || ! Synt.declare(prms.get("auto.append.assocs"), true)) {
            addAssocFields();
        }

        // 检查字段, 为其添加搜索、排序、列举参数
        chkFields(listTypz, sortTypz, findTypz, siftTypz, listColz, sortColz, findColz, siftColz);

        if (!listColz.isEmpty()) {
            listable = listColz.toArray(new String[]{});
        }
        if (!sortColz.isEmpty()) {
            sortable = sortColz.toArray(new String[]{});
        }
        if (!findColz.isEmpty()) {
            findable = findColz.toArray(new String[]{});
        }
        if (!siftColz.isEmpty()) {
            siftable = siftColz.toArray(new String[]{});
        }
        if (model != null) {
            model.listable = listable;
            model.sortable = sortable;
            model.findable = findable;
            model.siftable = siftable;
        }

        return fields;
    }

    private void chkFields(
            Set listTypz, Set sortTypz, Set findTypz, Set siftTypz,
            Set listColz, Set sortColz, Set findColz, Set siftColz) {
        for(Map.Entry<String, Map> ent
            : ( ( Map<String, Map> ) fields ).entrySet( )) {
            Map field = ent.getValue();
            String fn = ent.getKey(  );
            String ft = Synt.declare(field.get("__type__"), "text");

            // 表单信息字段需要排除
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
//                  field.put("listable", "yes");
//              } else
//              if (listColz.contains(fn)) {
//                  field.put("listable", "yes");
                }
            }

            if (field.containsKey("sortable")) {
                if (Synt.declare(field.get("sortable"), false)) {
                    sortColz.add(fn);
                }
            } else {
                if (sortTypz.contains(ft)) {
                    sortColz.add(fn);
//                  field.put("sortable", "yes");
//              } else
//              if (sortColz.contains(fn)) {
//                  field.put("sortable", "yes");
                }
            }

            if (field.containsKey("findable")) {
                if (Synt.declare(field.get("findable"), false)) {
                    findColz.add(fn);
                }
            } else {
                if (findTypz.contains(ft)) {
                    findColz.add(fn);
//                  field.put("findable", "yes");
//              } else
//              if (findColz.contains(fn)) {
//                  field.put("findable", "yes");
                }
            }

            if (field.containsKey("siftable")) {
                if (Synt.declare(field.get("siftable"), false)) {
                    siftColz.add(fn);
                }
            } else {
                if (siftTypz.contains(ft)) {
                    siftColz.add(fn);
//                  field.put("siftable", "yes");
//              } else
//              if (siftColz.contains(fn)) {
//                  field.put("siftable", "yes");
                }
            }
        }
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
        if (model instanceof Mview) {
            mview  = ( Mview ) model ;
            core.put (name , mview);
        } else {
            mview  = new Mview(model);
            core.put (name , mview);
        }

        return mview;
    }

}
