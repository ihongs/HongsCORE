package io.github.ihongs.util.verify;

import io.github.ihongs.HongsException;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.util.Synt;
import io.github.ihongs.util.Tool;
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
    public Object verify(Object value, Wheel watch) throws Wrong {
        // 跳过空值
        if (null == value) {
            return  STAND;
        }

        String str = Synt.declare(value, "");

        // 文本清理
        Set<String> sa = Synt.toSet(getParam("strip"));
        if (null != sa) {
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
        len = Synt.declare(getParam("minlength"), 0);
        if (len > 0 && len > str.length()) {
            throw new Wrong("fore.form.lt.minlength", Integer.toString(len));
        }
        len = Synt.declare(getParam("maxlength"), 0);
        if (len > 0 && len < str.length()) {
            throw new Wrong("fore.form.lt.maxlength", Integer.toString(len));
        }

        if (str.length() == 0) {
            return str;
        }

        // 正则匹配
        Map<String,String> pats;
        try {
            pats = FormSet.getInstance().getEnum("__patts__");
        } catch (HongsException ex) {
            throw ex.toExemption( );
        }
        String patt  = Synt.asString  (  getParam("pattern"));
        String patl  = pats.get (patt);
        if (patl != null && patl.length() != 0) {
            if (!Pattern.compile(patl).matcher(str).matches()) {
                throw new Wrong("fore.form.is.not."+patt);
            }
        } else
        if (patt != null && patt.length() != 0) {
            if (!Pattern.compile(patt).matcher(str).matches()) {
                throw new Wrong("fore.form.is.not.match");
            }
        }

        return str;
    }
}
