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
        setFtypes(ftypes);
        setDtypes(dtypes);
    }

    public ModelForm(Map fields) {
        setFields(fields);
    }

    public ModelForm() {
        // Nothing todo...
    }

    protected final void setFields(Map map) {
        fieldz = map;
    }

    /**
     * 设置字段类型映射
     * 结构:
     *  inputType: fieldType
     * @param map
     */
    protected final void setFtypes(Map map) {
        ftypez = map;
    }

    /**
     * 设置类型标识映射
     * 结构:
     *  xxxxable: inputType 或 fieldType, Set 或 逗号分隔字符串
     * @param map
     */
    protected final void setDtypes(Map map) {
        dtypez = map;
    }

    /**
     * 获取表单参数
     * 默认来自字段配置的 @ 项
     * @return
     */
    public Map getParams() {
        return Synt.asserts(getFields().get("@"), new HashMap());
    }

    /**
     * 获取字段配置
     * 如需覆盖, 可参考以下代码:
     * <code>
     *  try {
     *      return super.getFields();
     *  }
     *  catch (NullPointerException) {}
     *  // TODO: 自行获取 fields
     *  setFields(fields);
     *  return fields;
     * </code>
     * @return
     */
    public Map getFields() {
        if (null != fieldz) {
            return  fieldz;
        }
        throw new NullPointerException("Fields can not be null");
    }

    /**
     * 获取字段类型映射
     * @return
     */
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

    /**
     * 获取查询类型映射
     * @return
     */
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

    /**
     * 获取功能请求参数
     * @return
     */
    public Set<String> getFuncKeys() {
        return fnKeyz;
    }

    /**
     * 获取可列举的字段
     * @return
     */
    public Set<String> getListable() {
        if (null != rbColz) {
            return  rbColz;
        }
        rbColz = getXables("listable");
        return rbColz;
    }

    /**
     * 获取可排序的字段
     * @return
     */
    public Set<String> getSortable() {
        if (null != obColz) {
            return  obColz;
        }
        obColz = getXables("sortable");
        return obColz;
    }

    /**
     * 获取可搜索的字段
     * @return
     */
    public Set<String> getFindable() {
        if (null != wdColz) {
            return  wdColz;
        }
        wdColz = getXables("findable");
        return wdColz;
    }

    /**
     * 获取可过滤的字段
     * @return
     */
    public Set<String> getFiltable() {
        if (null != whColz) {
            return  whColz;
        }
        whColz = getXables("filtable");
        return whColz;
    }

    /**
     * 获取特定许可的字段
     * @param x 标识 例如 listable,sortable
     * @return
     */
    protected Set<String> getXables(String x) {
        Map<String, Map   > fields = getFields();
        Map<String, String> params = getParams();

        // 可在表参数区直接给出
        if (params.containsKey(x)) {
            return Synt.asTerms(params.get(x));
        }

        // 检查是否阻止自动识别
        Set sets ;
        Set abls = new LinkedHashSet();
        if (! Synt.declare(params.get("dont.auto.bind." + x), true)) {
            sets = Synt.asTerms(getDtypes().get(x));
        } else {
            sets = new HashSet();
            // 专用类型特例, 无需特别设置
            if ("findable".equals(x)) {
                sets.add("search");
            } else
            if ("sortable".equals(x)) {
                sets.add("sorted");
            }
        }

        for(Map.Entry<String, Map> et: fields.entrySet()) {
            Map field = et.getValue();
            String fn = et.getKey(  );
            if ("@".equals(fn)) {
                continue; // 排除掉 @
            }
            if (field.containsKey(x)) {
                if (Synt.declare (field.get(x) , false )) {
                    abls.add(fn);
                }
            } else {
                if (sets.contains(field.get("__type__"))) {
                    abls.add(fn);
                }
            }
        }

        return  abls;
    }

}
