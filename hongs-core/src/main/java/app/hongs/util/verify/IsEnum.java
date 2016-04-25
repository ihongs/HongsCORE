package app.hongs.util.verify;

import app.hongs.HongsException;
import app.hongs.action.FormSet;
import app.hongs.util.Synt;
import java.util.Map;

public class IsEnum extends Rule {
    @Override
    public Object verify(Object value) throws Wrong, HongsException {
        if (value == null || "".equals(value)) {
            return   null; // 允许为空
        }

        String conf = Synt.declare(params.get("conf"), String.class);
        String name = Synt.declare(params.get("enum"), String.class);
        if (conf == null || "".equals(conf)) {
            conf = Synt.declare(params.get("__conf__"), "");
        }
        if (name == null || "".equals(name)) {
            name = Synt.declare(params.get("__name__"), "");
        }

        Map data = FormSet.getInstance(conf).getEnum(name);
        if (! data.containsKey( value.toString() ) ) {
            throw new Wrong("fore.form.not.in.enum");
        }
        return  value;
    }
}
