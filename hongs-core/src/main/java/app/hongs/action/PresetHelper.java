package app.hongs.action;

import app.hongs.CoreLogger;
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

    private final Pattern INJ_PAT = Pattern.compile("\\(\\$(request|context|session|cookies|mapping)\\.([a-zA-Z0-9_]+)(?:\\.([a-zA-Z0-9_\\.]+))?\\)");
    private final Map<String, Object> defenseData;
    private final Map<String, Object> defaultData;

    public PresetHelper() {
        defenseData = new LinkedHashMap();
        defaultData = new LinkedHashMap();
    }

    public PresetHelper addDefenseData(String code, Object data) {
        defenseData.put(code, data);
        return this;
    }

    public PresetHelper addDefaultData(String code, Object data) {
        defaultData.put(code, data);
        return this;
    }

    public PresetHelper addDefenseData(Map<String, Object> data) {
        defenseData.putAll(data);
        return this;
    }

    public PresetHelper addDefaultData(Map<String, Object> data) {
        defaultData.putAll(data);
        return this;
    }

    public PresetHelper addDefenseData(FormSet form, String... used)
    throws HongsException {
        for(String usen : used) {
            try {
                Map data  = form.getEnum(usen);
                if (data != null) {
                    addDefenseData(data);
                }
            } catch(HongsException drop) {
                // Ignore enum data not exists.
                CoreLogger.trace("Defense preset data missing, {}", drop.getError());
            }
        }
        return this;
    }

    public PresetHelper addDefaultData(FormSet form, String... used)
    throws HongsException {
        for(String usen : used) {
            try {
                Map data  = form.getEnum(usen);
                if (data != null) {
                    addDefaultData(data);
                }
            } catch(HongsException drop) {
                // Ignore enum data not exists.
                CoreLogger.trace("Default preset data missing, {}", drop.getError());
            }
        }
        return this;
    }

    /**
     * 以表单配置追加预设值
     * 注意:
     *  deft,defs 中以 :,! 打头会把 name 作为前缀,
     *  name=abc,deft=:xyz 会取配置的枚举 abc:xyz,
     *  ! 用于外部默认参数
     * @param conf
     * @param name
     * @param deft 默认值
     * @param defs 防御值
     * @return
     * @throws HongsException
     */
    public PresetHelper addItemsByForm(String conf, String name, String[] deft, String[] defs)
    throws HongsException {
        FormSet form = FormSet.getInstance(conf);

        addDefenseData(form , name + ":defense");
        addDefaultData(form , name + ":default");

        for (String usen : defs) {
            if (name != null
            && (usen.startsWith( ":" )
            ||  usen.startsWith( "!" ))) {
                usen  = name  +  usen ;
            }
            addDefenseData(form, usen);
        }

        for (String usen : deft) {
            if (name != null
            && (usen.startsWith( ":" )
            ||  usen.startsWith( "!" ))) {
                usen  = name  +  usen ;
            }
            addDefaultData(form, usen);
        }

        return this;
    }

    public void preset(Map reqd, ActionHelper helper) {
        for(Map.Entry<String, Object> et : defenseData.entrySet()) {
            Object data = et.getValue();
            String code = et.getKey(  );
            inject(reqd , data , code , helper);
        }

        for(Map.Entry<String, Object> et : defaultData.entrySet()) {
            Object data = et.getValue();
            String code = et.getKey(  );
            insert(reqd , data , code , helper);
        }
    }

    private void inject(Map r, Object x, String n, ActionHelper h) {
        x = decode( x, r, h );
        if (null == x) {
            return;
        }

        if (x instanceof Map) {
            Map y = (Map) x;
            Dict.setParams(r, y, n);
        } else {
            Dict.setParam (r, x, n);
        }
    }

    private void insert(Map r, Object x, String n, ActionHelper h) {
        // 与 inject 的不同即在于此
        // 如果对应数据已存在则跳过
        if (null != Dict.getParam(r , n)) {
            return;
        }

        x = decode( x, r, h );
        if (null == x) {
            return;
        }

        if (x instanceof Map) {
            Map y = (Map) x;
            Dict.setParams(r, y, n);
        } else {
            Dict.setParam (r, x, n);
        }
    }

    private Object decode(Object data, Map reqd, ActionHelper help) {
        if (data == null) {
            return  data;
        }

        if (data instanceof String) {
            String text = ((String) data ).trim();
            Matcher mat = INJ_PAT.matcher( text );

            if (mat.matches()) {
                String s = mat.group(1);
                String n = mat.group(2);
                String k = mat.group(3);

                if ("request".equals(s)) {
                    data = help.getRequestData().get(n);
                } else
                if ("context".equals(s)) {
                    data = help.getAttribute(n);
                } else
                if ("session".equals(s)) {
                    data = help.getSessibute(n);
                } else
                if ("cookies".equals(s)) {
                    data = help.getCookibute(n);
                } else
                {
                    data = reqd.get (n);
                }

                if (k != null) {
                    Map  d   = Synt.asMap   (data);
                    if ( d  != null) {
                        data = Dict.getParam(d, k);
                    } else {
                        data = null;
                    }
                }
            } else
            if (text.startsWith("(") && text.endsWith(")")) {
                text = text.substring(1,text.length( ) - 1);
                data = Data.toObject (text);
            } else
            if (text.startsWith("{") && text.endsWith("}")) {
                data = Data.toObject (text);
            } else
            if (text.startsWith("[") && text.endsWith("]")) {
                data = Data.toObject (text);
            }
        }

        return  data;
    }

}
