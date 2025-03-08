package io.github.ihongs.dh.lucene.query;

import io.github.ihongs.util.Synt;
import java.util.List;

/**
 * 向量转换
 * @author Hongs
 */
public final class Vectors {

    /**
     * 转为向量
     * 数值不可转换抛 ClassCastException
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
            f[i] = Synt.declare(j, 0f);
            i ++ ;
        }
        return  f;
    }

    /**
     * 转为向量
     * 数值不可转换抛 ClassCastException
     * 维度不匹配也抛 ClassCastException
     * @param v 取值
     * @param d 维度
     * @return
     */
    public static float[] toVector(Object v, int d) {
        float[] f = toVector(v);
        if (d > 0 && d != f.length) {
            throw new ClassCastException("dimension must be "+d+", but "+ f.length);
        }
        return f;
    }

}
