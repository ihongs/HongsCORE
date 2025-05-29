package io.github.ihongs.dh.lucene.quest;

import io.github.ihongs.CruxExemption;
import org.apache.lucene.search.Query;

/**
 * 数值查询
 * @author Hongs
 */
abstract public class NumberQuest implements IQuest {
    @Override
    public Query wdr(String k, Object v) {
        throw new CruxExemption(1051, "Field "+k+" does not support search");
    }
}
