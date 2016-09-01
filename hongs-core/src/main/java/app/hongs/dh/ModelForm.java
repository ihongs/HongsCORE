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
 * 注意 ModelForm 默认 dont.auto.bind.xxxxable 为 true
 * 即默认不会根据字段类型自动设置 listable,sortable 等
 * @author Hongs
 */
public class ModelForm {

    private Map fieldz = null;
    private Map ftypez = null;
    private Map dtypez = null;
    private Set rbColz = null;
    private Set obColz = null;
    private Set wdColz = null;
    private Set whColz = null;

    private static final Set fnKeyz;
    static {
      fnKeyz = new HashSet( );
      fnKeyz.add(Cnst.PN_KEY);
      fnKeyz.add(Cnst.GN_KEY);
      fnKeyz.add(Cnst.RN_KEY);
      fnKeyz.add(Cnst.OB_KEY);
      fnKeyz.add(Cnst.RB_KEY);
      fnKeyz.add(Cnst.UD_KEY);
      fnKeyz.add(Cnst.MD_KEY);
      fnKeyz.add(Cnst.WD_KEY);
      fnKeyz.add(Cnst.WH_KEY);
      fnKeyz.add(Cnst.OR_KEY);
      fnKeyz.add(Cnst.AR_KEY);
      fnKeyz.add(Cnst.SR_KEY);
    }

    public ModelForm(Map fields, Map ftypes, Map dtypes) {
        setFields(fields);
        setFtypes(fields);
        setDtypes(fields);
    }

    public ModelForm(Map fields) {
        setFields(fields);
    }

    public ModelForm() {
        // Nothing todo...
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

    public Map getParams() {
        return Synt.asserts(getFields().get("@"), new HashMap());
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
        return fnKeyz;
    }

    public Set<String> getLists() {
        if (null != rbColz) {
            return  rbColz;
        }
        rbColz = getAbles("listable");
        return rbColz;
    }

    public Set<String> getSorts() {
        if (null != obColz) {
            return  obColz;
        }
        obColz = getAbles("sortable");
        return obColz;
    }

    public Set<String> getFinds() {
        if (null != wdColz) {
            return  wdColz;
        }
        wdColz = getAbles("findable");
        return wdColz;
    }

    public Set<String> getFilts() {
        if (null != whColz) {
            return  whColz;
        }
        whColz = getAbles("filtable");
        return whColz;
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
        if (! Synt.declare(params.get("dont.auto.bind." + dn), true)) {
            sets = Synt.asTerms( getDtypes( ).get( dn ) );
        } else {
            sets = new HashSet ( );
            // 专用类型特例, 无需特别设置
            if ("findable".equals(dn)) {
                sets.add("search");
            } else
            if ("sortable".equals(dn)) {
                sets.add("sorted");
            }
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
