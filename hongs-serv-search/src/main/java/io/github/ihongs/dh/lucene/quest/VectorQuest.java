package io.github.ihongs.dh.lucene.quest;

import java.util.List;
import io.github.ihongs.util.Synt;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.KnnFloatVectorQuery;
import org.apache.lucene.search.FloatVectorSimilarityQuery;

/**
 * 向量查询
 * @author Hongs
 */
public class VectorQuest implements IQuest {
    /**
     * 转为向量
     * @param v 取值
     * @return
     */
    public static float[] toVector(Object v) {
        if (v == null) {
            return new float [0];
        }
        if (v instanceof float[]) {
            return ( float[] ) v;
        }
        List    l;
        float[] f;
        int     i;
        i = 0;
        l = Synt.toList(v);
        f = new float[l.size( )];
        for (Object j : l) {
            f [i] = Synt.declare(j, 0f);
            i ++;
        }
        return f;
    }

    /**
     * 向量查询
     * @param k 字段
     * @param v 取值
     * @param n 数量
     * @return
     */
    public Query vtr(String k, Object v, int n) {
        float[] f = toVector(v);
        return new KnnFloatVectorQuery("@"+k, f, n);
    }

    /**
     * 向量查询
     * @param k 字段
     * @param v 取值
     * @param g 最小
     * @param l 最大
     * @return
     */
    public Query vtr(String k, Object v, float g, float l) {
        float[] f = toVector(v);
        return new FloatVectorSimilarityQuery("@"+k, f, g, l);
    }

    @Override
    public Query wdr(String k, Object v) {
        throw new UnsupportedOperationException("Field "+k+" does not support search");
    }
    @Override
    public Query whr(String k, Object v) {
        throw new UnsupportedOperationException("Field "+k+" does not support filter");
    }
    @Override
    public Query whr(String k, Object n, Object x, boolean l, boolean g) {
        throw new UnsupportedOperationException("Field "+k+" does not support filter");
    }
}
