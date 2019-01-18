package io.github.ihongs.util.verify;

import io.github.ihongs.util.Synt;
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
        if (def.contains(value)) {
            return null ;
        } else {
            return value;
        }
    }
}
