package io.github.ihongs.dh;

import io.github.ihongs.HongsException;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.util.Synt;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 表单模型通用配置
 *
 * 对表单中 @ 区域的 xxxxable 配置项,
 * 其取值为 ? 将根据字段的类型来判别,
 * 如取值为 * 表示当前全部字段都可用.
 * 默认情况需检查字段自身的 xxxxable.
 *
 * @author Hongs
 */
public class ModelCase implements IVolume {

    private Map fieldz = null;
    private Map fsavez = null;
    private Map fcasez = null;
    private Set rbColz = null;
    private Set obColz = null;
    private Set wdColz = null;
    private Set whColz = null;

    /**
     * 设置字段配置
     * @param map
     */
    protected void setFields(Map map) {
        fieldz = map;
    }

    /**
     * 设置存储类型映射
     * @param map
     */
    protected void setSaveTypes(Map<String, Set<String>> map) {
        fsavez = map;
    }

    /**
     * 设置使用类型映射
     * @param map
     */
    protected void setCaseTypes(Map<String, Set<String>> map) {
        fcasez = map;
    }

    /**
     * 获取字段配置
     * 如需覆盖, 可参考以下代码:
     * <code>
    try {
        return super.getFields();
    }
    catch (NullPointerException ex) {
        // 自定义 fields
        setFields(fields);
        return fields;
    }
     * </code>
     * @return
     */
    @Override
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
    @Override
    public Map getParams() {
        Map ps =  Synt.asMap(getFields().get("@"));
        if (ps == null) {
            ps =  Synt.mapOf();
        }
        return ps;
    }

    /**
     * 获取字段类型映射
     * @return
     */
    public Map<String, Set<String>> getSaveTypes() {
        if (null != fsavez) {
            return  fsavez;
        }
        try {
            Map<String, Object> m = FormSet.getInstance().getEnum("__saves__");
            fsavez = new HashMap();
            for(Map.Entry<String, Object> et : m.entrySet()) {
                fsavez.put(et.getKey(), Synt.toSet(et.getValue()));
            }
            return  fsavez;
        } catch (HongsException e) {
            throw e.toExemption( );
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
            Map<String, Object> m = FormSet.getInstance().getEnum("__cases__");
            fcasez = new HashMap();
            for(Map.Entry<String, Object> et : m.entrySet()) {
                fcasez.put(et.getKey(), Synt.toSet(et.getValue()));
            }
            return  fcasez;
        } catch (HongsException e) {
            throw e.toExemption( );
        }
    }

    /**
     * 获取特定类别的字段类型
     * @param x 类别, 如 string,number
     * @return
     */
    public Set<String> getSaveTypes(String x) {
        return getSaveTypes().get(x);
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
        return getCaseTypes().get(x);
    }

    /**
     * 获取特定用途的字段名称
     * @param x 标识 例如 listable,sortable
     * @return
     */
    public Set<String> getCaseNames(String x) {
        Set fts = null;

        Map<String, Object> pvs = getParams();
        if (pvs.containsKey(x)) {
            // 检查是否根据类型识别
            // 亦或提取全部字段名称
            Object  o  =  pvs.get(x);
            if (!"?".equals(o)) {
            if (!"*".equals(o)) {
                return Synt.toSet(o);
            } else {
                Set fs = getFields().keySet();
                fs = new LinkedHashSet ( fs );
                fs.remove("@");
                return fs;
            }}

            fts = getCaseTypes(x);
        } else {
            // 可在表参数区直接给出
            // 专用类型无需特别设置
            if ("srchable".equals(x)) {
                fts = new HashSet( );
                fts.add("search");
            } else
            if ("sortable".equals(x)) {
                fts = new HashSet( );
                fts.add("sorted");
            }
        }

        Map<String, Map   > fcs = getFields();
        Set<String> fns = new LinkedHashSet();
        for(Map.Entry<String, Map> et: fcs.entrySet( ) ) {
            Map field = et.getValue();
            String fn = et.getKey(  );
            if ("@".equals(fn)) {
                continue; // 排除掉 @
            }
            if (field.containsKey(x)) {
                if (Synt.declare(field.get(x), false ) ) {
                    fns.add(fn);
                }
            } else if ( fts != null ) {
                if (fts.contains(field.get("__type__"))) {
                    fns.add(fn);
                }
            }
        }

        return  fns;
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
    public Set<String> getSrchable() {
        if (null != wdColz) {
            return  wdColz;
        }
        wdColz = getCaseNames("srchable");
        return wdColz;
    }

    /**
     * 获取可过滤的字段
     * @return
     */
    public Set<String> getFindable() {
        if (null != whColz) {
            return  whColz;
        }
        whColz = getCaseNames("findable");
        return whColz;
    }

}
