package io.github.ihongs.dh;

import io.github.ihongs.HongsException;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.util.Synt;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 表单模型通用配置
 *
 * 对表单中 @ 区域的 xxxxable 配置项:
 * 其取值为 ? 将根据字段的类型来判别,
 * 其取值为 ! 将检查字段内的对应设置,
 * 其取值为 * 表示当前全部字段都可用,
 * 亦可直接指定一个字段列表,
 * 默认不作设置按类型来判别.
 *
 * @author Hongs
 */
public class ModelCase implements IVolume {

    private Map _fields = null;
    private Set _rb_fns = null;
    private Set _ob_fns = null;
    private Set _wd_fns = null;
    private Set _wh_fns = null;

    /**
     * 设置字段配置
     * @param map
     */
    protected void setFields(Map map) {
        _fields = map;
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
        if (null != _fields) {
            return  _fields;
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
     * 获取特定类别的字段类型
     * @param x 类别, 如 string,number
     * @return
     */
    protected Set<String> getSaveTypes(String x) {
        try {
            return ((Map<String, Set>) FormSet
                    .getInstance()
                    .getEnum("__saves__"))
                    .get( x );
        } catch (HongsException e) {
            throw e.toExemption( );
        }
    }

    /**
     * 获取特定用途的字段类型
     * @param x 标识, 如 listable,sortable
     * @return
     */
    protected Set<String> getCaseTypes(String x) {
        try {
            return ((Map<String, Set>) FormSet
                    .getInstance()
                    .getEnum("__cases__"))
                    .get( x );
        } catch (HongsException e) {
            throw e.toExemption( );
        }
    }

    /**
     * 获取特定类别的字段名称
     * @param x 类别, 如 string,number
     * @return
     */
    public Set<String> getSaveNames(String x) {
        Set fts;

        Map<String, Object> pvs = getParams();
        if (pvs.containsKey(x)) {
            Object  o  =  pvs.get (x);
            if ("*".equals (o)) {
                fts = new HashSet(getFields().keySet());
                fts.remove("@");
                return fts;
            } else
            if ("!".equals (o)) {
                fts = null;
            } else
            if ("?".equals (o)) {
                fts = getSaveTypes(x);
            } else
            {
                return Synt.toSet (o);
            }
        } else {
                fts = getSaveTypes(x);
        }

        Map<String, Map> fields = getFields();
        Set<String>      fns  = new HashSet();
        for(Map.Entry<String, Map> et:fields.entrySet()) {
            Map field = et.getValue();
            String fn = et.getKey(  );
            if ("@".equals (fn)) {
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
     * 获取特定用途的字段名称
     * @param x 标识 例如 listable,sortable
     * @return
     */
    public Set<String> getCaseNames(String x) {
        Set fts;

        Map<String, Object> pvs = getParams();
        if (pvs.containsKey(x)) {
            Object  o  =  pvs.get (x);
            if ("*".equals (o)) {
                fts = new HashSet(getFields().keySet());
                fts.remove("@");
                return fts;
            } else
            if ("!".equals (o)) {
                fts = null;
            } else
            if ("?".equals (o)) {
                fts = getCaseTypes(x);
            } else
            {
                return Synt.toSet (o);
            }
        } else {
                fts = getCaseTypes(x);
        }

        Map<String, Map> fields = getFields();
        Set<String>      fns  = new HashSet();
        for(Map.Entry<String, Map> et:fields.entrySet()) {
            Map field = et.getValue();
            String fn = et.getKey(  );
            if ("@".equals (fn)) {
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
        if (null != _rb_fns) {
            return  _rb_fns;
        }
        _rb_fns = getCaseNames("listable");
        return _rb_fns;
    }

    /**
     * 获取可排序的字段
     * @return
     */
    public Set<String> getSortable() {
        if (null != _ob_fns) {
            return  _ob_fns;
        }
        _ob_fns = getCaseNames("sortable");
        return _ob_fns;
    }

    /**
     * 获取可搜索的字段
     * @return
     */
    public Set<String> getSrchable() {
        if (null != _wd_fns) {
            return  _wd_fns;
        }
        _wd_fns = getCaseNames("srchable");
        return _wd_fns;
    }

    /**
     * 获取可过滤的字段
     * @return
     */
    public Set<String> getFindable() {
        if (null != _wh_fns) {
            return  _wh_fns;
        }
        _wh_fns = getCaseNames("findable");
        return _wh_fns;
    }

}
