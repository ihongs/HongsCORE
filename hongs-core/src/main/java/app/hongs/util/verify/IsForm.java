package app.hongs.util.verify;

import app.hongs.HongsException;
import app.hongs.action.VerifyHelper;
import app.hongs.util.Synt;
import java.util.Map;

public class IsForm extends Rule {
    @Override
    public Object verify(Object value) throws Wrongs, HongsException {
        if (value == null || "".equals(value)) {
            return   null; // 允许为空
        }

        String conf = Synt.declare(params.get("conf"), String.class);
        String name = Synt.declare(params.get("form"), String.class);
        if (conf == null || "".equals(conf)) {
            conf = Synt.declare(params.get("__conf__"), "");
        }
        if (name == null || "".equals(name)) {
            name = Synt.declare(params.get("__name__"), "");
        }

        Map data = Synt.declare(value , Map.class);
        VerifyHelper hlpr = new VerifyHelper();
        hlpr.addRulesByForm(conf, name );
        hlpr.isUpdate(helper.isUpdate());
        hlpr.isPrompt(helper.isPrompt());
        return hlpr.verify(data);
    }
}
