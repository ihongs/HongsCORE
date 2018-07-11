package foo.hongs.util.verify;

import foo.hongs.util.Synt;
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
    public Object verify(Object value) {
        Object prm = params.get("defiant");
        Set    def ;
        if ("".equals(prm)) {
            def = Synt.setOf("" );
        } else {
            def = Synt.toSet(prm);
        }
        if (null != value && ! def.contains(value)) {
            return  value;
        } else {
            return  EMPTY;
        }
    }
}
