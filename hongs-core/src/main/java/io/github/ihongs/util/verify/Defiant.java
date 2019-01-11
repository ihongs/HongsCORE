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
    public Object verify(Object value, Verity watch) {
        Object ant = getParam("defiant");
        if (ant == null) return value;
        Set def ;
        if (ant . equals    ("" )) {
            def = Synt.setOf("" );
        } else {
            def = Synt.toSet(ant);
        }
        if (null != value && ! def.contains( value )) {
            return  value;
        } else {
            return  null ;
        }
    }
}
