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
 用于对存储实例提供表单字段支持
 注意 FormBean 默认 dont.auto.bind.xxxxable 为 true
 即默认不会根据字段类型自动设置 listable,sortable 等
 * @author Hongs
 */
public class FormBean {

    private Map fieldz = null;
    private Map fkindz = null;
    private Map fcasez = null;
    private Set rbColz = null;
    private Set obColz = null;
    private Set wdColz = null;
    private Set whColz = null;

    private static final Set FN_KEYS;
    static {
        FN_KEYS = new HashSet( );
        FN_KEYS.add(Cnst.MD_KEY);
        FN_KEYS.add(Cnst.WD_KEY);
        FN_KEYS.add(Cnst.PN_KEY);
        FN_KEYS.add(Cnst.GN_KEY);
        FN_KEYS.add(Cnst.RN_KEY);
        FN_KEYS.add(Cnst.OB_KEY);
        FN_KEYS.add(Cnst.RB_KEY);
        FN_KEYS.add(Cnst.AB_KEY);
        FN_KEYS.add(Cnst.WR_KEY);
        FN_KEYS.add(Cnst.OR_KEY);
        FN_KEYS.add(Cnst.AR_KEY);
        FN_KEYS.add(Cnst.SR_KEY);
        FN_KEYS.add(Cnst.CB_KEY);
    }

    /**
     * 设置字段配置
     * @param map
     */
    protected final void setFields(Map map) {
        fieldz = map;
    }

    /**
     * 设置存储类型映射
     * @param map
     */
    protected final void setSaveTypes(Map<String, Set<String>> map) {
        fkindz = map;
    }

    /**
     * 设置使用类型映射
     * @param map
     */
    protected final void setCaseTypes(Map<String, Set<String>> map) {
        fcasez = map;
    }

    /**
     * 获取字段配置
     * 如需覆盖, 可参考以下代码:
     * <code>
  try {
      return super.getFields();
  }
  catch (NullPointerException) {}
  // TODO: 自行获取 fields
  setFields(fields);
  return fields;
 </code>
     * @return
     */
    public Map getFields() {
        if (null != fieldz) {
            return  fieldz;
        }
        throw new NullPointerException("Fields can not be null");
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
     * 获取字段类型映射
     * @return
     */
    public Map<String, Set<String>> getSaveTypes() {
        if (null != fkindz) {
            return  fkindz;
        }
        try {
            Map<String, String> m = FormSet.getInstance("default").getEnum("__saves__");
            fkindz = new HashMap();
            for(Map.Entry<String, String> et : m.entrySet()) {
                fkindz.put(et.getKey(), Synt.asTerms(et.getValue()));
            }
            return  fkindz;
        } catch (HongsException e) {
            throw e.toExpedient( );
        }
    }

    /**
     * 获取查询类型映射
     * @return
     */
    public Map<String, Set<String>> getCaseTypes() {
        if (null != fcasez) {
            return  fcasez;
        }
        try {
            Map<String, String> m = FormSet.getInstance("default").getEnum("__cases__");
            fcasez = new HashMap();
            for(Map.Entry<String, String> et : m.entrySet()) {
                fcasez.put(et.getKey(), Synt.asTerms(et.getValue()));
            }
            return  fcasez;
        } catch (HongsException e) {
            throw e.toExpedient( );
        }
    }

    /**
     * 获取特定类别的字段类型
     * @param x 类别, 如 string,number
     * @return 
     */
    public Set<String> getSaveTypes(String x) {
        return FormBean.this.getSaveTypes().get(x);
    }

    /**
     * 获取特定类别的字段名称
     * @param x 类别, 如 string,number
     * @return
     */
    public Set<String> getSaveNames(String x) {
        Map<String, Map> fields = getFields();
        Set<String> fns = new LinkedHashSet();
        Set         fts = getSaveTypes(x);

        for(Map.Entry<String, Map> et: fields.entrySet()) {
            Map field = et.getValue();
            String fn = et.getKey(  );
            if ("@".equals(fn)) {
                continue; // 排除掉 @
            }
            if (fts.contains( field.get( "__type__" ) ) ) {
                fns.add(fn);
            }
        }

        return  fns;
    }

    /**
     * 获取特定用途的字段类型
     * @param x 标识, 如 listable,sortable
     * @return 
     */
    public Set<String> getCaseTypes(String x) {
        return FormBean.this.getCaseTypes().get(x);
    }

    /**
     * 获取特定用途的字段名称
     * @param x 标识 例如 listable,sortable
     * @return
     */
    public Set<String> getCaseNames(String x) {
        // 可在表参数区直接给出
        Map<String, String> fps = getParams();
        if (fps.containsKey(x)) {
            return  Synt.asTerms( fps.get(x));
        }

        Map<String, Map> fields = getFields();
        Set<String> fns = new LinkedHashSet();
        Set         fts ;

        // 检查是否阻止自动识别
        // 专用类型无需特别设置
        if (Synt.declare(fps.get("auto.bind." + x), false)) {
            fts = getCaseTypes(x);
        } else {
            fts = new HashSet ( );
            if ("findable".equals(x)) {
                fts.add("search");
            } else
            if ("sortable".equals(x)) {
                fts.add("sorted");
            }
        }

        for(Map.Entry<String, Map> et: fields.entrySet()) {
            Map field = et.getValue();
            String fn = et.getKey(  );
            if ("@".equals(fn)) {
                continue; // 排除掉 @
            }
            if (field.containsKey(x)) {
                if (Synt.declare( field.get(x), false ) ) {
                    fns.add(fn);
                }
            } else {
                if (fts.contains( field.get("__type__"))) {
                    fns.add(fn);
                }
            }
        }

        return  fns;
    }

    /**
     * 获取关系符号
     * @return
     */
    public Set<String> getFuncKeys() {
        return FN_KEYS;
    }

    /**
     * 获取可列举的字段
     * @return
     */
    public Set<String> getListable() {
        if (null != rbColz) {
            return  rbColz;
        }
        rbColz = getCaseNames("listable");
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
        obColz = getCaseNames("sortable");
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
        wdColz = getCaseNames("findable");
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
        whColz = getCaseNames("filtable");
        return whColz;
    }

}
