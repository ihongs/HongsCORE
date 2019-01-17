package io.github.ihongs.util.verify;

import io.github.ihongs.HongsException;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.util.Synt;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * 单值约束
 * @author Hongs
 */
public class Ordinary extends Rule {
    @Override
    public Object verify(Object value, Wheel watch) throws Wrong {
        /**
         * 数组和集合要取第一个
         * 这样处理就可以适用于 Servlet 的 ParameterMap
         */
        if (value instanceof Object [ ]) {
            Object [ ] a  = (Object [ ]) value;
            return a.length > 0 ? a [0] : null;
        }
        if (value instanceof Collection) {
            Collection c  = (Collection) value;
            Object [ ] a  =  c.toArray();
            return a.length > 0 ? a [0] : null;
        }
        
        /**
         * 对象类型的字段要放行
         * 至于实际仍是多组取值
         * 在这里就没办法分辨了
         */
        if (value instanceof Map) {
            try {
            FormSet fields = FormSet.getInstance();
             Object type   = /****/ getParam ( "__type__"  );
                Map saves  = fields.getEnum  ( "__saves__" );
                Set types  = Synt.toSet(saves.get("object"));
                if (types != null && type != null
                &&  types.contains ( type )) {
                    return value;
                }
            } catch ( HongsException ex) {
                throw ex.toExemption(  );
            }

            throw new Wrong ("fore.form.ordinary");
        }

        return value;
    }
}
