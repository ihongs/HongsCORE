package io.github.ihongs.dh.lucene.quest;

import io.github.ihongs.CruxExemption;
import io.github.ihongs.dh.lucene.query.Vectors;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.KnnFloatVectorQuery;
import org.apache.lucene.search.FloatVectorSimilarityQuery;

/**
 * 向量查询
 * @author Hongs
 */
public class VectorQuest implements IQuest {
    private int dim = 0;
    public void dimension(int d) {
        dim = d;
    }

    public float[] toVector(Object v) {
        return Vectors.toVector(v, dim);
    }

    /**
     * 向量查询
     * @param k 字段
     * @param v 取值
     * @param n 最多数量
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
     * @param u 最小相似
     * @return
     */
    public Query vtr(String k, Object v, float u) {
        float[] f = toVector(v);
        return new FloatVectorSimilarityQuery("@"+k, f, u);
    }

    @Override
    public Query wdr(String k, Object v) {
        throw new CruxExemption(1051, "Field "+k+" does not support search");
    }
    @Override
    public Query whr(String k, Object v) {
        throw new CruxExemption(1051, "Field "+k+" does not support filter");
    }
    @Override
    public Query whr(String k, Object n, Object x, boolean l, boolean g) {
        throw new CruxExemption(1051, "Field "+k+" does not support filter");
    }
}
