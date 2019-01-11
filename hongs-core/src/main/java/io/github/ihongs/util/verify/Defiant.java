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
    public Object verify(Object value, Veri watch) {
        Object ant = getParam ("defiant");
        if (ant == null || value == null) {
            return value;
        }

        Set def ;
        if (ant . equals    ("" )) {
            def = Synt.setOf("" );
        } else {
            def = Synt.toSet(ant);
        }
        if (def . contains(value)) {
            return null ;
        } else {
            return value;
        }
    }
}
