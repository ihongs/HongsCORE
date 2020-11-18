package io.github.ihongs.dh.search;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.SortedNumericDocValues;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.NumericUtils;

public class StatisHandle {

    private final IndexSearcher finder;
    private       Query   query ;
    private       Field[] fields;
    private       Score[] scores;

    public StatisHandle (IndexSearcher finder) {
        this.finder = finder;
    }

    public StatisHandle query(Query query) {
        this.query = query;
        return this;
    }

    public StatisHandle group(Field... field) {
        this.fields = field;
        return this;
    }

    public StatisHandle score(Score... score) {
        this.scores = score;
        return this;
    }

    public List<Map> fetch() {
        return null;
    }

    public static abstract class Value<T> {

        protected static final byte INT    = 1;
        protected static final byte LONG   = 2;
        protected static final byte FLOAT  = 3;
        protected static final byte DOUBLE = 4;
        protected static final byte STRING = 0;

        protected final byte    type  ;
        protected final boolean many  ;
        protected final String  name  ;
        protected       Object  value ;
        private         Object  values;

        public Value(String name, boolean many) {
            this.name = name;
            this.many = many;

            Class<T> t = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
            if (t.isAssignableFrom(String.class)) {
                type = STRING;
            } else
            if (t.isAssignableFrom(Double.class)) {
                type = DOUBLE;
            } else
            if (t.isAssignableFrom(Float.class)) {
                type = FLOAT ;
            } else
            if (t.isAssignableFrom(Long.class)) {
                type = LONG  ;
            } else
            if (t.isAssignableFrom(Integer.class)) {
                type = INT   ;
            } else
            {
                throw new UnsupportedOperationException("Unsupported type " + t.getName());
            }
        }

        public void collect(LeafReader r) throws IOException {
            if (!many) {
                switch (type) {
                    case STRING:
                        values = r.getSortedDocValues (name);
                        break;
                    default:
                        values = r.getNumericDocValues(name);
                        break;
                }
            } else {
                switch (type) {
                    case STRING:
                        values = r.getSortedSetDocValues(name);
                        break;
                    default:
                        values = r.getSortedNumericDocValues(name);
                        break;
                }
            }
        }

        public void collect(int i) throws IOException {
            if (!many) {
                switch (type) {
                    case INT: {
                        NumericDocValues numValues = ((NumericDocValues) values);
                        if (! numValues.advanceExact(i)) {
                            value = null;
                            break;
                        }
                        value = (int ) numValues.longValue();
                        break;
                    }
                    case LONG: {
                        NumericDocValues numValues = ((NumericDocValues) values);
                        if (! numValues.advanceExact(i)) {
                            value = null;
                            break;
                        }
                        value = (long) numValues.longValue();
                        break;
                    }
                    case FLOAT: {
                        NumericDocValues numValues = ((NumericDocValues) values);
                        if (! numValues.advanceExact(i)) {
                            value = null;
                            break;
                        }
                        value = NumericUtils. sortableIntToFloat ((int ) numValues.longValue());
                        break;
                    }
                    case DOUBLE:{
                        NumericDocValues numValues = ((NumericDocValues) values);
                        if (! numValues.advanceExact(i)) {
                            value = null;
                            break;
                        }
                        value = NumericUtils.sortableLongToDouble((long) numValues.longValue());
                        break;
                    }
                    default: {
                        SortedDocValues strValues = ((SortedDocValues) values);
                        if (! strValues.advanceExact(i)) {
                            value = null;
                            break;
                        }
                        value = strValues.binaryValue().utf8ToString();
                        break;
                    }
                }
            } else {
                switch (type) {
                    case INT: {
                        SortedNumericDocValues numValues = ((SortedNumericDocValues) values);
                        if (! numValues.advanceExact(i)) {
                            value = null;
                            break;
                        }
                        int[] a = new int[numValues.docValueCount()];
                        for (int j = 0; j < a.length; j ++) {
                            a[j] = (int) numValues.nextValue();
                        }
                        value = a;
                        break;
                    }
                    case LONG: {
                        SortedNumericDocValues numValues = ((SortedNumericDocValues) values);
                        if (! numValues.advanceExact(i)) {
                            value = null;
                            break;
                        }
                        long[] a = new long[numValues.docValueCount()];
                        for (int j = 0; j < a.length; j ++) {
                            a[j] = (long) numValues.nextValue();
                        }
                        value = a;
                        break;
                    }
                    case FLOAT: {
                        SortedNumericDocValues numValues = ((SortedNumericDocValues) values);
                        if (! numValues.advanceExact(i)) {
                            value = null;
                            break;
                        }
                        float[] a = new float[numValues.docValueCount()];
                        for (int j = 0; j < a.length; j ++) {
                            a[j] = NumericUtils. sortableIntToFloat ((int ) numValues.nextValue());
                        }
                        value = a;
                        break;
                    }
                    case DOUBLE: {
                        SortedNumericDocValues numValues = ((SortedNumericDocValues) values);
                        if (! numValues.advanceExact(i)) {
                            value = null;
                            break;
                        }
                        double[] a = new double[numValues.docValueCount()];
                        for (int j = 0; j < a.length; j ++) {
                            a[j] = NumericUtils.sortableLongToDouble((long) numValues.nextValue());
                        }
                        value = a;
                        break;
                    }
                    default: {
                        SortedSetDocValues strValues = ((SortedSetDocValues) values);
                        if (! strValues.advanceExact(i)) {
                            value = null;
                            break;
                        }
                        List <String> l = new LinkedList();
                        long    k ;
                        while ((k = strValues.nextOrd()) != SortedSetDocValues.NO_MORE_ORDS) {
                            l.add(strValues.lookupOrd(k).utf8ToString());
                        }
                        value = l.toArray(new String[l.size()]);
                        break;
                    }
                }
            }
        }

        public Object getValue() {
            return value;
        }

    }

    public static class Group {
        
        private final Field[] fields;
        
        public Group(Field... field) {
            this.fields = field;
        }
        
        
        
    }
    
    public static abstract class Score<T> extends Value<T> {

        public Score(String name, boolean many) {
            super(name, many);
        }

    }

    public static class Field<T> extends Value<T> {

        public Field(String name, boolean many) {
            super(name, many);
        }

    }

    public static class Count<T>  extends Score<T>  {

        public Count(String name, boolean many) {
            super(name, many);
        }
        
    }

    public static class Sum<T>  extends Score<T>  {

        public Sum(String name, boolean many) {
            super(name, many);

            if (type == STRING) {
                throw new UnsupportedOperationException("Unsupported type String for Sum");
            }
        }

    }

    public static class Max<T>  extends Score<T>  {

        public Max(String name, boolean many) {
            super(name, many);

            if (type == STRING) {
                throw new UnsupportedOperationException("Unsupported type String for Max");
            }
        }

    }

    public static class Min<T>  extends Score<T>  {

        public Min(String name, boolean many) {
            super(name, many);

            if (type == STRING) {
                throw new UnsupportedOperationException("Unsupported type String for Min");
            }
        }

    }

}