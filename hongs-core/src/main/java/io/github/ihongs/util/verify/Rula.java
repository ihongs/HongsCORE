package io.github.ihongs.util.verify;

import io.github.ihongs.HongsException;

/**
 * 将 Ruly 包装为 Rule
 * @author hong
 */
public final class Rula extends Rule {

  final private Ruly ruly;

    public Rula(Ruly ruly) {
        this.ruly  = ruly;
    }

    @Override
    public  Object  verify (Object value) throws Wrong, Wrongs, HongsException {
        return ruly.verify(value , this);
    }

    public  static  Rule[] wrap(Ruly... rule) {
        Rule[] list = new Rule [rule.length ];
        for (int i= 0; i< rule.length; i ++ ) {
            list[i] = new Rula (rule[  i  ] );
        }
        return list;
    }
}
