package io.github.ihongs.util.verify;

import io.github.ihongs.util.Synt;
/*
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
*/
import java.util.Set;

/**
 * 弃用占位
 *
 * <pre>
 * 规则参数:
 *  defiant 为指定值转为 null 移交后续处理, 此同名属性在 Repeated 中为忽略取值
 * </pre>
 *
 * @author Hongs
 */
public class Defiant extends Rule {
    @Override
    public Object verify(Value watch) {
        Object value = watch.get( );
        if (null== value) {
            return STAND;
        }
        Object fiant = getParam("defiant");
        if (null== fiant) {
            return STAND;
        }

        Set def ;
        if (fiant.equals( ""  )) {
            def = Synt.setOf( ""  );
        } else {
            def = Synt.toSet(fiant);
        }

        /*
        // 多值校验中会处理
        if (getParam("__repeated__", false)) {
            if (value instanceof Collection) {
                Collection vs = (Collection) value;
                Iterator   it = vs.iterator( );
                while (it.hasNext()) {
                    Object v  = it.next( );
                    if (def.contains(v)) {
                        it.remove();
                    }
                }
            } else
            if (value instanceof Map) {
                Map        vs = (Map) value;
                Set        vz = vs.entrySet( );
                Iterator   it = vz.iterator( );
                while (it.hasNext()) {
                    Map.Entry et = (Map.Entry) it.next();
                    Object v  = et.getValue( );
                    if (def.contains(v)) {
                        it.remove();
                    }
                }
            }
        }
        */

        if (! def.contains(value)) {
            return value;
        } else {
            return null ;
        }
    }
}
