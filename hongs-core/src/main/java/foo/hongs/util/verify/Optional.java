package foo.hongs.util.verify;

/**
 * 可选约束
 * @author Hongs
 */
public class Optional extends Rule {
    @Override
    public Object verify(Object value) throws Wrong {
        if (null != value) {
            return  value;
        } else {
            return  BLANK;
        }
    }
 }
