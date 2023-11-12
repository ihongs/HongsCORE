package io.github.ihongs.dh.search;

import io.github.ihongs.CruxExemption;
import io.github.ihongs.util.Synt;
import java.io.IOException;
import java.util.Iterator;
import java.util.function.Consumer;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.util.NumericUtils;

/**
 * 统计辅助工具
 * @author Hongs
 */
public final class StatisHandle {

    /**
     * 字段取值类型
     */
    public static enum TYPE {
        INT , LONG , FLOAT , DOUBLE , STRING , // 单数
        INTS, LONGS, FLOATS, DOUBLES, STRINGS  // 复数
    };

    public static final int  NO_MORE_VALS = (int ) SortedSetDocValues.NO_MORE_ORDS;
    public static final long NO_MORE_ORDS = (long) SortedSetDocValues.NO_MORE_ORDS;

    private final IndexSearcher finder;
    private       Field[]       fields;
    private       Query         query ;

    public StatisHandle (IndexSearcher finder) {
        this.finder = finder;
    }

    /**
     * 限定字段
     * @param fields
     * @return
     */
    public StatisHandle field(Field... fields) {
        this.fields = fields;
        return this;
    }

    /**
     * 限定条件
     * @param query
     * @return
     */
    public StatisHandle where(Query    query ) {
        this.query  = query ;
        return this;
    }

    /**
     * 查询数据
     * @param fx
     * @throws IOException
     */
    public void search (Consumer<Field[]> fx)
    throws IOException {
        if (query == null) {
            query =  new MatchAllDocsQuery( );
        }

        finder. search (query , new Fetch(fx, fields));
    }

    /**
     * 采集器
     * 可以重写 collect 采集数据
     */
    public static class Fetch implements Collector, LeafCollector {

        private final Consumer<Field[]> filter;
        private final /*Base*/ Field[]  fields;

        public Fetch( Consumer<Field[]> filter, Field... fields ) {
            if (filter == null) {
                throw new NullPointerException("Filter required");
            }
            if (fields == null || fields.length == 0) {
                throw new NullPointerException("Fields required");
            }

            this.filter = filter;
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
            filter.accept(fields);
        }

        @Override
        public void setScorer(Scorer s) {
            // 不需要打分
        }

        @Override
        public boolean needsScores( ) {
            return false;
        }

    }

    /**
     * 字段
     */
    public static class Field {

        public final TYPE   type ;
        public final String filed;
        public final String alias;
        public final Values values;

        public Field(TYPE type, String field, String alias) {
            this.type  = type ;
            this.filed = field;
            this.alias = alias;

            switch (type) {
                case INT:
                case LONG:
                case FLOAT:
                case DOUBLE:
                    values = new NumberValues(field, type);
                    break;
                case STRING:
                    values = new StringValues(field);
                    break;
                case INTS:
                case LONGS:
                case FLOATS:
                case DOUBLES:
                    values = new NumberSetValues(field, type);
                    break;
                case STRINGS:
                    values = new StringSetValues(field);
                    break;
                default:
                    throw new UnsupportedOperationException("Unsupported field type");
            }
        }

        public Object getDocValues() {
            return values.getDocValues( );
        }

        public Valuer getValues() {
            return values;
        }

        public void prepare(LeafReader r) throws IOException {
            values.prepare(r);
        }

        public void collect(    int    i) throws IOException {
            values.collect(i);
        }

    }

    /**
     * 值迭代
     * @param <T>
     */
    public static interface Valuer<T> extends Iterator<T>, Iterable<T> {

        @Override
        public default Iterator<T> iterator() {
            return this;
        }

    }

    /**
     * 字段值
     * @param <T>
     */
    abstract public static class Values<T> implements Valuer<T> {

      protected final String field;

        public Values(String field) {
            this.field = field;
        }

        public abstract Object getDocValues();

        public abstract void prepare(LeafReader r) throws IOException;

        public abstract void collect(    int    i) throws IOException;

    }

    /**
     * 单一数字类型值
     */
    public static class NumberValues extends Values<Number> {

        protected final TYPE type;
        protected int   j  = NO_MORE_VALS;
        protected NumericDocValues values = null;

        public NumberValues(String field, TYPE type) {
            super(field);
            this.type = type;
        }

        @Override
        public Object getDocValues() {
            return values;
        }

        @Override
        public void prepare(LeafReader r) throws IOException {
            values = r.getNumericDocValues(field);
        }

        @Override
        public void collect(int i) throws IOException {
            if (values == null
            ||  values.advanceExact(i) == false) {
                j = NO_MORE_VALS;
            } else {
                j = 0;
            }
        }

        @Override
        public boolean hasNext() {
            return j != NO_MORE_VALS;
        }

        @Override
        public  Number next() {
            try {  j  = NO_MORE_VALS;
                switch (type) {
                    case DOUBLE:
                        return NumericUtils.sortableLongToDouble(/***/ values.longValue());
                    case FLOAT :
                        return NumericUtils. sortableIntToFloat ((int) values.longValue());
                    case INT:
                        return (int) values.longValue();
                    default :
                        return /***/ values.longValue();
                }
            } catch (IOException e ) {
                throw new CruxExemption(e);
            }
        }

    }

    /**
     * 数字集合类型值
     */
    public static class NumberSetValues extends Values<Number> {

        protected final TYPE type;
        protected int   j  = NO_MORE_VALS;
        protected SortedNumericDocValues values = null;

        public NumberSetValues(String field, TYPE type) {
            super(field);
            this.type = type;
        }

        @Override
        public Object getDocValues() {
            return values;
        }

        @Override
        public void prepare(LeafReader r) throws IOException {
            values = r.getSortedNumericDocValues(field);
        }

        @Override
        public void collect(int i) throws IOException {
            if (values == null
            ||  values.advanceExact(i) == false) {
                j = NO_MORE_VALS;
            } else {
                j = 0;
            }
        }

        @Override
        public boolean hasNext() {
            return j != NO_MORE_VALS && j < values.docValueCount();
        }

        @Override
        public  Number next() {
            try {  j ++ ;
                switch (type) {
                    case DOUBLE:
                        return NumericUtils.sortableLongToDouble(/***/ values.nextValue());
                    case FLOAT :
                        return NumericUtils. sortableIntToFloat ((int) values.nextValue());
                    case INT:
                        return (int) values.nextValue();
                    default :
                        return /***/ values.nextValue();
                }
            } catch (IOException e ) {
                throw new CruxExemption(e);
            }
        }

    }

    /**
     * 单一字串类型值
     */
    public static class StringValues extends Values<String> {

        private int j = NO_MORE_VALS;
        private SortedDocValues values = null;

        public StringValues(String field) {
            super(field);
        }

        @Override
        public Object getDocValues() {
            return values;
        }

        @Override
        public void prepare(LeafReader r) throws IOException {
            values = r.getSortedDocValues(field);
        }

        @Override
        public void collect(int i) throws IOException {
            if (values == null
            ||  values.advanceExact(i) == false) {
                j = NO_MORE_VALS;
            } else {
                j = 0;
            }
        }

        @Override
        public boolean hasNext() {
            return j != NO_MORE_VALS;
        }

        @Override
        public  String next() {
            try {  j  = NO_MORE_VALS;
                return values.binaryValue().utf8ToString();
            } catch (IOException e ) {
                throw new CruxExemption(e);
            }
        }

    }

    /**
     * 字串集合类型值
     */
    public static class StringSetValues extends Values<String> {

        private long j = NO_MORE_ORDS;
        private SortedSetDocValues values = null;

        public StringSetValues(String field) {
            super(field);
        }

        @Override
        public Object getDocValues() {
            return values;
        }

        @Override
        public void prepare(LeafReader r) throws IOException {
            values = r.getSortedSetDocValues(field);
        }

        @Override
        public void collect(int i) throws IOException  {
            if (values == null
            ||  values.advanceExact(i) == false) {
                j = NO_MORE_ORDS;
            } else {
                j = values.nextOrd();
            }
        }

        @Override
        public boolean hasNext() {
            return j != NO_MORE_ORDS;
        }

        @Override
        public  String next() {
            try {
                String s;
                s = values.lookupOrd(j).utf8ToString();
                j = values.nextOrd();
                return s;
            } catch (IOException e ) {
                throw new CruxExemption(e);
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

}
