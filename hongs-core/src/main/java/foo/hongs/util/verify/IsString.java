package foo.hongs.util.verify;

import foo.hongs.HongsException;
import foo.hongs.action.FormSet;
import foo.hongs.util.Synt;
import foo.hongs.util.Tool;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 文本校验
 * <pre>
 * 规则参数:
 *  maxlength   最大长度
 *  minlength   最短长度
 *  pattern     校验正则, 可使用 default.form.xml 里的 _patt_ 下预定好的正则, 如 email,url
 *  strip       文本清理: trim 首尾, cros 脚本, tags 标签, ends 首尾(含全角), 可逗号隔多个
 * </pre>
 * @author Hongs
 */
public class IsString extends Rule {
    @Override
    public Object verify(Object value) throws Wrong, HongsException {
        String str = Synt.declare(value, "");

        // 文本清理
        Set<String> sa = Synt.toSet(params.get("strip"));
        if (sa != null) {
            if (sa.contains("cros")) {
                str = Tool.stripTags(str); // 清除脚本
            }
            if (sa.contains("tags")) {
                str = Tool.stripTags(str); // 清除标签
            }
            if (sa.contains("ends")) {
                str = Tool.stripEnds(str); // 首尾清理(含全角)
            }
            if (sa.contains("trim") || sa.contains("true")) {
                str = str.trim();
            }
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
