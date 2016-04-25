package app.hongs.util.verify;

import app.hongs.HongsException;
import app.hongs.action.FormSet;
import app.hongs.util.Synt;
import java.util.Map;
import java.util.regex.Pattern;

public class IsString extends Rule {
    @Override
    public Object verify(Object value) throws Wrong, HongsException {
        String str = Synt.declare(value, "");

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
