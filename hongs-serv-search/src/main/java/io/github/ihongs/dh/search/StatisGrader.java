package io.github.ihongs.dh.search;

import io.github.ihongs.dh.search.StatisHandle.Field;
import io.github.ihongs.util.Synt;
import java.io.IOException;
import java.util.Objects;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;

/**
 * 分类统计工具
 * @author Kevin
 */
public class StatisGrader {

    public static final int COUNT = 0;
    public static final int ASSET = 1;
    public static final int RANGE = 2; // Range Count
    public static final int TALLY = 3;
    public static final int TOTAL = 4;

    private final IndexSearcher finder;
    private       Field[]       fields;
    private       Query         query ;

    private final Map<String, Map> counts = new HashMap();
    private final Map<String, Set> countx = new HashMap();
    private final Map<String, Integer> styles = new HashMap();

    public StatisGrader (IndexSearcher finder) {
        this.finder = finder;
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
     * 限定字段
     * @param fields
     * @return
     */
    public StatisGrader field(Field... fields) {
        this.fields = fields;
        return this;
    }

    /**
     * 排除统计项
     * @param alias  字段别名
     * @param values 排除项
     * @return
     */
    public StatisGrader repel(String alias, Set values) {
        countx.put(alias, values);
        return this;
    }

    /**
     * 指定统计项
     * @param alias  字段别名
     * @param values 指定项
     * @return
     */
    public StatisGrader allow(String alias, Set values) {
        int style = 0;
        for (Object value : values) {
            if (value instanceof Range) {
                style = RANGE;
            } else {
                style = ASSET;
            }
            break;
        }
        return allow(alias, values, style);
    }

    /**
     * 指定统计项
     * @param alias  字段别名
     * @param values 指定项
     * @param style  TOTAL 求和, ASSET 限定...
     * @return
     */
    public StatisGrader allow(String alias, Set values, int style) {
        styles.put(alias, style);
        Map countz = new HashMap();
        switch (style) {
        case TOTAL:
            for (Object value : values) {
                countz.put(value, new Total((Range) value));
            } break;
        case TALLY:
            for (Object value : values) {
                countz.put(value, new Tally((Range) value));
            } break;
        default:
            for (Object value : values) {
                countz.put(value, new Count(value));
            } break;
        }
        counts.put(alias, countz);
        return this;
    }

    /**
     * 分类统计
     * @return
     * @throws IOException
     */
    public Map<String, Map> acount() throws IOException {
        if (query == null) {
            query =  new MatchAllDocsQuery();
        }
        finder.search(query, new Ferry(fields, counts, countx, styles));
        return counts;
    }

    /**
     * 采集器
     * 可以重写 collect 采集数据
     */
    public static class Fetch implements Collector, LeafCollector {

        protected final Field [] fields;
        private   int   count  =   0   ;

        public Fetch(Field... fields) {
            if (fields == null || fields.length == 0) {
                throw new NullPointerException("Fields required");
            }

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
    public static class Ferry extends Fetch {

        private final Map<String, Map> counts;
        private final Map<String, Set> countx;
        private final Map<String, Integer> styles;

        public Ferry (Field[] fields,
                      Map<String, Map> counts,
                      Map<String, Set> countx,
                      Map<String, Integer> styles) {
            super(fields);
            this.counts = counts;
            this.countx = countx;
            this.styles = styles;
        }

        @Override
        public void collect(int i) throws IOException {
            super.collect(i);

            for(Field  f :  fields) {
                String k = f.alias;
                Map cntc = counts.get(k);
                Set cntx = countx.get(k);

                if (cntc == null) {
                    cntc = new HashMap();
                    counts.put(k, cntc );
                }

                Iterable a = f.getValues( );

                switch (Synt.defxult(styles.get(k), 0)) {
                case TOTAL:   // 计数,求和,最小,最大
                case TALLY:   // 计数,0,最小,最大
                case RANGE: { // 计数
                    Map<Range, Coach> cntc2 = cntc;
                    Set<Range       > cntx2 = cntx;
                    F : for(Object o : a) {
                            Number v = (Number) o ;
                        if (  null  != cntx2) {
                        for(Range  w : cntx2) {
                            if (w.covers(v)) {
                                continue F ;
                            }
                        }}
                        for(Map.Entry<Range, Coach> rr : cntc2.entrySet()) {
                            Coach w = rr.getValue();
                            Range m = rr.getKey  ();
                            if (m.covers(v)) {
                                w.add   (v);
                            }
                        }
                    }
                } break;
                case ASSET: {
                    /**
                     * 限定只管指定的选项,
                     * 不去理会其他的取值.
                     */
                    for ( Object v : a  ) {
                        if (cntx != null
                        &&  cntx.contains   (v)) {
                            continue;
                        }
                        if (cntc.containsKey(v)) {
                          ((Count) cntc.get (v)).add();
                        }
                    }
                } break;
                default: {
                    for ( Object v : a  ) {
                        if (cntx != null
                        &&  cntx.contains   (v)) {
                            continue;
                        }
                        if (cntc.containsKey(v)) {
                          ((Count) cntc.get (v)).add();
                        } else {
                            Count c = new Count(v);
                                  c.add( );
                            cntc.put(v, c);
                        }
                    }
                }}
            }
        }

    }

    /**
     * 数字区间
     */
    public static class Range implements Comparable {
        public final double min;
        public final double max;
        public final boolean ge;
        public final boolean le;

        public Range(double n) {
            min  = n ;
            max  = n ;
            ge   = true;
            le   = true;
        }

        public Range(Number n) {
            double v = n.doubleValue();
            min  = v ;
            max  = v ;
            ge   = true;
            le   = true;
        }

        public Range(Object s) {
            Object[] a = Synt.toRange(s);
            if (a != null) {
                min = Synt.declare(a[0], Double.NEGATIVE_INFINITY);
                max = Synt.declare(a[1], Double.POSITIVE_INFINITY);
                ge  = (boolean) a[2];
                le  = (boolean) a[3];
            } else {
                min = Double.NEGATIVE_INFINITY;
                max = Double.POSITIVE_INFINITY;
                ge  = true;
                le  = true;
            }
        }

        public boolean covers(double n) {
            if (ge) { if (n <  min) {
                return false;
            }} else { if (n <= min) {
                return false;
            }}
            if (le) { if (n >  max) {
                return false;
            }} else { if (n >= max) {
                return false;
            }}
            return true;
        }

        public boolean covers(Number n) {
            double x = n.doubleValue( );
            return covers(x);
        }

        @Override
        public boolean equals(Object o) {
            if ( o instanceof Range) {
                Range that = (Range) o;

                return that.ge  == this.ge
                    && that.le  == this.le
                    && that.min == this.min
                    && that.max == this.max;
            }
            return false;
        }

        @Override
        public int compareTo (Object o) {
            if ( o instanceof Range) {
                Range that = (Range) o;

                if (this.min < that.min) {
                    return -1;
                }
                if (this.min > that.min) {
                    return  1;
                }

                if (this.ge && !that.ge) {
                    return -1;
                }
                if (that.ge && !this.ge) {
                    return  1;
                }

                if (this.max < that.max) {
                    return -1;
                }
                if (this.max > that.max) {
                    return  1;
                }

                if (that.le && !this.le) {
                    return -1;
                }
                if (this.le && !that.le) {
                    return  1;
                }
            }
            return  0;
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        @Override
        public String toString() {
            // 不限则为空
            if (ge && min == Double.NEGATIVE_INFINITY
            &&  le && max == Double.POSITIVE_INFINITY) {
                return "";
            }
            StringBuilder sb = new StringBuilder();
            sb.append(ge ? "[" : "(");
            sb.append(min != Double.NEGATIVE_INFINITY ? Synt.asString(min) : "");
            sb.append(",");
            sb.append(max != Double.POSITIVE_INFINITY ? Synt.asString(max) : "");
            sb.append(le ? "]" : ")");
            return sb.toString();
        }
    }

    /**
     * 计算结果
     */
    public static class Total extends ArrayList implements Comparable, Coach {
        public long   cnt = 0;
        public double sum = 0;
        public double min = Double.NEGATIVE_INFINITY;
        public double max = Double.POSITIVE_INFINITY;

        public Total(Range rng) {
            super(6);
            super.add(rng );
            super.add(null);
            super.add(cnt );
            super.add(sum );
            super.add(min );
            super.add(max );
        }

        @Override
        public void add() {
            cnt = 1 + cnt;
        }

        @Override
        public void add(Number v) {
            add(v.doubleValue( ));
        }

        @Override
        public void add(double v) {
            cnt = 1 + cnt;
            super.set(2, cnt);
            sum = v + sum;
            super.set(3, sum);
            if (min > v || min == Double.NEGATIVE_INFINITY) {
                min = v;
                super.set(4, min);
            }
            if (max < v || max == Double.POSITIVE_INFINITY) {
                max = v;
                super.set(5, max);
            }
        }

        @Override
        public Object set(int i, Object v) {
            switch ( i ) {
                case 2 :
                    cnt = Synt.asLong  (v);
                    return super.set(2, cnt);
                case 3 :
                    sum = Synt.asDouble(v);
                    return super.set(3, sum);
                case 4 :
                    min = Synt.asDouble(v);
                    return super.set(4, min);
                case 5 :
                    max = Synt.asDouble(v);
                    return super.set(5, max);
                default:
                    return super.set(i, v);
            }
        }

        @Override
        public String toString() {
            return cnt +","+ sum +","+ min +","+ max ;
        }

        @Override
        public int hashCode() {
            return Objects . hash(cnt, sum, min, max);
        }

        @Override
        public boolean equals(Object o) {
            if (! (o instanceof Total)) {
                return false;
            }
            Total  m = (Total ) o;
            return m.cnt == cnt && m.sum == sum
                && m.min == min && m.max == max;
        }

        @Override
        public int compareTo(Object o) {
            if (o instanceof Total) {
                Total that =(Total) o;

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

    /**
     * 计算结果
     */
    public static class Tally extends ArrayList implements Comparable, Coach {
        public long   cnt = 0;
        public double sum = 0;
        public double min = Double.NEGATIVE_INFINITY;
        public double max = Double.POSITIVE_INFINITY;

        public Tally(Range rng) {
            super(6);
            super.add(rng );
            super.add(null);
            super.add(cnt );
            super.add(sum );
            super.add(min );
            super.add(max );
        }

        @Override
        public void add() {
            cnt = 1 + cnt;
        }

        @Override
        public void add(Number v) {
            add(v.doubleValue( ));
        }

        @Override
        public void add(double v) {
            cnt = 1 + cnt;
            super.set(2, cnt);
            if (min > v || min == Double.NEGATIVE_INFINITY) {
                min = v;
                super.set(4, min);
            }
            if (max < v || max == Double.POSITIVE_INFINITY) {
                max = v;
                super.set(5, max);
            }
        }

        @Override
        public Object set(int i, Object v) {
            switch ( i ) {
                case 2 :
                    cnt = Synt.asLong  (v);
                    return super.set(2, cnt);
                case 3 :
                    sum = Synt.asDouble(v);
                    return super.set(3, sum);
                case 4 :
                    min = Synt.asDouble(v);
                    return super.set(4, min);
                case 5 :
                    max = Synt.asDouble(v);
                    return super.set(5, max);
                default:
                    return super.set(i, v);
            }
        }

        @Override
        public String toString() {
            return cnt +","+ sum +","+ min +","+ max ;
        }

        @Override
        public int hashCode() {
            return Objects . hash(cnt, sum, min, max);
        }

        @Override
        public boolean equals(Object o) {
            if (! (o instanceof Total)) {
                return false;
            }
            Total  m = (Total ) o;
            return m.cnt == cnt && m.sum == sum
                && m.min == min && m.max == max;
        }

        @Override
        public int compareTo(Object o) {
            if (o instanceof Total) {
                Total that =(Total) o;

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

    public static class Count extends ArrayList implements Comparable, Coach {
        public long cnt = 0L;

        public Count(Object val) {
            super(3);
            super.add(val );
            super.add(null);
            super.add(cnt );
        }

        @Override
        public void add() {
            cnt = 1 + cnt;
            super.set(2, cnt);
        }

        @Override
        public void add(Number v) {
            add ( );
        }

        @Override
        public void add(double v) {
            add ( );
        }

        @Override
        public Object set(int i, Object v) {
            switch ( i ) {
                case 2 :
                    cnt = Synt.asLong  (v);
                    return super.set(2, cnt);
                default:
                    return super.set(i, v);
            }
        }

        @Override
        public String toString() {
            return cnt + "";
        }

        @Override
        public int hashCode() {
            return Objects . hash (cnt);
        }

        @Override
        public boolean equals(Object o) {
            if (! (o instanceof Total)) {
                return false;
            }
            Count  m = (Count ) o;
            return m.cnt == cnt;
        }

        @Override
        public int compareTo(Object o) {
            if (o instanceof Count) {
                Count that =(Count) o;
                if (this.cnt < that.cnt) {
                    return -1;
                }
                if (this.cnt > that.cnt) {
                    return  1;
                }
            }
            return 0;
        }
    }

    public static abstract interface Coach {

        public void add();
        public void add(Number v);
        public void add(double v);
        public Object get (int i);

    }

}
