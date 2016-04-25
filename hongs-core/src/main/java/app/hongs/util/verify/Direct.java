package app.hongs.util.verify;

/**
 * 直接通过
 */
public class Direct extends Rule {
    @Override
    public Object verify(Object value) {
        return value;
    }
}
