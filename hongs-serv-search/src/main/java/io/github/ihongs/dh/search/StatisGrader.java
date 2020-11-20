package io.github.ihongs.dh.search;

import io.github.ihongs.util.Synt;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.util.NumericUtils;

/**
 * 分类统计工具
 * @author Kevin
 */
public class StatisGrader {

    public static enum TYPE {
        INT , LONG , FLOAT , DOUBLE , STRING , // 单数
        INTS, LONGS, FLOATS, DOUBLES, STRINGS  // 复数
    };

    private final IndexSearcher finder;
    private       Field[] fields;
    private       Query   query ;

    public StatisGrader (IndexSearcher finder) {
        this.finder = finder;
    }

    /**
     * 限定字段
     * @param fields
     * @return
     */
    public StatisGrader field(Field... fields) {
        this.fields = fields;
        return this;
    }

    /**
     * 限定条件
     * @param query
     * @return
     */
    public StatisGrader where(Query    query ) {
        this.query  = query ;
        return this;
    }

    /**
     * 一般查询
     * @param coller
     * @throws IOException
     */
    public void search(Coller coller) throws IOException {
        finder. search(query, new Collec(coller, fields));
    }

    /**
     * 分类统计
     * @return
     * @throws IOException
     */
    public Map<String, Map<Object, Long>> acount() throws IOException {
        Map<String, Map<Object, Long>> counts = new HashMap();
        Map<String, Set<Object      >> countx = new HashMap();
        for(Field field : fields) {
            counts.put(field.alias, new HashMap());
        }
        search(new Acount(counts, countx));
        return counts;
    }

    /**
     * 数值计算
     * @return
     */
    public Map<String, Map<Range, Ratio>> amount() throws IOException {
        Map<String, Map<Range, Ratio>> counts = new HashMap();
        Map<String, Set<Range       >> countx = new HashMap();
        for(Field field : fields) {
            counts.put(field.alias, new HashMap());
        }
        search(new Amount(counts, countx));
        return counts;
    }

    /**
     * 字段
     */
    public static class Field {

        public final TYPE   type ;
        public final String filed;
        public final String alias;

        public Field(String field, String alias, TYPE type) {
            this.type  = type ;
            this.filed = field;
            this.alias = alias;
        }

        /**
         * 获取当前字段的 DocValues
         * 返回类型 SortedDocValues|SortedSetDocValues|NumericDocValues|SortedNumericDocValues
         * @param r
         * @return
         * @throws IOException
         */
        public Object getDocValues(LeafReader r) throws IOException {
            switch (type) {
                case INT:
                case LONG:
                case FLOAT:
                case DOUBLE:
                    return r.getNumericDocValues(filed);
                case STRING:
                    return r.getSortedDocValues (filed);
                case INTS:
                case LONGS:
                case FLOATS:
                case DOUBLES:
                    return r.getSortedNumericDocValues(filed);
                case STRINGS:
                    return r.getSortedSetDocValues    (filed);
            }
            return  null;
        }

    }

    /**
     * 采集器
     */
    public static class Collec implements Collector, LeafCollector {

        protected final Coller   coller;
        protected       Object[] values;
        protected       Field [] fields;
        private         long     total ;

        public Collec(Coller coller, Field... fields) {
            this.coller = coller;
            this.fields = fields;
            this.values = new Object[fields.length];

            // 初始化
            if (coller instanceof Collar) {
              ((Collar ) coller ).collate ( fields);
            }
        }

        @Override
        public LeafCollector getLeafCollector(LeafReaderContext lrc) throws IOException {
            LeafReader r  = lrc.reader();
            for (int i = 0; i < fields.length; i ++ ) {
                values[i] = fields[i].getDocValues(r);
            }
            return this;
        }

        @Override
        public void collect(int doc) throws IOException {
            total ++ ;

            for (int i = 0; i < fields.length; i ++) {
                Field  f = fields[i];
                Object d = values[i];
                if (d == null) {
                    continue ;
                }

                switch(f.type) {
                    case INT : {
                        NumericDocValues b = (NumericDocValues) d;
                        if (!b.advanceExact(doc)) {
                            continue;
                        }
                        coller.collect(f , (int ) b.longValue());
                        break;
                    }
                    case LONG: {
                        NumericDocValues b = (NumericDocValues) d;
                        if (!b.advanceExact(doc)) {
                            continue;
                        }
                        coller.collect(f , (long) b.longValue());
                        break;
                    }
                    case FLOAT: {
                        NumericDocValues b = (NumericDocValues) d;
                        if (!b.advanceExact(doc)) {
                            continue;
                        }
                        coller.collect(f , NumericUtils. sortableIntToFloat ((int ) b.longValue()));
                        break;
                    }
                    case DOUBLE: {
                        NumericDocValues b = (NumericDocValues) d;
                        if (!b.advanceExact(doc)) {
                            continue;
                        }
                        coller.collect(f , NumericUtils.sortableLongToDouble((long) b.longValue()));
                        break;
                    }
                    case STRING: {
                        SortedDocValues  b = (SortedDocValues ) d;
                        if (!b.advanceExact(doc)) {
                            continue;
                        }
                        coller.collect(f , b.binaryValue().utf8ToString() );
                        break;
                    }
                    case INTS : {
                        SortedNumericDocValues b = (SortedNumericDocValues) d;
                        if (!b.advanceExact(doc)) {
                            continue;
                        }
                        Object[] v = new Object[b.docValueCount()];
                        for( int j = 0 ; j < v.length ; j ++ ) {
                            v[j] =(int ) b.nextValue();
                        }
                        coller.collect(f , v);
                        break;
                    }
                    case LONGS: {
                        SortedNumericDocValues b = (SortedNumericDocValues) d;
                        if (!b.advanceExact(doc)) {
                            continue;
                        }
                        Object[] v = new Object[b.docValueCount()];
                        for( int j = 0 ; j < v.length ; j ++ ) {
                            v[j] =(long) b.nextValue();
                        }
                        coller.collect(f , v);
                        break;
                    }
                    case FLOATS: {
                        SortedNumericDocValues b = (SortedNumericDocValues) d;
                        if (!b.advanceExact(doc)) {
                            continue;
                        }
                        Object[] v = new Object[b.docValueCount()];
                        for( int j = 0 ; j < v.length ; j ++ ) {
                            v[j] = NumericUtils. sortableIntToFloat ((int ) b.nextValue());
                        }
                        coller.collect(f , v);
                        break;
                    }
                    case DOUBLES: {
                        SortedNumericDocValues b = (SortedNumericDocValues) d;
                        if (!b.advanceExact(doc)) {
                            continue;
                        }
                        Object[] v = new Object[b.docValueCount()];
                        for( int j = 0 ; j < v.length ; j ++ ) {
                            v[j] = NumericUtils.sortableLongToDouble((long) b.nextValue());
                        }
                        coller.collect(f , v);
                        break;
                    }
                    case STRINGS: {
                        SortedSetDocValues b = (SortedSetDocValues) d;
                        if (!b.advanceExact(doc)) {
                            continue;
                        }
                        List<String> u;
                        Object[]     v;
                        long         j;
                        u = new LinkedList( );
                        while ((j = b.nextOrd()) != SortedSetDocValues.NO_MORE_ORDS) {
                            u .add(b.lookupOrd(j).utf8ToString());
                        }
                        v = u .toArray( new String [ u.size() ] );
                        coller.collect(f , v);
                        break;
                    }
                }
            }
        }

        @Override
        public void setScorer(Scorer scorer) {
            // 默认无需打分
        }

        @Override
        public boolean needsScores() {
            return false;
        }

        public long    countTotals() {
            return total;
        }

    }

    /**
     * 预处理
     */
    public static interface Collar {
        public void collate (Field... fields);
    }

    /**
     * 处理器
     */
    public static interface Coller {
        public void collect (Field field, Object... values);
    }

    /**
     * 分类统计处理器
     */
    public static class Acount implements Coller {

        private final Map<String, Map<Object, Long>> counts;
        private final Map<String, Set<Object      >> countx;
        private final Set<String                   > limits;

        public Acount(Map<String, Map<Object, Long>> counts,
                      Map<String, Set<Object      >> countx) {
            this.counts = counts;
            this.countx = countx;
            this.limits = new HashSet();

            // 无需额外值的字段
            for(Map.Entry<String, Map<Object, Long>> entry : counts.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    limits.add( entry. getKey());
                }
            }
        }

        @Override
        public void collect(Field f, Object... a) {
            String k = f.alias;
            Map<Object, Long> cntc = counts.get(k);
            Set<Object      > cntx = countx.get(k);

            if (cntc == null) {
                cntc  = new HashMap();
                counts.put( k, cntc );
            }

            if (limits.contains( k )) {
                for ( Object v : a  ) {
                    if (cntx != null
                    &&  cntx.contains   (v)) {
                        continue;
                    }
                    if (cntc.containsKey(v)) {
                        cntc.put(v, 1L + cntc.get(v));
                    }
                }
            } else {
                for ( Object v : a  ) {
                    if (cntx != null
                    &&  cntx.contains   (v)) {
                        continue;
                    }
                    if (cntc.containsKey(v)) {
                        cntc.put(v, 1L + cntc.get(v));
                    } else {
                        cntc.put(v, 1L);
                    }
                }
            }
        }

    }

    /**
     * 数值计算处理器
     */
    public static class Amount implements Coller {

        private final Map<String, Map<Range, Ratio>> counts;
        private final Map<String, Set<Range       >> countx;

        public Amount(Map<String, Map<Range, Ratio>> counts,
                      Map<String, Set<Range       >> countx) {
            this.counts = counts;
            this.countx = countx;
        }

        @Override
        public void collect(Field f, Object... a) {
            String k = f.alias;
            Map<Range, Ratio> cntc = counts.get(k);
            Set<Range       > cntx = countx.get(k);

            if (cntc == null) {
                cntc  = new HashMap();
                counts.put( k, cntc );
            }

            F : for(Object o :  a  ) {
                    Number v = (Number) o;
                if (  null  != cntx)
                for(Range  w : cntx) {
                    if (w.covers(v)) {
                        continue F ;
                    }
                }

                for(Map.Entry<Range, Ratio> rr : cntc.entrySet()) {
                    Ratio w = rr.getValue();
                    Range m = rr.getKey  ();

                    if (m.covers(v)) {
                        w.add   (v);
                    }
                }
            }
        }

    }

    /**
     * 数字区间
     */
    public static class Range {
        public double min = Double.NEGATIVE_INFINITY;
        public double max = Double.POSITIVE_INFINITY;
        public boolean le = true;
        public boolean ge = true;

        public Range(double n) {
            min = max = n;
        }

        public Range(Number n) {
            min = max = n.doubleValue( );
        }

        public Range(String s) {
            Object[] a = Synt.toRange(s);
            if (a == null) {
                return;
            }
            if (a[0] != null) {
                min = Synt.declare(a[0], min);
            }
            if (a[1] != null) {
                max = Synt.declare(a[1], max);
            }
            le = (boolean) a[2];
            ge = (boolean) a[3];
        }

        @Override
        public String toString() {
            // 不限则为空
            if (le && min == Double.NEGATIVE_INFINITY
            &&  ge && max == Double.POSITIVE_INFINITY) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            sb.append(le ? "[" : "(");
            sb.append(min != Double.NEGATIVE_INFINITY ? Synt.asString(min) : "");
            sb.append(",");
            sb.append(max != Double.POSITIVE_INFINITY ? Synt.asString(max) : "");
            sb.append(ge ? "]" : ")");
            return sb.toString();
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (! (o instanceof Range)) {
                return false;
            }
            Range  m = (Range ) o;
            return m.le  == le  && m.ge  == ge
                && m.min == min && m.max == max;
        }

        public boolean covers(Number n) {
            double x = n.doubleValue( );
            return covers(x);
        }

        public boolean covers(double n) {
            if (le) { if (n >  max) {
                return false;
            }} else { if (n >= max) {
                return false;
            }}
            if (ge) { if (n <  min) {
                return false;
            }} else { if (n <= min) {
                return false;
            }}
            return true;
        }

        public boolean covers() {
            return le && ge
            && min == Double.NEGATIVE_INFINITY
            && max == Double.POSITIVE_INFINITY;
        }
    }

    /**
     * 计算结果
     */
    public static class Ratio {
        public int    cnt = 0;
        public double sum = 0;
        public double min = Double.NEGATIVE_INFINITY;
        public double max = Double.POSITIVE_INFINITY;

        public void add(Number v) {
            add (v.doubleValue());
        }

        public void add(double v) {
            cnt += 1;
            sum += v;
            if (min > v || min == Double.NEGATIVE_INFINITY) {
                min = v;
            }
            if (max < v || max == Double.POSITIVE_INFINITY) {
                max = v;
            }
        }
    }

}
