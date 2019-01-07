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
public class NoRepeat extends Rule {
    @Override
    public Object verify(Object value) throws Wrong {
        if (value instanceof Map) {
            /**
             * 对象类型的字段要放行
             * 至于下层是否多组取值
             * 只能在后面另行检验了
             */
            try {
            FormSet fields = FormSet.getInstance ();
            Object  type   = params.get/**/  ( "__type__"  );
                Map saves  = fields.getEnum  ( "__saves__" );
                Set types  = Synt.toSet(saves.get("object"));
                if (types != null && type != null
                &&  types.contains ( type )) {
                    return value;
                }
            } catch (HongsException ex) {
                throw new Wrong(ex.getMessage( ));
            }

            throw new Wrong("fore.form.norepeat");
        }
        if (value instanceof Collection) {
            throw new Wrong("fore.form.norepeat");
        }
        if (value instanceof Object[ ] ) {
            throw new Wrong("fore.form.norepeat");
        }
        return value;
    }
}
