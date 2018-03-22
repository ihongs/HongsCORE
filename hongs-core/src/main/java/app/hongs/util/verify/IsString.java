package app.hongs.util.verify;

import app.hongs.HongsException;
import app.hongs.action.FormSet;
import app.hongs.util.Synt;
import app.hongs.util.Tool;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 文本校验
 * <pre>
 * 规则参数:
 *  maxlength   最大长度
 *  minlength   最短长度
 *  pattern     校验正则, 可使用 default.form.xml 里的 _patt_ 下预定好的正则, 如 email,url
 *  strim       为 true 则清理首尾空字符
 *  strip       为 true 则清理空白空行等
 * </pre>
 * @author Hongs
 */
public class IsString extends Rule {
    @Override
    public Object verify(Object value) throws Wrong, HongsException {
        String str = Synt.declare(value, "");

        // 文本清理
        if (Synt.declare(params.get("strim"), false)) {
            str = str.trim();
        }
        if (Synt.declare(params.get("strip"), false)) {
            str = Tool.clearSC (str); // 清除首尾空白
            str = Tool.cleanSC (str); // 合并多个空白
            str = Tool.clearEL (str); // 清除空行
            str = Tool.cleanNL (str); // 统一换行
        }

        // 长度限制
        int len;
        len = Synt.declare(params.get("minlength"), 0);
        if (len > 0 && len > str.length()) {
            throw new Wrong("fore.form.lt.minlength", Integer.toString(len));
        }
        len = Synt.declare(params.get("maxlength"), 0);
        if (len > 0 && len < str.length()) {
            throw new Wrong("fore.form.lt.maxlength", Integer.toString(len));
        }

        if (str.length() == 0) {
            return str;
        }

        // 正则匹配
        Map<String,String> pats = FormSet.getInstance().getEnum("__patts__");
        String patt  = Synt.declare(params.get("pattern"), "");
        String patp  = pats.get(patt);
        if (   patp != null ) {
            if (!Pattern.compile(patp).matcher(str).matches()) {
                throw new Wrong("fore.form.is.not."+patt);
            }
        } else
        if (!"".equals(patt)) {
            if (!Pattern.compile(patt).matcher(str).matches()) {
                throw new Wrong("fore.form.is.not.match");
            }
        }

        return str;
    }
}
