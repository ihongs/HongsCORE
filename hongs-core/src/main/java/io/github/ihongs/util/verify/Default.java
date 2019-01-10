package io.github.ihongs.util.verify;

import io.github.ihongs.Core;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.util.Dict;
import io.github.ihongs.util.Tool;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 默认取值
 * <pre>
 * 规则参数:
 *  default 默认值, 可使用 =@别名字段 =$会话属性, =%应用属性, =%now+-偏移毫秒
 *  deforce 强制写, 控制不同阶段, create 创建时, update 更新时, always 任何时
 *  deforce 还可设为 blanks, 此时为空则返回空值, 可用 SelectHelper 在读时补全
 * </pre>
 * @author Hongs
 */
public class Default extends Rule {
    @Override
    public Object verify(Object value, Verity watch) {
        Object force = getParam("deforce");
        if ("create".equals(force)) {
            if (watch.isUpdate() == true ) {
                return BLANK;
            }
        } else
        if ("update".equals(force)) {
            if (watch.isUpdate() == false) {
                return BLANK;
            }
        } else
        if ("always".equals(force) == false) { // 非 always
        if ("blanks".equals(force) == false) { // 非 blanks
            /**
             * 一般情况
             * 空串空值将设为默认值
             * 更新时没赋值将会跳过
             */
            if (! "".equals(value) && value != null) {
                return value;
            }
            if (watch.isUpdate() && ! watch.isValued()) {
                return BLANK;
            }
        } else {
            /**
             * 留空模式
             * 空串空值将设为空对象
             * 更新时没赋值也会跳过
             */
            if (! "".equals(value) && value != null) {
                return value;
            }
            if (watch.isUpdate() && ! watch.isValued()) {
                return BLANK;
            } else {
                return EMPTY;
            }
        }}

        Object val = getParam("default");
        String def = val != null ? val.toString(  ).trim(  ) : "";
        String bef = def.length() >= 2 ? def.substring(0, 2) : "";

        // 拼接字段
        if (bef.equals("=~")) {
            return Tool.inject(def.substring(2), watch.getCleans());
        }

        // 别名字段
        if (bef.equals("=@")) {
            return Dict.getParam(watch.getCleans(), BLANK, def.substring(2));
        }

        // 会话属性
        if (bef.equals("=$")) {
            return Core.getInstance(ActionHelper.class).getSessibute(def.substring(2));
        }

        // 应用属性
        if (bef.equals("=#")) {
            return Core.getInstance(ActionHelper.class).getAttribute(def.substring(2));
        }

        // 新唯一ID
        if (def.equals("=%id")) {
            return Core.newIdentity();
        }

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
            String flag = mat.group(1);
            String plus = mat.group(2);
            if (flag.length() == 4) {// time 字串长度
                now.setTime(Core.ACTION_TIME.get( ));
            }
            if (plus != null) {
               Long msc = Long.valueOf(plus.substring(1));
                if ("+".equals(plus.substring(0,1))) {
                    now.setTime(now.getTime() + msc);
                } else {
                    now.setTime(now.getTime() - msc);
                }
            }
            return  now;
        }

            return  val;
    }

    private static final Pattern NOW = Pattern.compile ("^=%(time|now)([+\\-]\\d+)?$");
}
