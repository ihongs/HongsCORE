package app.hongs.util.verify;

import app.hongs.Core;
import app.hongs.util.Synt;
import app.hongs.action.ActionHelper;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 默认取值
 * <pre>
 * 规则参数:
 *  default 默认值, 可使用 =@请求参数 =$会话属性, =%应用属性, =%now+-偏移毫秒
 *  default-create yes|no 仅创建的时候设置
 *  default-always yes|no 无论有没有都设置
 * </pre>
 * @author Hongs
 */
public class Default extends Rule {
    @Override
    public Object verify(Object value) {
        if ( Synt.declare(params.get("default-create"), false)) {
            if (helper.isUpdate()) {
                return BLANK;
            }
        }

        if (!Synt.declare(params.get("default-always"), false)) {
            if (helper.isUpdate() && value == null) {
                return BLANK;
            }
            if (!"".equals(value) && value != null) {
                return value;
            }
        }

        value = params.get("default");
        String  def = Synt.declare(value, "").trim();

        // 动作选项
        if (def.equals("=%zone")) {
            return Core.ACTION_ZONE.get();
        }
        if (def.equals("=%lang")) {
            return Core.ACTION_LANG.get();
        }
        if (def.equals("=%addr")) {
            return Core.CLIENT_ADDR.get();
        }

        // 默认时间
        Matcher mat = Pattern.compile("^=%now(([+\\-])(\\d+))?$").matcher(def);
        if (mat.matches()) {
            Date now = new Date();
            if (mat.group(1) != null) {
                Long msc = Synt.declare(mat.group(2), 0L);
                if ("+".equals(mat.group(1))) {
                    now.setTime(now.getTime() + msc);
                } else {
                    now.setTime(now.getTime() - msc);
                }
            }
            return  now;
        }

        // 别名属性
        if (def.startsWith("=@")) {
            def= def.substring(2);
            return cleans.containsKey(def) ? cleans.get(def) : values.get(def);
        }

        // 会话属性
        if (def.startsWith("=$")) {
            return Core.getInstance(ActionHelper.class).getSessibute(def.substring(2));
        }

        // 应用属性
        if (def.startsWith("=%")) {
            return Core.getInstance(ActionHelper.class).getAttribute(def.substring(2));
        }

        return  value;
    }
}
