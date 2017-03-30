package app.hongs.action.anno;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.FormSet;
import app.hongs.util.Data;
import app.hongs.util.Dict;
import app.hongs.util.Synt;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 预置补充处理器
 * <pre>
 * 参数含义:
 * md=8  将启用对象模式
 * </pre>
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
        if (envm.length() == 0) {
            envm = chains.getEntity();
        }
        if (conf.length() == 0) {
            conf = chains.getModule();
            // 照顾 Module Action 的配置规则
            if (FormSet.hasConfFile(conf+"/"+envm)) {
                conf = conf+"/"+envm ;
            }
        }

        // 补充参数
        try {
            Map<String, String> data = (Map) helper.getAttribute("enum:"+conf+"."+envm+act);
            if (data == null) {
                data  = FormSet.getInstance(conf).getEnum(envm+act);
            }

            Map reqd = helper.getRequestData();

            // 预置参数
            for(String k : used /**/ ) {
                String v = data.get(k);
                if (v != null && v.length() != 0) {
                    Map rxqd = decode(helper , v);
                    insert(reqd, rxqd);
                }
            }

            // 防御参数
            String n = ":defense";
            if (data.containsKey(n)) {
                String v = data.get(n);
                if (v != null && v.length() != 0) {
                    Map rxqd = decode(helper , v);
                    inject(reqd, rxqd);

                    /**
                     * 防御条件
                     * update,delete 通常只用 id 作为参数
                     * 故需要将防御参数作为特定的过滤参数
                     */
                    if (! rxqd.containsKey(Cnst.WH_KEY)) {
                        Map rwqd  =  Synt.declare(reqd.get(Cnst.WH_KEY), Map.class);
                        if (rwqd ==  null) {
                            reqd.put(Cnst.WH_KEY, rxqd);
                        } else {
                            inject  (rwqd /***/ , rxqd);
                        }
                    }
                }
            }
        } catch (HongsException ex) {
            int ec  = ex.getErrno();
            if (ec != 0x10e8 && ec != 0x10e9 && ec != 0x10eb) {
                throw ex ;
            }
        }

        // 是否要求启用对象模式
        byte mode = Synt.declare(helper.getParameter(Cnst.MD_KEY), (byte) -1);
        if ( mode != -1 && 8 == (8 & mode) ) {
            Core.getInstance().put(Cnst.OBJECT_MODE, true);
        }

        chains.doAction();
    }

    private void inject(Map r, Map v) throws HongsException {
        Map<String, Object> o = v;
        for(Map.Entry<String, Object> m: o.entrySet()) {
            Object x = m.getValue();
            String n = m.getKey(  );
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

    private void insert(Map r, Map v) throws HongsException {
        Map<String, Object> o = v;
        for(Map.Entry<String, Object> m: o.entrySet()) {
            Object x = m.getValue();
            String n = m.getKey(  );
            if (Dict.getParam(r, n) == null) {
                continue; // 存在就不要设置;
            }
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

    private Map decode(ActionHelper helper, String v) throws HongsException {
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
        return (Map) Data.toObject(v);
    }

}
