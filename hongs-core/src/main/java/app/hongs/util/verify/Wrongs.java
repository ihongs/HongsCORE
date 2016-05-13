package app.hongs.util.verify;

import app.hongs.CoreLocale;
import app.hongs.HongsError;
import app.hongs.HongsException;
import app.hongs.util.Dict;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class Wrongs extends HongsException {
  protected final Map<String,Wrong> wrongs ;

    public Wrongs(Map<String,Wrong> wrongs) {
        super(0x1100 , "fore.form.invalid"); // 0x1100 对应 HTTP 400 错误
        this.setLocalizedSection("default");
        this.wrongs = wrongs;
    }

    public Map<String, Wrong > getWrongs() {
        return wrongs;
    }

    public Map<String, String> getErrors() throws HongsException {
        Map<String, String> errors = new LinkedHashMap();
        for (Map.Entry et : wrongs.entrySet()) {
            Wrong  w = (Wrong )  et.getValue();
            String n = (String)  et.getKey ( );
            String e = w.getLocalizedMessage();
            errors.put(n, e);
        }
        return errors;
    }

    public Map<String, Object> getErrmap() throws HongsException {
        Map<String, Object> errors = new LinkedHashMap();
        for (Map.Entry et : wrongs.entrySet()) {
            Wrong  w = (Wrong )  et.getValue();
            String n = (String)  et.getKey ( );
            String e = w.getLocalizedMessage();
            Dict.setParam(errors, e, n);
        }
        return errors;
    }

    @Override
    public String getLocalizedMessage() {
        StringBuilder sb = new StringBuilder();
        try {
            for (Map.Entry<String, String> et : getErrors().entrySet()) {
                sb.append(et.getValue()).append("\r\n");
            }
        } catch (HongsException ex) {
            throw ex.toUnchecked( );
        }
        return sb.toString().trim();
    }

    /**
     * 转换为响应数据结构
     * @param mode 0错误消息 1单层Map 2复合Map
     * @return
     * @throws HongsException
     */
    public Map toReply(byte mode) throws HongsException {
        Map data = new HashMap();
        data.put( "ok" , false );
        if (mode != 1 && mode != 2 ) {
            data.put("err", "Er400");
            data.put("msg", this.getLocalizedMessage());
        } else {
            Map errs;
            if (mode == 2) {
                errs = this.getErrmap();
            } else {
                errs = this.getErrors();
            }
            data.put("errs", errs  );
            data.put("err", "Er400");
            data.put("msg", CoreLocale.getInstance().translate("fore.form.invalid"));
        }
        return data;
    }
}
