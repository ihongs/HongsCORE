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
 *  default 默认值, 可使用 =$会话属性, =%应用属性, =%now+-偏移毫秒
 *  default-create yes|no 仅创建的时候设置
 *  default-always yes|no 无论有没有都设置
 * </pre>
 * @author Hongs
 */
public class Default extends Rule {
    @Override
    public Object verify(Object value) {
        if (helper.isUpdate() && Synt.declare(params.get("default-create"), false)) {
            return BLANK;
        }

        if (value != null  &&  ! Synt.declare(params.get("default-always"), false)) {
            return value;
        }

        value = params.get("default");
        String  def = Synt.declare(value, "").trim();

        // 默认时间
        Matcher mat = Pattern.compile("^=%now(([+-])(\\d+))?$").matcher(def);
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

        // 应用属性
        if (def.startsWith("=%")) {
            return Core.getInstance(ActionHelper.class).getAttribute(def.substring(1));
        }

        // 会话属性
        if (def.startsWith("=$")) {
            return Core.getInstance(ActionHelper.class).getSessibute(def.substring(1));
        }

        return  value;
    }
}
