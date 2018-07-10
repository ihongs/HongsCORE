package app.hongs.util.verify;

import app.hongs.Core;
import app.hongs.action.ActionHelper;
import app.hongs.util.Synt;
import app.hongs.util.Dict;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 默认取值
 * <pre>
 * 规则参数:
 *  default 默认值, 可使用 =@别名字段 =$会话属性, =%应用属性, =%now+-偏移毫秒
 *  deforce 强制写, 控制不同阶段, create 创建时, update 更新时, always 任何时
 * </pre>
 * @author Hongs
 */
public class Default extends Rule {
    @Override
    public Object verify(Object value) {
        Object force = params.get("deforce");
        if ( "update".equals(force)) {
            if (helper.isUpdate() != true) {
                return BLANK;
            }
        } else
        if ( "create".equals(force)) {
            if (helper.isUpdate() == true) {
                return BLANK;
            }
        } else
        if (!"always".equals(force)) {
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
        Matcher mat = NOW.matcher(def);
        if (mat.matches()) {
           Date now = new Date();
            if (mat.group(1).equals("time") ) {
                now.setTime( Core.ACTION_TIME.get());
            }
            if (mat.group(2) != null) {
               Long msc = Synt.declare( mat.group(4), 0L );
                if ("+".equals(mat.group(3))) {
                    now.setTime(now.getTime() + msc);
                } else {
                    now.setTime(now.getTime() - msc);
                }
            }
            return  now;
        }

        // 别名字段
        if (def.startsWith("=@")) {
            return Dict.get(cleans,BLANK,def.substring(2));
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

    private static final Pattern NOW = Pattern.compile ("^=%(time|now)([+\\-]\\d+)?$");
}
