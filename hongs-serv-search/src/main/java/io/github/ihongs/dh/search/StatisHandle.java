package io.github.ihongs.dh.search;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.dh.lucene.LuceneRecord;
import io.github.ihongs.util.Synt;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashSet;
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
import org.apache.lucene.search.LeafCollector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.util.NumericUtils;

/**
 * 统计方法
 *
 * @author Kevin
 */
public class StatisHandle {

    private final LuceneRecord that;

    public StatisHandle(LuceneRecord that) {
        this.that = that;
    }

    public final LuceneRecord getRecord( ) {
        return that;
    }

    public List<Map> search(Map rd)
    throws HongsException {
        return  search  (rd,
            Synt.toTerms(rd.get(Cnst.RB_KEY)),
            Synt.toTerms(rd.get(Cnst.OB_KEY))
        );
    }

    public List<Map> search(Map rd, Set<String> rb, Set<String> ob)
    throws HongsException {
        List<     Map> list = new LinkedList();
        Map <Dim, Map> maps = new   HashMap ();

        Map<String, Map> fs = that.getFields();
        Set<String> rl = new LinkedHashSet();
        for(String  fn : rb) {
            int p = fn.indexOf('|');
            if (p > -1) {
                fn = fn.substring(0, p);
            }
            rl.add(fn);
        }
        
        search( rd , (Object[] values) -> {
            // Todo: 聚合维度，计算指标
        }, rl.toArray(new String[rl.size()]));

        return null;
    }

    public void search(Map rd, Coller coller, String... fields)
    throws HongsException {
        Query  q = that.padQry(rd);
        Collec c = new Collec(coller, fields, that);

        if (0 < Core.DEBUG && 8 != (8 & Core.DEBUG)) {
            CoreLogger.debug("StatisHandler.search: "+ q.toString());
        }

        try {
            that.getFinder( ).search(q,c);
        }
        catch (IOException e) {
            throw new HongsException( e );
        }
    }

    public static class Dim {
        public Dim(Object... values) {
            
        }
        
        @Override
        public int hashCode() {
            return 1;
        }
        
        @Override
        public boolean equals(Object that) {
            return false;
        }
    }
    
    public static interface Coller {
        public void collect(Object[] values);
    }

    public static class Collec implements Collector, LeafCollector {

        protected final Coller    coller;
        protected final String[ ] fields;
        protected final Object[ ] values;
        protected final Object[ ] valuez;
        protected final int[ ][ ] groups; // 类型

        public Collec(Coller coller, String[] fields, LuceneRecord record) {
            this.coller = coller;
            this.fields = fields;
            this.values = new Object[this.fields.length];
            this.valuez = new Object[this.fields.length];
            this.groups = new int[this.fields.length][2];

                int  i  = -1;
                Map fs  = record.getFields();
            for(String fn : fields) { i ++  ;
                Map fc  = (Map) fs.get( fn );
                if (fc == null) {
                    continue;
                }

                String  t = (String) fc.get("__type__");
                if ( t == null) {
                    continue;
                }

                if (Synt.declare(fc.get("__repeated__"), false)) {
                    groups[i][1] = 1;
                } else {
                    groups[i][1] = 0;
                }

                switch (t) {
                    case "string":
                    case "search":
                    case "sorted":
                    case "stored":
                    case "object":
                        groups[i][0] = 0;
                        continue ;
                }

                // 基准类型
                try {
                    String k  = (String) FormSet
                          .getInstance ( /***/ )
                          .getEnum ("__types__")
                          .get (t);
                    if (null != k) {
                           t  = k;
                    }
                } catch (HongsException e) {
                    throw e.toExemption( );
                }

                Object  k = fc.get("type");
                switch (t) {
                    case "number":
                        if (   "int".equals(k)) {
                            groups[i][0] = 1;
                        } else
                        if (  "long".equals(k)) {
                            groups[i][0] = 2;
                        } else
                        if ("float".equals(k)) {
                            groups[i][0] = 3;
                        } else
                        {
                            groups[i][0] = 4;
                        }
                        break ;
                    case "hidden":
                    case  "enum" :
                        if (   "int".equals(k)) {
                            groups[i][0] = 1;
                        } else
                        if (  "long".equals(k)) {
                            groups[i][0] = 2;
                        } else
                        if ( "float".equals(k)) {
                            groups[i][0] = 3;
                        } else
                        if ("double".equals(k)
                        ||  "number".equals(k)) {
                            groups[i][0] = 4;
                        }
                        break ;
                    case  "date" :
                        groups[i][0] = 2;
                        break ;
                    default:
                        groups[i][0] = 0;
                }
            }
        }

        @Override
        public LeafCollector getLeafCollector(LeafReaderContext lrc)
        throws IOException {
            LeafReader reader = lrc.reader( );

            for (int i = 0; i < fields.length; i ++) {
                if (groups[i][0] >= 1) {
                if (groups[i][1] == 1) {
                    values[i] = reader.getSortedNumericDocValues("%"+fields[i]);
                } else {
                    values[i] = reader.      getNumericDocValues("#"+fields[i]);
                }
                } else {
                if (groups[i][1] == 1) {
                    values[i] = reader.getSortedSetDocValues("%"+fields[i]);
                } else {
                    values[i] = reader.   getSortedDocValues("#"+fields[i]);
                }
                }
            }

            return this;
        }

        @Override
        public void collect(int doc)
        throws IOException {
            for (int i = 0; i < fields.length; i ++) {
                valuez[i] = null;
                
                String n = fields[i];
                Object d = values[i];
                if (d == null) {
                    continue ;
                }

                if (groups[i][0] == 0 && groups[i][1] == 0) {
                    SortedDocValues b = (SortedDocValues) d;
                    if (!b.advanceExact(doc)) {
                        continue;
                    }

                    String[] v = new String[1];
                    v[0] = b.binaryValue().utf8ToString();
                    valuez[i] = v;
                } else
                if (groups[i][0] == 0 && groups[i][1] == 1) {
                    SortedSetDocValues b = (SortedSetDocValues) d;
                    if (!b.advanceExact(doc)) {
                        continue;
                    }

                    List<String> u;
                    String[]     v;
                    long         j;
                    u = new LinkedList();
                    while ((j = b.nextOrd()) != SortedSetDocValues.NO_MORE_ORDS) {
                        u.add(b.lookupOrd(j).utf8ToString() );
                    }
                    v = u.toArray ( new String [ u.size() ] );
                    valuez[i] = v;
                } else
                if (groups[i][0] >= 1 && groups[i][1] == 0) {
                    NumericDocValues b = (NumericDocValues) d;
                    if (!b.advanceExact(doc)) {
                        continue;
                    }

                    double[] v = new double[1];
                    if (groups[i][0] == 4) {
                        v[0] = NumericUtils.sortableLongToDouble((long) b.longValue());
                    }  else
                    if (groups[i][0] == 3) {
                        v[0] = NumericUtils. sortableIntToFloat (( int) b.longValue());
                    }  else
                    {
                        v[0] = (double) b.longValue();
                    }
                    values[i] = v;
                } else
                if (groups[i][0] >= 1 && groups[i][1] == 1) {
                    SortedNumericDocValues b = (SortedNumericDocValues) d;
                    if (!b.advanceExact(doc)) {
                        continue;
                    }

                    double[] v = new double[(int) b.docValueCount()];
                    if (groups[i][0] == 4) {
                    for( int j = 0; j < v.length; j ++ ) {
                        v[j] = NumericUtils.sortableLongToDouble((long) b.nextValue());
                    }} else
                    if (groups[i][0] == 3) {
                    for( int j = 0; j < v.length; j ++ ) {
                        v[j] = NumericUtils. sortableIntToFloat (( int) b.nextValue());
                    }} else
                    {
                    for( int j = 0; j < v.length; j ++ ) {
                        v[j] = (double) b.nextValue();
                    }}
                    values[i] = v;
                }
            }
            coller.collect(valuez);
        }

        @Override
        public void setScorer(Scorer sco)
        throws IOException {
        }

        @Override
        public boolean needsScores()
        {
            return false;
        }

    }

}
