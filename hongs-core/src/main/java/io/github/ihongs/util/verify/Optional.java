package io.github.ihongs.util.verify;

/**
 * 可选约束
 * @author Hongs
 */
public class Optional extends Rule {
    @Override
    public Object verify(Object value, Verity watch) throws Wrong {
        if (null != value) {
            return  value;
        } else {
            return  BLANK;
        }
    }
 }
