package io.github.ihongs.dh.lucene.stock;

import io.github.ihongs.dh.lucene.query.Vectors;
import io.github.ihongs.util.Synt;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.KnnFloatVectorField;
import org.apache.lucene.index.VectorSimilarityFunction;

/**
 * 向量存储
 * @author Hongs
 */
public class VectorStock implements IStock {
    private VectorSimilarityFunction sim = null;
    public void similarly(VectorSimilarityFunction f) {
        sim = f;
    }

    private int dim = 0;
    public void dimension(int d) {
        dim = d;
    }

    public float[] toVector(Object v) {
        return Vectors.toVector(v, dim);
    }

    @Override
    public Field get(String k, Object v) {
        float[] f = toVector(v);
        StringBuilder b = new StringBuilder();
        for( float x : f) {
            String s = Synt.asString(x);
            b.append( s ).append( "," );
        }
        if (f.length > 0) {
            b.setLength(b.length() - 1);
        }
        return new StoredField(k , b.toString());
    }
    @Override
    public Field whr(String k, Object v) {
        float[] f = toVector(v);
        return sim == null
             ? new KnnFloatVectorField("@"+k, f)
             : new KnnFloatVectorField("@"+k, f, sim);
    }
    @Override
    public Field wdr(String k, Object v) {
        return null; // 对象类型无法执行搜索, 无法增加搜索字段
    }
    @Override
    public Field odr(String k, Object v) {
        return null; // 对象类型无法用于排序, 无法增加排序字段
    }
    @Override
    public Field ods(String k, Object v) {
        return null; // 对象类型无法用于排序, 无法增加排序字段
    }
}
