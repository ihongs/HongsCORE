package io.github.ihongs.dh;

import io.github.ihongs.HongsException;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.util.Synt;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 表单模型通用配置
 *
 * 对表单中 @ 区域的 xxxxable 配置项:
 * 其取值为 @all 表示当前全部字段都可用,
 * 其取值为 @set 将检查字段内的对应设置,
 * 其取值为 @not 则无此字段, 空串也一样;
 * 亦可直接指定一个字段列表,
 * 默认不作设置按类型来判别.
 *
 * @author Hongs
 */
public class JFigure implements IFigure {

    private Map _fields = null;
    private Map _params = null;
    private Set _rb_fns = null;
    private Set _ob_fns = null;
    private Set _wh_fns = null;
    private Set _wd_fns = null;
    private Set _rk_fns = null;

    /**
     * 设置表单字段
     *
     * 会同时设置参数, 但需注意:
     * 重复调用时第一次不带参数,
     * 后面再带上参数并不会覆盖,
     * 重设字段务必自行处理参数.
     *
     * @param map
     */
    protected final void setFields(Map map) {
        _fields = map;

        if (_fields != null
        &&  _params == null) {
            _params  = (Map) map.get ("@");
        if (_params == null) {
            _params  = new LinkedHashMap();
        }}
    }

    /**
     * 设置表单参数
     *
     * 如果自定义字段, 需要注意:
     * 初始化未设字段千万要小心,
     * 此时您可能需要重写此方法,
     * 在获取不到时尝试先取字段.
     *
     * @param map
     */
    protected final void setParams(Map map) {
        _params = map;
    }

    /**
     * 获取表单字段
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
     * @return
     */
    @Override
    public Map getParams() {
        if (null != _params) {
            return  _params;
        }
        throw new NullPointerException("Params can not be null");
    }

    /**
     * 获取特定类别的字段类型
     * @param x 类别, 如 string,number
     * @return
     */
    public Set<String> getSaveTypes(String x) {
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
    public Set<String> getCaseTypes(String x) {
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
        Map<String, Map> fields = getFields();
        Set fts = getSaveTypes(  x  );
        Set fns = new LinkedHashSet();

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
     * 获取特定用途的字段名称
     * @param x 标识 例如 listable,sortable
     * @return
     */
    public Set<String> getCaseNames(String x) {
        Map<String, Map   > fields = getFields();
        Map<String, Object> params = getParams();
        Object ab = params.get(x);
        Set fts ;
        Set fns ;

        // 按类型判断, 类型符合或有设置, 兼容旧的问号标识
        if ( null  ==  ab
        ||  "?".equals(ab)) {
            fts = getCaseTypes(x);
            fns = new LinkedHashSet();
            if (fts!= null) {
            for(Map.Entry<String, Map> et:fields.entrySet()) {
                Map field = et.getValue();
                String fn = et.getKey(  );
                if ("@".equals(fn)) {
                    continue; // 排除掉 @
                }
                if (field.containsKey(x)) {
                if (Synt.declare(field.get( x ), false)) {
                    fns.add(fn);
                }} else {
                if (fts.contains(field.get("__type__"))) {
                    fns.add(fn);
                }}
            }} else {
            for(Map.Entry<String, Map> et:fields.entrySet()) {
                Map field = et.getValue();
                String fn = et.getKey(  );
                if ("@".equals(fn)) {
                    continue; // 排除掉 @
                }
                if (Synt.declare(field.get( x ), false)) {
                    fns.add(fn);
                }
            }}
            return  fns;
        }

        // 有设的字段, 仅接受有设置 xable 且不为 false
        if ("@set".equals(ab)) {
            fns = new LinkedHashSet();
            for(Map.Entry<String, Map> et:fields.entrySet()) {
                Map field = et.getValue();
                String fn = et.getKey(  );
                if ("@".equals(fn)) {
                    continue; // 排除掉 @
                }
                if (Synt.declare(field.get( x ), false)) {
                    fns.add(fn);
                }
            }
            return  fns;
        }

        // 所有的字段, 除非明确声明 xable 且值为 false
        if ("@all".equals(ab)) {
            fns = new LinkedHashSet();
            for(Map.Entry<String, Map> et:fields.entrySet()) {
                Map field = et.getValue();
                String fn = et.getKey(  );
                if ("@".equals(fn)) {
                    continue; // 排除掉 @
                }
                if (Synt.declare(field.get( x ), true )) {
                    fns.add(fn);
                }
            }
            return  fns;
        }

        // 关闭此属性
        if ("@not".equals(ab)) {
            fns = new LinkedHashSet();
            return  fns;
        }

        // 指定的字段
        return Synt.toSet(ab);
    }

    /**
     * 获取可列举的字段
     * @return
     */
    @Override
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
    @Override
    public Set<String> getSortable() {
        if (null != _ob_fns) {
            return  _ob_fns;
        }
        _ob_fns = getCaseNames("sortable");
        return _ob_fns;
    }

    /**
     * 获取可过滤的字段
     * @return
     */
    @Override
    public Set<String> getFindable() {
        if (null != _wh_fns) {
            return  _wh_fns;
        }
        _wh_fns = getCaseNames("findable");
        return _wh_fns;
    }

    /**
     * 获取可搜索的字段 (用于模糊查询)
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
     * 获取可比对的字段 (用于区间查询)
     * @return
     */
    public Set<String> getRankable() {
        if (null != _rk_fns) {
            return  _rk_fns;
        }
        _rk_fns = getCaseNames("rankable");
        return _rk_fns;
    }

}
