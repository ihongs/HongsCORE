package io.github.ihongs.util.verify;

import io.github.ihongs.CoreLocale;
import io.github.ihongs.HongsException;
import io.github.ihongs.util.Dict;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 错误集合
 * @author Hongs
 */
public class Wrongs extends HongsException {
  protected final Map<String,Wrong> wrongs ;
  private Causes caus = null;

    public Wrongs(Map<String,Wrong> wrongs) {
        super( "@fore.form.invalid" );
        this.wrongs = wrongs;
    }

    @Override
    public Throwable getCause() {
        if (caus == null) {
            caus = new Causes(wrongs);
        }
        return ! caus.isEmpty()
               ? caus : null ;
    }

    public Map<String, Wrong > getWrongs() {
        return wrongs;
    }

    public Map<String, String> getErrors() {
        Map<String, String> errors = new LinkedHashMap();
        for (Map.Entry et : wrongs.entrySet()) {
            Wrong  w = (Wrong ) et.getValue( );
            String n = (String) et.getKey  ( );
            String e = w.getLocalizedMistake();
            errors.put ( n, e );
        }
        return errors;
    }

    public Map<String, Object> getErrmap() {
        Map<String, Object> errors = new LinkedHashMap();
        for (Map.Entry et : wrongs.entrySet()) {
            Wrong  w = (Wrong ) et.getValue( );
            String n = (String) et.getKey  ( );
            String e = w.getLocalizedMistake();
            Dict.setParam(errors, e, n);
        }
        return errors;
    }

    @Override
    public String getLocalizedMessage() {
        StringBuilder  sb = new StringBuilder( );
        for (Map.Entry et : wrongs.entrySet()) {
            Wrong  w = (Wrong ) et.getValue( );
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

    @Override
    public int getState() {
        return 400;
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

    private static class Causes extends Throwable {
    private final Map<String, Throwable> causes ;

        public Causes(Map<String, Wrong> wrongs ) {
                causes = new LinkedHashMap();
            for(Map.Entry<String, Wrong> et : wrongs.entrySet()) {
                String    fn = et.getKey(  );
                Wrong     wr = et.getValue();
                Throwable tr = wr.getCause();
                if (tr != null) {
                    causes.put(fn, tr);
                }
            }
        }

        public boolean isEmpty() {
            return causes.isEmpty();
        }

        @Override
        public String getMessage() {
            if (isEmpty()) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
                sb.append("Causes of wrongs");
            for(Map.Entry<String , Throwable> et : causes.entrySet()) {
                sb.append("\r\n")
                  .append(et.getKey( ))
                  .append( ": " )
                  .append(et.getValue().getMessage());
            }
            return sb.toString();
        }

        @Override
        public void printStackTrace(PrintStream ps) {
                ps.print ("Causes of wrongs\r\n");
            for(Map.Entry<String , Throwable> et : causes.entrySet()) {
                ps.print ( et.getKey(  ) + ": " );
                et.getValue().printStackTrace(ps);
            }
        }

        @Override
        public void printStackTrace(PrintWriter ps) {
                ps.print ("Causes of wrongs\r\n");
            for(Map.Entry<String , Throwable> et : causes.entrySet()) {
                ps.print ( et.getKey(  ) + ": " );
                et.getValue().printStackTrace(ps);
            }
        }
    }
}
