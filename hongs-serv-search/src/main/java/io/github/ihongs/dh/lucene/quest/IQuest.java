package io.github.ihongs.dh.lucene.quest;

import org.apache.lucene.search.Query;

/**
 *
 * @author Hongs
 */
public interface IQuest {
    public Query wdr(String k, Object v);
    public Query whr(String k, Object v);
    public Query whr(String k, Object n, Object x, boolean l, boolean g);
}
