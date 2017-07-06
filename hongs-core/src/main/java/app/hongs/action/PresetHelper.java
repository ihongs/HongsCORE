package app.hongs.action;

import app.hongs.HongsException;
import app.hongs.util.Data;
import app.hongs.util.Dict;
import app.hongs.util.Synt;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Hongs
 */
public class PresetHelper {

    private final Map<String, Object> data;
    private final Pattern INJ_PAT = Pattern.compile("\\(\\$(request|context|session|cookies)\\.([a-zA-Z0-9_]+)(?:\\.([a-zA-Z0-9_\\.]+))?\\)");

    public PresetHelper() {
        data = new LinkedHashMap();
    }

    public PresetHelper addItem(String code, Object info) {
        data.put(code, info);
        return this;
    }

    public PresetHelper addItemsByEnum(Map<String, Object> envm) {
        data.putAll(envm);
        return this;
    }

    public PresetHelper addItemsByEnum(String conf, String name) throws HongsException {
        Map envm = FormSet.getInstance(conf).getEnum(name);
        return addItemsByEnum(envm);
    }

    public PresetHelper addItemsByForm(String conf, String name) throws HongsException {
        name += ":preset";
        Map envm = FormSet.getInstance(conf).getEnum(name);
        return addItemsByEnum(envm);
    }

    /**
     * 预置数据
     * 注意: 内部会 insert:default 和 inject:defense
     * @param helper
     * @param reqd
     * @param used
     * @throws HongsException
     */
    public void preset(ActionHelper helper, Map reqd, String... used) throws HongsException {
        insert(helper, reqd, used);
        insert(helper, reqd, ":default");
        inject(helper, reqd, ":defense");
    }

    /**
     * 强制写入, 不管目标是否存在
     * @param helper
     * @param reqd
     * @param used
     * @throws HongsException
     */
    public void inject(ActionHelper helper, Map reqd, String... used) throws HongsException {
        for(String k : used) {
            Object v = data.get(k);
            if (v != null && !"".equals(v)) {
                Map rxqd = decode(helper, v);
                inject(reqd, rxqd);
            }
        }
    }

    /**
     * 补充插入, 目标存在则不跳过
     * @param helper
     * @param reqd
     * @param used
     * @throws HongsException
     */
    public void insert(ActionHelper helper, Map reqd, String... used) throws HongsException {
        for(String k : used) {
            Object v = data.get(k);
            if (v != null && !"".equals(v)) {
                Map rxqd = decode(helper, v);
                insert(reqd, rxqd);
            }
        }
    }

    private void inject(Map r, Map v) throws HongsException {
        Map<String, Object> o = v;
        for(Map.Entry<String, Object> m: o.entrySet()) {
            Object x = m.getValue();
            String n = m.getKey(  );
            if (x != null) {
                if (x instanceof Map) {
                    Map y = (Map) x;
                    Dict.setParams(r, y, n);
                } else {
                    Dict.setParam (r, x, n);
                }
            }
        }
    }

    private void insert(Map r, Map v) throws HongsException {
        Map<String, Object> o = v;
        for(Map.Entry<String, Object> m: o.entrySet()) {
            Object x = m.getValue();
            String n = m.getKey(  );
            if (Dict.getParam(r, n) != null) {
                continue; // 存在就不要设置;
            }
            if (x != null) {
                if (x instanceof Map) {
                    Map y = (Map) x;
                    Dict.setParams(r, y, n);
                } else {
                    Dict.setParam (r, x, n);
                }
            }
        }
    }

    private Map decode(ActionHelper helper, Object x) throws HongsException {
        if (x instanceof Map) {
            return (Map) x;
        }

        String       v   = x.toString();
        Matcher      mat = INJ_PAT.matcher(v);
        StringBuffer buf = new StringBuffer();
        while ( mat.find( ) ) {
            String s = mat.group(1);
            String n = mat.group(2);
            String k = mat.group(3);
            Object o = null;
            if ("context".equals(s)) {
                o = helper.getAttribute(n);
            } else
            if ("session".equals(s)) {
                o = helper.getSessibute(n);
            } else
            if ("cookies".equals(s)) {
                o = helper.getCookibute(n);
            } else
            if ("request".equals(s)) {
                o = helper.getRequestData().get(n);
            }
            if (k != null) {
                Map m  = Synt.declare ( o , Map.class  );
                if (m != null) {
                    o  = Dict.getParam( m , k );
                }
            }
            mat.appendReplacement(buf, Data.toString(o));
        }
        if (buf.length() > 0) {
            v = mat.appendTail(buf).toString( );
        }

        return (Map) Data.toObject(v);
    }

}
