package app.hongs.dh;

import app.hongs.Cnst;
import app.hongs.HongsException;
import app.hongs.action.FormSet;
import app.hongs.util.Synt;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 模型表单
 * 用于对存储实例提供表单字段支持
 * @author Hongs
 */
abstract public class ModelForm {

    private Map   fieldz = null;
    private Map   ftypez = null;
    private Map   dtypez = null;

    private Set listColz = null;
    private Set findColz = null;
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

    /**
     * 设置字段类型映射
     * 结构:
     *  inputType: fieldType
     * @param map
     */
    protected void setFtypes(Map map) {
        ftypez = map;
    }

    /**
     * 设置类型标识映射
     * 结构:
     *  xxxxable: inputType 或 fieldType, Set 或 逗号分隔字符串
     * @param map
     */
    protected void setDtypes(Map map) {
        dtypez = map;
    }

    public Map getFields() {
        if (null != fieldz) {
            return  fieldz;
        }
        throw new NullPointerException("Fields can not be null");
    }

    public Map getParams() {
        return Synt.asserts(getFields().get("@"), new HashMap());
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

    public Set<String> getLists() {
        if (null != listColz) {
            return  listColz;
        }
        listColz = getAbles("listable");
        return  listColz;
    }

    public Set<String> getFinds() {
        if (null != findColz) {
            return  findColz;
        }
        findColz = getAbles("findable");
        return  findColz;
    }

    public Set<String> getSorts() {
        if (null != sortColz) {
            return  sortColz;
        }
        sortColz = getAbles("sortable");
        return  sortColz;
    }

    protected Set<String> getAbles(String dn) {
        Map<String, Map   > fields = getFields();
        Map<String, String> params = getParams();

        // 可在表参数区直接给出
        if (params.containsKey ( dn )) {
            return Synt.asTerms(params.get(dn) );
        }

        // 检查是否阻止自动识别
        Set sets ;
        Set abls = new LinkedHashSet();
        if (! Synt.declare(params.get("dont.auto.bind." + dn), false)) {
            sets = Synt.asTerms( getDtypes( ).get( dn ) );
        } else {
            sets = new HashSet ( );
        }

        for(Map.Entry<String, Map> et: fields.entrySet()) {
            Map field = et.getValue();
            String fn = et.getKey(  );
            if ("@".equals(fn)) {
                continue; // 排除掉 @
            }
            if (field.containsKey(dn)) {
                if (Synt.declare (field.get(dn) , false)) {
                    abls.add     (fn);
                }
            } else {
                if (sets.contains(field.get("__type__"))) {
                    abls.add     (fn);
                }
            }
        }

        return  abls;
    }

}
