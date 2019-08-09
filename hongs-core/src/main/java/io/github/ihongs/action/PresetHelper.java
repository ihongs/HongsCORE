package io.github.ihongs.action;

import io.github.ihongs.CoreLogger;
import io.github.ihongs.HongsException;
import io.github.ihongs.util.Dawn;
import io.github.ihongs.util.Dict;
import io.github.ihongs.util.Synt;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 预置数据助手
 * xxx:xxx 为内部预置数据, :default为默认补充, :defense为全局强制, :defence为变更强制.
 * xxx.xxx 为外部打包数据, 例如传参 ab=ios10 可对应 .ios10 的数据, 即针对特定参数打包,
 * @author Hongs
 */
public class PresetHelper {

    private final Pattern INJ_PAT = Pattern.compile("\\(\\$(request|context|session|cookies|mapping)\\.([a-zA-Z0-9_]+)(?:\\.([a-zA-Z0-9_\\.]+))?(?:\\|\\|(.*?))?\\)");
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
     * @throws io.github.ihongs.HongsException
     */
    public PresetHelper addItemsByForm(String conf, String name, String[] deft, String[] defs)
    throws HongsException {
        FormSet form = FormSet.getInstance(conf);

        addDefenseData(form , name + ":defense");
        addDefaultData(form , name + ":default");

        for (String usen : defs) {
            if (name != null
            && (usen.startsWith( ":" )
            ||  usen.startsWith( "." ))) {
                usen  = name  +  usen ;
            }
            addDefenseData(form, usen);
        }

        for (String usen : deft) {
            if (name != null
            && (usen.startsWith( ":" )
            ||  usen.startsWith( "." ))) {
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

    private void insert(Map reqd, Object data, String code, ActionHelper help) {
        // 与 inject 的不同即在于此
        // 对应数据不存在才需要设置
        if (null == Dict.getParam(reqd , code)) {
            inject( reqd , data , code , help );
        }
    }

    private void inject(Map reqd, Object data, String code, ActionHelper help) {
        if (data != null && data instanceof String) {
            String text = ((String) data ).trim();
            Matcher mat = INJ_PAT.matcher( text );

            if (mat.matches()) {
                String s = mat.group(1);
                String n = mat.group(2);
                String k = mat.group(3);
                String x = mat.group(4);

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

                // 下级键, 可向深层提取
                if (k != null) {
                    if (data != null) {
                        data  = Dict.getParam(Synt.asMap(data), k);
                    }
                }

                // 默认值, 没取到则跳过
                if (x != null) {
                    if (data == null) {
                        data  = Dawn.toObject(x);
                    }
                } else {
                    if (data == null) {
                        return;
                    }
                }
            } else
            if (text.equalsIgnoreCase("(VOID)")) {
                /**/ return;
            } else
            if (text.equalsIgnoreCase("(NULL)")) {
                data = null;
            } else
            if (text.startsWith("(") && text.endsWith(")")) {
                text = text.substring(1,text.length( ) - 1);
                data = Dawn.toObject (text);
            } else
            if (text.startsWith("{") && text.endsWith("}")) {
                data = Dawn.toObject (text);
            } else
            if (text.startsWith("[") && text.endsWith("]")) {
                data = Dawn.toObject (text);
            }
        }

        Dict.setParam( reqd, data, code );
    }

}
