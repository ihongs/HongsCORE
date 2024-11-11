package io.github.ihongs.dh.lucene.stock;

import io.github.ihongs.util.Synt;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.KnnFloatVectorField;
import org.apache.lucene.index.VectorSimilarityFunction;
import static io.github.ihongs.dh.lucene.quest.VectorQuest.toVector;

/**
 * 向量存储
 * @author Hongs
 */
public class VectorStock implements IStock {
    private VectorSimilarityFunction sim = null;
    public void  similar(VectorSimilarityFunction f) {
        this.sim = f;
    }
    @Override
    public Field get(String k, Object v) {
        float[] f = toVector(v);
        StringBuilder b = new StringBuilder();
        for(float  x : f) {
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
