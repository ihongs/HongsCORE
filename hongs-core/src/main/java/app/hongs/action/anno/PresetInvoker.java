package app.hongs.action.anno;

import app.hongs.Cnst;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.FormSet;
import app.hongs.util.Data;
import app.hongs.util.Dict;
import app.hongs.util.Synt;
import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 预置补充处理器
 * @author Hong
 */
public class PresetInvoker implements FilterInvoker {

    private  final Pattern INJ_PAT = Pattern.compile("\\(\\$(request|context|session|cookies)\\.([a-zA-Z0-9_]+)(?:\\.([a-zA-Z0-9_\\.]+))?\\)");

    @Override
    public void invoke(ActionHelper helper, ActionRunner chains, Annotation anno)
    throws HongsException {
        Preset   ann  = (Preset) anno;
        String   conf = ann.conf();
        String   envm = ann.envm();
        String[] used = ann.used();

        if (used == null || used.length == 0) {
            Set<String> uzed = Synt.asTerms(helper.getParameter(Cnst.UD_KEY));
        if (uzed != null && ! uzed.isEmpty()) {
            used  = uzed.toArray(new String[] {});
        }
        if (used == null || used.length == 0) {
            used  =  new String[] { ":default" } ;
        }
        }

        // 获取行为
        String act ;
        int pos  = envm.indexOf  (  ':'  );
        if (pos >= 1) {
            act  = envm.substring(0 + pos);
            envm = envm.substring(0 , pos);
        } else
        if (pos == 0) {
            act  = envm;
            envm =  "" ;
        } else {
            act  = ":preset";
        }

        // 识别路径
        if (envm.length() == 0 || conf.length() == 0) {
            envm = chains.getEntity();
            conf = chains.getModule();
            if (FormSet.hasConfFile( conf+"/"+envm )) {
                conf = conf+"/"+envm ;
            }
        }

        // 补充参数
        try {
            Map<String, String> data = FormSet.getInstance(conf).getEnum(envm + act);
            Map reqd = helper.getRequestData();
            Map rxqd = new HashMap(  );

            // 防御参数
            String n = ":defense";
            if (data.containsKey( n )) {
                String v = data.get(n);
                if (v != null && v.length() != 0) {
                    inject(helper, v, reqd);
                }
            }

            // 预置参数
            for(String k : used /**/ ) {
                String v = data.get(k);
                if (v != null && v.length() != 0) {
                    inject(helper, v, rxqd);
                }
            }

            /**
             * 执行了两次 putAll
             * 目的在于确保当前请求参数优先
             * 如当前请求参数有a
             * 而补充参数中也有a
             * 则用当前请求参数a
             * 但 :defence 除外
             */
            Dict.putAll(rxqd, reqd);
            reqd.putAll(rxqd  /**/);
        } catch (HongsException ex) {
            int ec  = ex.getCode( );
            if (ec != 0x10e8 && ec != 0x10e9 && ec != 0x10eb) {
                throw ex ;
            }
        }

        chains.doAction();
    }

    private  void  inject(ActionHelper helper, String v, Map r) throws HongsException {
        v = inject(helper , v);
        Map<String, Object> o = (Map) Data.toObject(v);
        for(Map.Entry<String, Object> m: o.entrySet()) {
            String n = m.getKey(  );
            Object x = m.getValue();
            if (x != null) {
                if (x instanceof Map) {
                    Map y = (Map) x;
                    Dict.setParams(r, y, n);
                } else {
                    Dict.setParam (r, x, n);
                }
            }
        }
    }

    private String inject(ActionHelper helper, String v) throws HongsException {
        Matcher      mat = INJ_PAT.matcher(v);
        StringBuffer buf = new StringBuffer();
        while ( mat.find( )  ) {
            String s = mat.group(1);
            String n = mat.group(2);
            String k = mat.group(3);
            Object o = null;
            if ("context".equals(s)) {
                o = helper.getAttribute(n);
            } else
            if ("session".equals(s)) {
                o = helper.getSessibute(n);
            } else
            if ("cookies".equals(s)) {
                o = helper.getCookibute(n);
            } else
            if ("request".equals(s)) {
                o = helper.getRequestData().get(n);
            }
            if (k != null) {
                Map m  = Synt.declare ( o , Map.class  );
                if (m != null) {
                    o  = Dict.getParam( m , k );
                }
            }
            mat.appendReplacement(buf, Data.toString(o));
        }
        if (buf.length() > 0) {
               v = mat.appendTail(buf).toString( );
        }
        return v;
    }

}
