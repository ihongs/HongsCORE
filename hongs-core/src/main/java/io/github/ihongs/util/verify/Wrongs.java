package io.github.ihongs.util.verify;

import io.github.ihongs.CoreLocale;
import io.github.ihongs.HongsException;
import io.github.ihongs.util.Dict;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 错误集合
 * @author Hongs
 */
public class Wrongs extends HongsException {
  protected final Map<String,Wrong> wrongs ;
  protected CoreLocale lang = null;

    public Wrongs(Map<String,Wrong> wrongs) {
        super( 0x400 , "fore.form.invalid");
        this.setLocalizedContext("default");
        this.wrongs = wrongs;
    }

    @Override
    public Wrongs setLocalizedOptions(String...  opts) {
        super.setLocalizedOptions(opts);
        return this;
    }

    @Override
    public Wrongs setLocalizedContext(String     name) {
        super.setLocalizedContext(name);
        this.lang = null;
        return this;
    }

    public Wrongs setLocalizedContext(CoreLocale lang) {
        this.lang = lang;
        return this;
    }

    public Map<String, Wrong > getWrongs() {
        return wrongs;
    }

    public Map<String, String> getErrors() {
        if (null== lang) {
            lang = CoreLocale.getInstance(getLocalizedContext());
        }
        Map<String, String> errors = new LinkedHashMap();
        for (Map.Entry et : wrongs.entrySet()) {
            Wrong  w = (Wrong ) et.getValue( );
                   w.setLocalizedContext(lang);
            String n = (String) et.getKey  ( );
            String e = w.getLocalizedMistake();
            errors.put ( n, e );
        }
        return errors;
    }

    public Map<String, Object> getErrmap() {
        if (null== lang) {
            lang = CoreLocale.getInstance(getLocalizedContext());
        }
        Map<String, Object> errors = new LinkedHashMap();
        for (Map.Entry et : wrongs.entrySet()) {
            Wrong  w = (Wrong ) et.getValue( );
                   w.setLocalizedContext(lang);
            String n = (String) et.getKey  ( );
            String e = w.getLocalizedMistake();
            Dict.setParam(errors, e, n);
        }
        return errors;
    }

    @Override
    public String getLocalizedMessage() {
        if (null== lang) {
            lang = CoreLocale.getInstance(getLocalizedContext());
        }
        StringBuilder  sb = new StringBuilder( );
        for (Map.Entry et : wrongs.entrySet()) {
            Wrong  w = (Wrong ) et.getValue( );
                   w.setLocalizedContext(lang);
            String e = w.getLocalizedMessage();
            sb.append(e).append("\r\n");
        }
        return sb.toString().trim( );
    }

    @Override
    public String getMessage() {
        return getLocalizedMessage();
    }

    @Override
    public String toString() {
        return getLocalizedMessage();
    }

    /**
     * 转换为响应数据结构
     * @param mode 0错误消息 1单层Map 2复合Map
     * @return
     */
    public Map toReply(byte mode) {
        Map data = new HashMap();
        data.put( "ok" , false );
        if (mode != 1 && mode != 2 ) {
            data.put("ern", "Er400");
            data.put("err", "Invalid values");
            data.put("msg", this.getLocalizedMessage());
        } else {
            Map errs;
            if (mode == 2) {
                errs = this.getErrmap();
            } else {
                errs = this.getErrors();
            }
            data.put("errs", errs  );
            data.put("ern", "Er400");
            data.put("err", "Invalid values");
            data.put("msg", CoreLocale.getInstance().translate("fore.form.invalid"));
        }
        return data;
    }
}
