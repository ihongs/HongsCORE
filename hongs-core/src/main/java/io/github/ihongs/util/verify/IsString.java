package io.github.ihongs.util.verify;

import io.github.ihongs.CruxException;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.util.Syno;
import io.github.ihongs.util.Synt;
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
    public Object verify(Value watch) throws Wrong {
        // 跳过空值
        Object value = watch.get();
        if (null == value) {
            return  PASS;
        }

        String str = Synt.declare(value, "");

        // 文本清理
        Set<String> sa = Synt.toSet(getParam("strip"));
        if (null != sa) {
            if (sa.contains("cros") || sa.contains("html")) {
                str = Syno.stripCros(str); // 清除脚本
            }
            if (sa.contains("tags") || sa.contains("html")) {
                str = Syno.stripTags(str); // 清除标签
            }
            if (sa.contains("ends") || sa.contains("html") || sa.contains("text")) {
                str = Syno.stripEnds(str); // 首尾清理
            }
            if (sa.contains("trim") || sa.contains("html") || sa.contains("text")) {
                str = str.trim();
            }
        }

        // 截取部分
        int len = Synt.declare(getParam("limit"), 0);
        if (len > 0 && len < str.length()) {
            str = str.substring( 0, len );
        }

        // 长度限制
        len = Synt.declare(getParam("minlength"), 0);
        if (len > 0 && len > str.length()) {
            throw new Wrong("@fore.form.lt.minlength", Integer.toString(len), Integer.toString(str.length()));
        }
        len = Synt.declare(getParam("maxlength"), 0);
        if (len > 0 && len < str.length()) {
            throw new Wrong("@fore.form.gt.maxlength", Integer.toString(len), Integer.toString(str.length()));
        }

        if (str.length() == 0) {
            return str;
        }

        // 正则匹配
        Map<String,String> pats;
        try {
            pats = FormSet.getInstance().getEnum("__typos__");
        } catch ( CruxException e) {
            throw e.toExemption( );
        }
            String type = Synt.asString(getParam("__type__"));
            String patt = Synt.asString(getParam("pattern" ));
        if (patt != null && patt.length() != 0) {
            String patl = pats.get(patt);
            if (patl != null && patl.length() != 0) {
                if (!Pattern.compile(patl).matcher(str).matches()) {
                    throw new Wrong ("@fore.form.is.not."+patt);
                }
            } else {
                if (!Pattern.compile(patt).matcher(str).matches()) {
                    throw new Wrong ("@fore.form.is.not.match");
                }
            }
        } else
        if (type != null && type.length() != 0) {
            String patl = pats.get(type);
            if (patl != null && patl.length() != 0) {
                if (!Pattern.compile(patl).matcher(str).matches()) {
                    throw new Wrong ("@fore.form.is.not."+type);
                }
            }
        }

        return str;
    }
}
