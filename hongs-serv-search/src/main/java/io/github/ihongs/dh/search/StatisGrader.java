package io.github.ihongs.dh.search;

import io.github.ihongs.dh.search.StatisHandle.Field;
import io.github.ihongs.util.Synt;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;

/**
 * 分类统计工具
 * @author Kevin
 */
public class StatisGrader {

    private final IndexSearcher finder;
    private       Field[]       fields;
    private       Query         query ;

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
        finder.search (query, new Acount(fields, counts, countx));
        return counts ;
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
        finder.search (query, new Amount(fields, counts, countx));
        return counts ;
    }

    /**
     * 采集器
     * 可以重写 collect 采集数据
     */
    public static class Fetch implements Collector, LeafCollector {

        protected final Field [] fields;
        private   int   count  =   0   ;

        public Fetch(Field... fields) {
            this.fields = fields;
        }

        @Override
        public LeafCollector getLeafCollector(LeafReaderContext c) throws IOException {
            LeafReader r = c.reader();
            for(Field field : fields) {
                field.prepare(r);
            }
            return this;
        }

        @Override
        public void collect(int i) throws IOException {
            for(Field field : fields) {
                field.collect(i);
            }
            count ++ ;
        }

        @Override
        public void setScorer(Scorer s) {
            // 不需要打分
        }

        @Override
        public boolean needsScores( ) {
            return false;
        }

        public int count() {
            return count;
        }

    }

    /**
     * 分类统计处理器
     */
    public static class Acount extends Fetch {

        private final Map<String, Map<Object, Long>> counts;
        private final Map<String, Set<Object      >> countx;
        private final Set<String                   > limits;

        public Acount(Field[] fields,
                      Map<String, Map<Object, Long>> counts,
                      Map<String, Set<Object      >> countx) {
            super(fields);
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
        public void collect(int i) throws IOException {
            super.collect(i);

            for(Field  f :  fields) {
                String k = f.alias;
                Map<Object, Long> cntc = counts.get(k);
                Set<Object      > cntx = countx.get(k);

                if (cntc == null) {
                    cntc  = new HashMap();
                    counts.put( k, cntc );
                }

                Iterable a = f.getValues( );

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

    }

    /**
     * 数值计算处理器
     */
    public static class Amount extends Fetch {

        private final Map<String, Map<Range, Ratio>> counts;
        private final Map<String, Set<Range       >> countx;

        public Amount(Field[] fields,
                      Map<String, Map<Range, Ratio>> counts,
                      Map<String, Set<Range       >> countx) {
            super(fields);
            this.counts = counts;
            this.countx = countx;
        }

        @Override
        public void collect(int i) throws IOException {
            super.collect(i);

            for(Field  f :  fields) {
                String k = f.alias;
                Map<Range, Ratio> cntc = counts.get(k);
                Set<Range       > cntx = countx.get(k);

                if (cntc == null) {
                    cntc  = new HashMap();
                    counts.put( k, cntc );
                }

                Iterable a = f.getValues( );

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

    }

    /**
     * 数字区间
     */
    public static class Range implements Comparable {
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

        public Range(Object s) {
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
            return Double.valueOf(min + max).hashCode();
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

        @Override
        public int compareTo(Object o) {
            if (o instanceof Range) {
                Range that =(Range) o;

                if (this.min < that.min) {
                    return -1;
                }
                if (this.min > that.min) {
                    return  1;
                }

                if (this.le && !that.le) {
                    return -1;
                }
                if (that.le && !this.le) {
                    return  1;
                }

                if (this.max < that.max) {
                    return -1;
                }
                if (this.max > that.max) {
                    return  1;
                }

                if (!this.ge && that.ge) {
                    return -1;
                }
                if (!that.ge && this.ge) {
                    return  1;
                }
            }
            return  0;
        }
    }

    /**
     * 计算结果
     */
    public static class Ratio implements Comparable {
        public long   cnt = 0;
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

        @Override
        public String toString() {
            return cnt+ "," +sum+ "," + min + "," + max;
        }

        @Override
        public int hashCode() {
            return Double.valueOf(sum + cnt).hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (! (o instanceof Ratio)) {
                return false;
            }
            Ratio  m = (Ratio ) o;
            return m.cnt == cnt && m.sum == sum
                && m.min == min && m.max == max;
        }

        @Override
        public int compareTo(Object o) {
            if (o instanceof Ratio) {
                Ratio that =(Ratio) o;

                if (this.cnt < that.cnt) {
                    return -1;
                }
                if (this.cnt > that.cnt) {
                    return  1;
                }

                if (this.sum < that.sum) {
                    return -1;
                }
                if (this.sum > that.sum) {
                    return  1;
                }

                if (this.min < that.min) {
                    return -1;
                }
                if (this.min > that.min) {
                    return  1;
                }

                if (this.max < that.max) {
                    return -1;
                }
                if (this.max > that.max) {
                    return  1;
                }
            }
            return 0;
        }
    }

}
