package app.hongs.util.verify;

/**
 * 扔掉此值
 */
public class Ignore extends Rule {
    @Override
    public Object verify(Object value) {
        return FALSE;
    }
}
