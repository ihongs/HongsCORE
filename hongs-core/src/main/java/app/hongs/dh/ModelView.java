package app.hongs.dh;

import app.hongs.Cnst;
import app.hongs.HongsException;
import app.hongs.action.FormSet;
import app.hongs.util.Synt;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 模型视图
 * 用于对存储实例提供表单字段支持
 * @author Hongs
 */
abstract public class ModelView {

    private Map   fieldz = null;
    private Map   ftypez = null;
    private Map   dtypez = null;

    private Set findColz = null;
    private Set listColz = null;
    private Set sortColz = null;

    private static final Set funcKeyz;
    static {
        funcKeyz = new HashSet( );
        funcKeyz.add(Cnst.PN_KEY);
        funcKeyz.add(Cnst.GN_KEY);
        funcKeyz.add(Cnst.RN_KEY);
        funcKeyz.add(Cnst.OB_KEY);
        funcKeyz.add(Cnst.RB_KEY);
        funcKeyz.add(Cnst.UD_KEY);
        funcKeyz.add(Cnst.MD_KEY);
        funcKeyz.add(Cnst.WD_KEY);
        funcKeyz.add(Cnst.WH_KEY);
        funcKeyz.add(Cnst.OR_KEY);
        funcKeyz.add(Cnst.AR_KEY);
        funcKeyz.add(Cnst.SR_KEY);
    }

    protected void setFields(Map map) {
        fieldz = map;
    }

    protected void setFtypes(Map map) {
        ftypez = map;
    }

    protected void setDtypes(Map map) {
        dtypez = map;
    }

    public Map getFields() {
        if (null != fieldz) {
            return  fieldz;
        }
        throw new NullPointerException("Fields can not be null");
    }

    public Map getFtypes() {
        if (null != ftypez) {
            return  ftypez;
        }
        try {
            ftypez = FormSet.getInstance("default").getEnum("__types__");
            return  ftypez;
        } catch (HongsException e) {
            throw e.toUnchecked( );
        }
    }

    public Map getDtypes() {
        if (null != dtypez) {
            return  dtypez;
        }
        try {
            dtypez = FormSet.getInstance("default").getEnum("__ables__");
            return  dtypez;
        } catch (HongsException e) {
            throw e.toUnchecked( );
        }
    }

    public Set<String> getFuncs() {
        return  funcKeyz;
    }

    public Set<String> getFinds() {
        if (null != findColz) {
            return  findColz;
        }
        findColz = getAbles("findable");
        return  findColz;
    }

    public Set<String> getLists() {
        if (null != listColz) {
            return  listColz;
        }
        listColz = getAbles("listable");
        return  listColz;
    }

    public Set<String> getSorts() {
        if (null != sortColz) {
            return  sortColz;
        }
        sortColz = getAbles("sortable");
        return  sortColz;
    }

    protected boolean listable(Map  fc) {
        return xxxxable("listable", fc);
    }

    protected boolean sortable(Map  fc) {
        if ("sorted".equals(fc.get("__type__"))) {
            return true;
        }
        return xxxxable("sortable", fc);
    }

    protected boolean findable(Map  fc) {
        if ("search".equals(fc.get("__type__"))) {
            return true;
        }
        return xxxxable("findable", fc);
    }

    protected final boolean xxxxable(String dn, Map fc) {
        if (fc.containsKey(dn)) {
            return Synt.asserts(fc.get(dn), false);
        }
        String t = Synt.asserts(fc.get("__type__"), "" );
        Set    s = getAbles(dn);
        return s == null || s.isEmpty() || s.contains(t);
    }

    protected final Set<String> getAbles(String dn) {
        Set ables = new LinkedHashSet();

        Map<String, Map   > fields = getFields();
        Map<String, String> fc = fields.get("@");
        Set sets;
        if ( fc == null || ! Synt.declare(fc.get("dont.auto.bind."+dn), false)) {
            sets = Synt.asTerms(getDtypes( ).get( dn ));
        } else {
            sets = new HashSet ();
        }

        for(Map.Entry<String, Map> et: fields.entrySet()) {
            Map field = et.getValue();
            String fn = et.getKey(  );
            if (field.containsKey(dn)) {
                if (Synt.declare (field.get(dn) , false)) {
                    ables.add    (fn);
                }
            } else {
                if (sets.contains(field.get("__type__"))) {
                    ables.add    (fn);
                }
            }
        }

        return  ables;
    }

}
