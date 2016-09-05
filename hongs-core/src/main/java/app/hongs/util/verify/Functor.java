package app.hongs.util.verify;

import app.hongs.HongsException;

/**
 * 函数式规则
 * 可简化代码
 * <pre>
 *  values = new Verify()
 *      .addRule("f1", (v, r)->{
 *          return v != null ? v : BLANK;
 *      })
 *      .addRule("f2", (v, r)->{
 *          return v != null ? v : EMPTY;
 *      })
 *      .verify(values);
 * </pre>
 * @author Hongs
 */
public class Functor extends Rule {

    private final  Rune rune;

    public Functor(Rune rune) {
        this.rune = rune;
    }

    @Override
    public Object verify(Object value) throws Wrong, Wrongs, HongsException {
        return rune.run (value, this );
    }

    public static interface Rune {
        public Object run(Object value, Rule rule);
    }

}
