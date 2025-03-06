package io.github.ihongs.dh.lucene.quest;

import org.apache.lucene.search.Query;

/**
 * 数值查询
 * @author Hongs
 */
abstract public class NumberQuest implements IQuest {
    @Override
    public Query wdr(String k, Object v) {
        throw new UnsupportedOperationException("Field "+k+" does not support search");
    }
}
