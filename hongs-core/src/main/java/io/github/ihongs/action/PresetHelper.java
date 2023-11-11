package io.github.ihongs.action;

import io.github.ihongs.CoreLogger;
import io.github.ihongs.HongsException;
import io.github.ihongs.util.Dict;
import io.github.ihongs.util.Dist;
import io.github.ihongs.util.Synt;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 预置数据助手
 * xxxx.xxxx 为内部预置数据, .default为默认补充, .defense为全局强制, .defence为变更强制, .initial为创建补充.
 * xxxx.def. 为外部补充数据, 如传参 ab=v1 将对应 xxxx.def.v1 的数据, 会将其注入到请求中, 但只是补充而非覆盖.
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

    public PresetHelper addDefenseData(FormSet form, String... used) {
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

    public PresetHelper addDefaultData(FormSet form, String... used) {
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
     *  deft,defs 中以 . 打头会把 name 作为前缀,
     *  name=abc, deft=.xyz 会取 abc.xyz 的枚举.
     * @param conf
     * @param name
     * @param deft 默认值
     * @param defs 防御值
     * @return
     * @throws io.github.ihongs.HongsException
     */
    public PresetHelper addItemsByForm(String conf, String name, String[] deft, String[] defs)
    throws HongsException {
        FormSet form = FormSet.getInstance (conf);

        // 缺省指定
        if (defs.length == 0 && deft.length == 0) {
            addDefenseData(form, name+".defense");
            addDefaultData(form, name+".default");
            return this;
        }

        for (String usen : defs) {
            if (name != null
            &&  usen.startsWith(".") ) {
                usen  = name  +  usen ;
            }
            addDefenseData(form, usen);
        }

        for (String usen : deft) {
            if (name != null
            &&  usen.startsWith(".") ) {
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

                if (null != s) switch (s) {
                case "request":
                    data = help.getRequestData().get(n);
                    break;
                case "context":
                    data = help.getAttribute(n);
                    break;
                case "session":
                    data = help.getSessibute(n);
                    break;
                case "cookies":
                    data = help.getCookibute(n);
                    break;
                default:
                    data = reqd.get(n);
                } else {
                    data = reqd.get(n);
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
                        data  = Dist.toObject(x);
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
                data = Dist.toObject (text);
            } else
            if (text.startsWith("{") && text.endsWith("}")) {
                data = Dist.toObject (text);
            } else
            if (text.startsWith("[") && text.endsWith("]")) {
                data = Dist.toObject (text);
            }
        }

        Dict.setParam( reqd, data, code );
    }

}
