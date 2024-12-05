package io.github.ihongs.dh.lucene.conn;

import io.github.ihongs.CruxExemption;
import io.github.ihongs.util.Syno;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.StoredFields;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHits;
import org.apache.lucene.store.AlreadyClosedException;

/**
 * 查询读取
 * @author Hongs
 */
public class Loop implements Iterable<Document>, Iterator<Document> {
    private final IndexSearcher schr;
    private final StoredFields  fids;
    private       ScoreDoc[]    docs;
    private       ScoreDoc      doc ;
    private final Query   q;
    private final Sort    s;
    private final Set     r;
    private final int     b; // 起始位置
    private final int     l; // 数量限制
    private final boolean t; // 有限查询
    private       int     i; // 提取游标
    private       int     h; // 单次总数
    private       long    H; // 全局总数

    /**
     * 查询迭代器
     * @param p 搜索对象
     * @param q 查询对象
     * @param s 排序对象
     * @param r 返回字段
     * @param b 起始偏移
     * @param l 查询限额
     */
    public Loop(IndexSearcher p, Query q, Sort s, Set r, int b, int l) {
        // 空取全部字段
        if (r!= null && r.isEmpty()) {
            r = null ;
        }

        // 是否获取全部
        if (l == 0 ) {
            l = 65536;
            t = false;
        } else {
            t = true ;
        }

        this.q = q;
        this.s = s;
        this.r = r;
        this.b = b;
        this.l = l;

        try {
            schr = p;
            fids = schr.storedFields();
        } catch (IOException e) {
            throw new CruxExemption(e);
        }
    }

    @Override
    public Iterator<Document> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        try {
            if ( docs == null) {
                int L  = l+b ;
                 TopDocs tops;
               TotalHits tots;
                if (s != null) {
                    tops = schr.searchAfter(doc, q, L, s);
                } else {
                    tops = schr.searchAfter(doc, q, L);
                }
                tots = tops.totalHits;
                H    = tots.value ;
                docs = tops.scoreDocs;
                h    = docs.length;
                i    = b;
            } else
            if (! t && i >= h) {
                 TopDocs tops;
                if (s != null) {
                    tops = schr.searchAfter(doc, q, l, s);
                } else {
                    tops = schr.searchAfter(doc, q, l);
                }
                docs = tops.scoreDocs;
                h    = docs.length;
                i    = 0;
            }
            return i < h;
        } catch (IOException|AlreadyClosedException e) {
            throw new CruxExemption( e );
        }
    }

    @Override
    public Document next() {
        if ( i >= h ) {
            throw new NullPointerException("hasNext not run?");
        }
        try {
            doc =  docs[i ++ ];
            return fids.document(doc.doc, r);
        } catch (IOException|AlreadyClosedException e) {
            throw new CruxExemption( e );
        }
    }

    /**
     * 获取单次数量
     * @return
     */
    public int  size() {
        if (docs == null) {
            hasNext ();
        }
        int L;
        if (t) {
            L  = (int) (h - b);
        } else {
            L  = (int) (H - b);
        }
        return L > 0 ?  L : 0 ;
    }

    /**
     * 获取命中总数
     * @return
     */
    public int  hits() {
        if (docs == null) {
            hasNext ();
        }
        // 最多 2G
        return H < Integer.MAX_VALUE
        ?(int) H : Integer.MAX_VALUE;
    }

    /**
     * 真实命中总数
     * @return
     */
    public long tots() {
        if (docs == null) {
            hasNext ();
        }
        return H ;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (q != null) {
            sb.append("QUERY: ");
            sb.append( q );
            sb.append(" ");
        }
        if (s != null) {
            sb.append( "SORT: ");
            sb.append( s );
            sb.append(" ");
        }
        if (r != null && ! r.isEmpty( ) ) {
            sb.append("REPLY: ");
            sb.append(Syno.concat(",",r ) );
            sb.append(" ");
        }
        if (l != 0 || b != 0) {
            sb.append("LIMIT: ");
            sb.append(Syno.concat(",",b,l));
            sb.append(" ");
        }
        if (sb.length( ) > 0) {
            sb.setLength( sb.length() - 1 );
        }
        return sb.toString();
    }

    @Override
    @Deprecated
    public void remove() {
        throw new UnsupportedOperationException("Not supported remove in search loop.");
    }
}
