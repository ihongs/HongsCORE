package app.hongs.db;

import app.hongs.CoreConfig;
import app.hongs.CoreLocale;
import app.hongs.HongsException;
import app.hongs.action.FormSet;
import app.hongs.action.NaviMap;
import app.hongs.util.Synt;
import java.sql.Types;
import java.util.Arrays;
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

    private Model  model = null;
    private String nmkey = null;
    private String title = null;
    private Map<String, Map<String, String>> fields = null;

    public Mview(Table table) throws HongsException {
        super(table);
    }

    public Mview(Model model) throws HongsException {
        this(model.table);
        this.model=model ;
    }

    public final CoreConfig getConf() {
        CoreConfig conf = CoreConfig.getInstance( ).clone();
        conf.loadIgnrFNF(db.name);
        return  conf;
    }

    public final CoreLocale getLang() {
        CoreLocale lang = CoreLocale.getInstance( ).clone();
        lang.loadIgnrFNF(db.name);
        return  lang;
    }

    public final Map getMenu() throws HongsException {
        NaviMap navi = NaviMap.getInstance(db.name);
        return  navi.getMenu(db.name +"/"+ table.name +"/");
    }

    public final Map getForm() throws HongsException {
        FormSet form = FormSet.getInstance(db.name);
        try {
            return form.getFormTranslated( table.name /**/);
        } catch (HongsException ex) {
        if (ex.getCode() == 0x10ea) {
            return null;
        } else {
            throw  ex  ;
        }
        }
    }

    public String getIdKey()
    throws HongsException {
        return table.primaryKey;
    }

    public String getNmKey()
    throws HongsException {
        if (null != nmkey) {
            return  nmkey;
        }

        getFields( );

        if (this.listCols.length > 0) {
            nmkey = this.listCols [0];
            return nmkey;
        }
        if (this.findCols.length > 0) {
            nmkey = this.findCols [0];
            return nmkey;
        }

        nmkey  =  "";
        return nmkey;
    }

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
            if (item != null  && item.containsKey("__disp__")) {
                title = (String) item.get("__disp__");
                break;
            }
            }

            // 再从菜单取名字
            item = getMenu( );
            if (item != null  && item.containsKey(  "disp"  )) {
                title = (String) item.get(  "disp"  );
                break;
            }

            // 最后配置取名字
            title = "core.entity."+table.name+".name";
        } while (false);

        title = getLang().translate(title);
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

    public Map<String, Map<String, String>> getFields()
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

        CoreLocale lang = getLang();
        Set<String> findCols = new LinkedHashSet();
        Set<String> listCols = new LinkedHashSet();

        Map<String, String> conf = fields.get("@");
        Set findable = null;
        if (conf == null || !Synt.declare(conf.get("dont.auto.bind.findable"), false)) {
            findable = new HashSet(Arrays.asList(Synt.declare(
                CoreConfig.getInstance()
               .get("core.findable.types"),
                    "string,search,text,email,url,tel,textarea"
            ).split(",")));
        }
        Set sortable = null;
        if (conf == null || !Synt.declare(conf.get("dont.auto.bind.sortable"), false)) {
            sortable = new HashSet(Arrays.asList(Synt.declare(
                CoreConfig.getInstance()
               .get("core.sortable.types"),
                    "string,search,text,email,url,tel,number,range,onoff,date,time,datetime,enum,select,radio,check"
            ).split(",")));
        }
        Set listable = null;
        if (conf == null || !Synt.declare(conf.get("dont.auto.bind.listable"), false)) {
            listable = new HashSet(Arrays.asList(Synt.declare(
                CoreConfig.getInstance()
               .get("core.listable.types"),
                    "string,search,text,email,url,tel,number,range,onoff,date,time,datetime,enum,select,radio,check,fork,form"
            ).split(",")));
        }

        for(Map.Entry<String, Map<String, String>> ent : fields.entrySet()) {
            String name = ent.getKey();
            Map field = ent.getValue();

            // 多值字段默认不能列举、排序和搜索
            if (Synt.declare(field.get("__repeated__"), false)) {
                continue;
            }

            // 特定类型才能搜索、列举、排序
            String ft = (String) field.get("__type__");
            if (!field.containsKey("listable") && listable != null && listable.contains(ft)) {
                field.put("listable", "yes");
            }
            if (!field.containsKey("findable") && findable != null && findable.contains(ft)) {
                field.put("findable", "yes");
            }
            if (!field.containsKey("sortable") && sortable != null && sortable.contains(ft)) {
                field.put("sortable", "yes");
            }

            // 提取搜索和列举字段
            if (Synt.declare(field.get(  "listable"  ), false)) {
                listCols.add(name);
            }
            if (Synt.declare(field.get(  "findable"  ), false)) {
                findCols.add(name);
            }
        }

        if (null == conf || !Synt.declare(conf.get("dont.auto.append.fields"), false)) {
        /*
        String sql = "SHOW FULL FIELDS FROM `"+table.tableName+"`";
        List<Map<String, Object>> rows = db.fetchAll(sql);
        for (Map<String, Object>  row  : rows) {
            String disp = (String)row.get("Comment");
            String name = (String)row.get( "Field" );
            String type = (String)row.get( "Type"  );
        */
        Map<String, Map> cols = table.getFields();
        for(Map.Entry<String, Map> ent : cols.entrySet()) {
            Map     col  = ent.getValue();
            String  name = ent.getKey(  );
            Integer type = (Integer) col.get("type");
            Boolean rqrd = (Boolean) col.get("required");
            String  disp = "field."+ table.name +"."+ name +".name";

            Map field = (Map) fields.get(name);
            if (field == null) {
                field =  new HashMap( );
                fields.put(name, field);
            }

            if (!field.containsKey("__required__") || "".equals(field.get("__required__"))) {
//              field.put("__required__", "NO".equals(row.get("Null")) ? "yes" : "");
                field.put("__required__", rqrd ? "yes" : "");
            }

            if (!field.containsKey("__disp__") || "".equals(field.get("__disp__"))) {
//              if (disp!=null && !"".equals(disp)) {
                if (lang.containsKey(disp)) {
                    disp = lang.translate(disp);
                    field.put("__disp__", disp);
                } else {
                    field.put("__disp__", name);
                }
            }

            if (!field.containsKey("__type__") || "".equals(field.get("__type__"))) {
                if (name.equals(table.primaryKey) || name.endsWith("_id")) {
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

            // 特定类型才能搜索、列举、排序
            String ft = (String) field.get("__type__");
            if (!field.containsKey("listable") && listable != null && listable.contains(ft)) {
                field.put("listable", "yes");
            }
            if (!field.containsKey("sortable") && sortable != null && sortable.contains(ft)) {
                field.put("sortable", "yes");
            }
            if (!field.containsKey("findable") && findable != null && findable.contains(ft)) {
                field.put("findable", "yes");
            }

            // 提取搜索和列举字段
            if (Synt.declare(field.get("listable"), false)) {
                listCols.add(name);
            }
            if (Synt.declare(field.get("findable"), false)) {
                findCols.add(name);
            }
        }
        }

        if (null == conf || !Synt.declare(conf.get("dont.auto.append.assocs"), false)) {
        if (null != table.assocs) {
        Iterator it = table.assocs.entrySet().iterator();
        while (  it.hasNext( )  ) {
            Map.Entry et = (Map.Entry)it.next();
            Map       vd = (Map ) et.getValue();
            String  type = (String) vd.get("type");

            String  name, disp, vk, tk, ak, ek, tn;

            if ("BLS_TO".equals(type)) {
                ak   = (String) vd.get("name");
                tn   = (String) vd.get("tableName" );
                vk   = (String) vd.get("foreignKey");
                tn   = tn != null ? tn : ak;
                name = vk;

                Model hm = db.getModel(tn);
                Mview hb = hm.table.name.equals(table.name)
                        && hm.db.name.equals(db.name)
                    ? this : new Mview(hm);
                tk   = hb.getNmKey();
                disp = hb.getTitle();
            } else
            if ("HAS_ONE".equals(type)) {
                ak   = (String) vd.get("name");
                tn   = (String) vd.get("tableName" );
                vk   = (String) vd.get("foreignKey");
                tn   = tn != null ? tn : ak;
                name = ak + "." + vk;

                Model hm = db.getModel(tn);
                Mview hb = hm.table.name.equals(table.name)
                        && hm.db.name.equals(db.name)
                    ? this : new Mview(hm);
                tk   = hb.getNmKey();
                disp = hb.getTitle();
            } else
            if ("HAS_MANY".equals(type)) {
                ak   = (String) vd.get("name");
                tn   = (String) vd.get("tableName" );
                vk   = (String) vd.get("foreignKey");
                tn   = tn != null ? tn : ak;
                name = ak + ".." + vk;

                Model hm = db.getModel(tn);
                Mview hb = hm.table.name.equals(table.name)
                        && hm.db.name.equals(db.name)
                    ? this : new Mview(hm);
                tk   = hb.getNmKey();
                disp = hb.getTitle();
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
                name = ak + ".." + vk;

                Model hm = db.getModel(tn);
                Mview hb = hm.table.name.equals(table.name)
                        && hm.db.name.equals(db.name)
                    ? this : new Mview(hm);
                tk   = hb.getNmKey();
                disp = hb.getTitle();
            } else {
                continue;
            }

            Map field = (Map) fields.get(name);
            if (field == null) {
                field =  new HashMap( );
                fields.put(name, field);

                field = new HashMap();
                fields.put(name, field);
                field.put("__type__","pick");
                field.put("__disp__", disp );
                field.put("data-ak", ak);
                field.put("data-vk", vk);
                field.put("data-tk", tk);
            }

            // 特定类型才能搜索、列举、排序
            String ft = (String) field.get("__type__");
            if (!field.containsKey("listable") && listable != null && listable.contains(ft)) {
                field.put("listable", "yes");
            }
            if (!field.containsKey("sortable") && sortable != null && sortable.contains(ft)) {
                field.put("sortable", "yes");
            }
            if (!field.containsKey("findable") && findable != null && findable.contains(ft)) {
                field.put("findable", "yes");
            }

            // 提取搜索和列举字段
            if (Synt.declare(field.get("listable"), false)) {
                listCols.add(name);
            }
            if (Synt.declare(field.get("findable"), false)) {
                findCols.add(name);
            }
        }
        }
        }

        // 设置搜索和列举字段
        this.findCols = findCols.toArray(new String[0]);
        this.listCols = listCols.toArray(new String[0]);
        if (model != null) {
            model.findCols = this.findCols;
            model.listCols = this.listCols;
        }

        return fields;
    }

}
