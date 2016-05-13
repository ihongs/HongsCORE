package app.hongs.db;

import app.hongs.CoreConfig;
import app.hongs.CoreLocale;
import app.hongs.HongsException;
import app.hongs.action.FormSet;
import app.hongs.action.NaviMap;
import app.hongs.util.Synt;
import java.sql.Types;
import java.util.Iterator;
import java.util.HashMap;
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

    private Map<String, Map<String, String>> fields = null;
    private Map<String, Mview> views = null;
    private Model    model = null;
    private String   title = null;
    private String   nmkey = null;

    public Mview(Table table) throws HongsException {
        super(table);
    }

    public Mview(Model model) throws HongsException {
        this(model.table);
        this.model=model ;
    }

    protected Mview(Model model, Map views) throws HongsException {
        this(model.table);
        this.model=model ;
        this.views=views ;
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

    private Map getMenu() throws HongsException {
        try {
        NaviMap navi = NaviMap.getInstance(db.name);
        return  navi.getMenu(db.name +"/"+ table.name +"/");
        } catch (HongsException ex) {
            if (ex.getErrno() != 0x10e0) {
                throw ex;
            }
            return  null;
        }
    }

    private Map getForm() throws HongsException {
        try {
        FormSet form = FormSet.getInstance(db.name);
        return  form.getFormTranslated(/**/table.name/**/ );
        } catch (HongsException ex) {
            if (ex.getErrno() != 0x10e8
            &&  ex.getErrno() != 0x10ea) {
                throw ex;
            }
            return  null;
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

    public Map<String, String> getParams()
    throws HongsException {
        Map form =  /**/getForm();
        if (form == null) {
            return  new HashMap();
        }
        return Synt.asserts(form.get("@"), new HashMap());
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

        Map<String, String> conf = fields.get("@");
        Set<String> findColz = new LinkedHashSet();
        Set<String> listColz = new LinkedHashSet();
        Set<String> sortColz = new LinkedHashSet();

        Set findable = null;
        if (conf == null || ! Synt.declare(conf.get("dont.auto.bind.findable"), false)) {
            findable = Synt.asTerms(FormSet.getInstance().getEnum("__ables__").get("findable"));
        }
        Set listable = null;
        if (conf == null || ! Synt.declare(conf.get("dont.auto.bind.listable"), false)) {
            listable = Synt.asTerms(FormSet.getInstance().getEnum("__ables__").get("listable"));
        }
        Set sortable = null;
        if (conf == null || ! Synt.declare(conf.get("dont.auto.bind.sortable"), false)) {
            sortable = Synt.asTerms(FormSet.getInstance().getEnum("__ables__").get("sortable"));
        }

        if (null == conf || ! Synt.declare(conf.get("dont.auto.append.fields"), false)) {
            addTableFields();
        }

        if (null == conf || ! Synt.declare(conf.get("dont.auto.append.assocs"), false)) {
            addAssocFields();
        }

        // 检查字段, 为其添加搜索、排序、列举参数
        chkFields(findable, listable, sortable, findColz, listColz, sortColz );

        this.findCols = findColz.toArray(new String[]{});
        this.listCols = listColz.toArray(new String[]{});
        this.sortCols = sortColz.toArray(new String[]{});
        if (model != null) {
            model.findCols = this.findCols;
            model.listCols = this.listCols;
            model.sortCols = this.sortCols;
        }

        return fields;
    }

    private void chkFields(
            Set findable, Set listable, Set sortable,
            Set findColz, Set listColz, Set sortColz
    ) {
        for(Map.Entry<String, Map<String, String>> ent : fields.entrySet()) {
            String name = ent.getKey();
            Map field = ent.getValue();
            String ft = (String) field.get("__type__");

            // 多值字段不能搜索、排序、列举
            if (Synt.declare(field.get("__repeated__"), false)) {
                continue;
            }

            // 特定类型才能搜索、排序、列举
            if (!field.containsKey("findable") && findable != null && findable.contains(ft)) {
                field.put("findable", "yes");
            }
            if (!field.containsKey("listable") && listable != null && listable.contains(ft)) {
                field.put("listable", "yes");
            }
            if (!field.containsKey("sortable") && sortable != null && sortable.contains(ft)) {
                field.put("sortable", "yes");
            }

            if (Synt.declare(field.get(  "findable"  ), false)) {
                findColz.add(name);
            }
            if (Synt.declare(field.get(  "listable"  ), false)) {
                listColz.add(name);
            }
            if (Synt.declare(field.get(  "sortable"  ), false)) {
                sortColz.add(name);
            }
        }
    }

    private void addTableFields() throws HongsException {
        CoreLocale lang = getLang();

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
        }
    }

    private void addAssocFields() throws HongsException {
        if (  null == table.assocs  ) {
            return;
        }

        Map<String, Mview> viewz = new LinkedHashMap();
        if (views != null) viewz.putAll ( views );
        viewz.put(table.db.name+":"+table.name , this);

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

                Model  hm = db.getModel(tn);
                tk   = hm.db.name+":"+hm.table.name ;
                Mview  hb = viewz.containsKey(tk)
                          ? viewz.get(tk)
                          : new Mview(hm , viewz);

                tk   = hb.getNmKey();
                disp = hb.getTitle();
                name = vk;
            } else
            if ("HAS_ONE".equals(type)) {
                ak   = (String) vd.get("name");
                tn   = (String) vd.get("tableName" );
                vk   = (String) vd.get("foreignKey");
                tn   = tn != null ? tn : ak;

                Model  hm = db.getModel(tn);
                tk   = hm.db.name+":"+hm.table.name ;
                Mview  hb = viewz.containsKey(tk)
                          ? viewz.get(tk)
                          : new Mview(hm , viewz);

                tk   = hb.getNmKey();
                disp = hb.getTitle();
                name = ak + "." + vk;
            } else
            if ("HAS_MANY".equals(type)) {
                ak   = (String) vd.get("name");
                tn   = (String) vd.get("tableName" );
                vk   = (String) vd.get("foreignKey");
                tn   = tn != null ? tn : ak;

                Model  hm = db.getModel(tn);
                tk   = hm.db.name+":"+hm.table.name ;
                Mview  hb = viewz.containsKey(tk)
                          ? viewz.get(tk)
                          : new Mview(hm , viewz);

                tk   = hb.getNmKey();
                disp = hb.getTitle();
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
                tk   = hm.db.name+":"+hm.table.name ;
                Mview  hb = viewz.containsKey(tk)
                          ? viewz.get(tk)
                          : new Mview(hm , viewz);

                tk   = hb.getNmKey();
                disp = hb.getTitle();
                name = ak + ".."+ vk;
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
        }
    }

}
