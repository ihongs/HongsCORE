package app.hongs.util.verify;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public class Repeated extends Rule {
    @Override
    public Object verify(Object value) throws Wrong {
        if (value instanceof Object[ ] ) {
            return Arrays.asList((Object[]) value);
        }
        if (value instanceof Collection) {
            return value;
        }
        if (value instanceof Map) {
            return value;
        }
        throw new Wrong("fore.form.repeated");
    }
}
