package io.github.ihongs.dh.lucene;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.HongsException;
import io.github.ihongs.HongsExemption;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.dh.IEntity;
import io.github.ihongs.dh.ITrnsct;
import io.github.ihongs.dh.ModelCase;
import io.github.ihongs.dh.lucene.field.*;
import io.github.ihongs.dh.lucene.value.*;
import io.github.ihongs.dh.lucene.query.*;
import io.github.ihongs.util.Dawn;
import io.github.ihongs.util.Dict;
import io.github.ihongs.util.Syno;
import io.github.ihongs.util.Synt;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.document.Document;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.Directory;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;

/**
 * Lucene 记录模型
 *
 * 可选字段配置参数:
 *  lucene-type         Lucene 字段类型(string,search,stored,sorted,int,long,float,double)
 *  lucene-tokenizer    Lucene 分词器类
 *  lucene-char-filter  存储时使用的 CharFilter  类
 *  lucene-token-filter 存储时使用的 TokenFilter 类
 *  lucene-find-filter  查询时使用的 CharFilter  类
 *  lucene-query-filter 查询时使用的 TokenFilter 类
 *
 * @author Hongs
 */
public class LuceneRecord extends ModelCase implements IEntity, ITrnsct, AutoCloseable {

    protected boolean OBJECT_MODE = false;
    protected boolean TRNSCT_MODE = false;
    protected final  boolean TRNSCT_BASE ;

    private IndexSearcher finder  = null ;
    private IndexReader   reader  = null ;
    private IndexWriter   writer  = null ;
    private String        dbpath  = null ;
    private String        dbname  = null ;

    /**
     * 构造方法
     *
     * 注意 : path 务必使用绝对地址
     *
     * @param form 字段配置, 可覆盖 getFields
     * @param path 存储路径, 可覆盖 getDbPath
     * @param name 存储名称, 可覆盖 getDbName
     */
    public LuceneRecord(Map form, String path, String name) {
        // 必要参数
        setFields (form);
        setDbPath (path);
        setDbName (name);

        // 环境模式
        CoreConfig conf = CoreConfig.getInstance();
        OBJECT_MODE = Synt.declare(
            Core.getInstance().got(Cnst.OBJECT_MODE),
            conf.getProperty("core.in.object.mode", false));
        TRNSCT_MODE = Synt.declare(
            Core.getInstance().got(Cnst.TRNSCT_MODE),
            conf.getProperty("core.in.trnsct.mode", false));
        TRNSCT_BASE = TRNSCT_MODE;
    }

    /**
     * 获取实例
     * 存储为 conf/form 表单为 conf.form
     * 表单缺失则尝试获取 conf/form.form
     * 实例生命周期将交由 Core 维护
     * @param conf
     * @param form
     * @return
     * @throws HongsException
     */
    public static LuceneRecord getInstance(String conf, String form) throws HongsException {
        String code = LuceneRecord.class.getName() +":"+ conf +"."+ form;
        Core   core = Core.getInstance( );
        if ( ! core.containsKey( code ) ) {
            String path = conf +"/"+ form;
            String name = conf +"."+ form;
            Map    fxrm = FormSet.getInstance(conf).getForm(form);

            // 表单配置中可指定数据路径
            Map c = (Map) fxrm.get("@");
            if (c!= null) {
                String p;
                p = (String) c.get("db-path");
                if (null != p && p.length() != 0) {
                    path  = p;
                }
                p = (String) c.get("db-name");
                if (null != p && p.length() != 0) {
                    name  = p;
                }
            }

            // 进一步处理路径中的变量等
            Map m = new HashMap();
            m.put("SERVER_ID", Core.SERVER_ID);
            m.put("CORE_PATH", Core.CORE_PATH);
            m.put("DATA_PATH", Core.DATA_PATH);
            path = Syno.inject(path, m);
            if ( ! new File(path).isAbsolute())
            path = Core.DATA_PATH + "/lucene/" + path;

            LuceneRecord inst = new LuceneRecord(fxrm, path,name);
            core.put( code, inst ) ; return inst ;
        } else {
            return  (LuceneRecord) core.got(code);
        }
    }

    //** 实体方法 **/

    /**
     * 获取数据

 以下参数为特殊参数, 可在 default.properties 中配置:
 id   ID, 仅指定单个 id 时则返回详情(info)
 rn   行数, 明确指定为 0 则不分页
 gn   分页
 pn   页码
 wd   搜索
 ob   排序
 r2   字段
 r2   多组"或"关系条件
 ar   串联多组关系条件
 sr   附加多组"或"关系, LuceneRecord 特有
 请注意尽量避免将其作为字段名(id,wd除外)
     *
     * @param rd
     * @return
     * @throws HongsException
     */
    @Override
    public Map search(Map rd) throws HongsException {
        // 指定单个 id 则走 getOne
        Object id = rd.get (Cnst.ID_KEY);
        if (id != null && !(id instanceof Collection) && !(id instanceof Map)) {
            if ( "".equals( id ) ) {
                return  new HashMap(); // id 为空则不获取
            }
            Map  data = new HashMap();
            Map  info = getOne(rd);
            data.put("info", info);
            return data;
        }

        // 默认仅返回可以列举的字段
        Set rb = Synt.declare(rd.get(Cnst.RB_KEY), Set.class);
        if (rb == null || rb.isEmpty( )) {
            rd =  new LinkedHashMap (rd);
            rd.put( Cnst.RB_KEY , getListable() );
        }

        // 获取行数, 默认依从配置
        int rn;
        if (rd.containsKey(Cnst.RN_KEY)) {
            rn = Synt.declare(rd.get(Cnst.RN_KEY), 0); if ( rn < 0 ) rn = Math.abs( rn );
        } else {
            rn = CoreConfig.getInstance().getProperty("fore.rows.per.page", Cnst.RN_DEF);
        }

        // 获取链数, 默认依从配置
        int gn;
        if (rd.containsKey(Cnst.GN_KEY)) {
            gn = Synt.declare(rd.get(Cnst.GN_KEY), 1); if ( gn < 0 ) gn = Math.abs( gn );
        } else {
            gn = CoreConfig.getInstance().getProperty("fore.pugs.for.page", Cnst.GN_DEF);
        }

        // 获取页码
        int pn = 1;
        if (rd.containsKey(Cnst.PN_KEY)) {
            pn = Synt.declare(rd.get(Cnst.PN_KEY), 1); if ( pn < 0 ) pn = Math.abs( pn );
        }

        // 指定行数 0, 则获取全部
        if (rn == 0) {
            Map  data = new HashMap();
            List list = getAll(rd);
            data.put("list", list);
            return data;
        }

        return search(rd, rn, pn, gn);
    }

    /**
     * 创建记录
     * @param rd
     * @return id,name等(由dispCols指定)
     * @throws HongsException
     */
    @Override
    public Map create(Map rd) throws HongsException {
        String  id = add( rd);
        Set<String> fs = getListable();
        if (null != fs && ! fs.isEmpty( )) {
            Map sd = new LinkedHashMap( );
            for(String fn : fs) {
                if ( ! fn.contains( "." )) {
                    sd.put( fn, rd.get(fn) );
                }
            }
            sd.put(Cnst.ID_KEY, id);
            return sd;
        } else {
            rd.put(Cnst.ID_KEY, id);
            return rd;
        }
    }

    /**
     * 更新记录
     * @param rd
     * @return
     * @throws HongsException
     */
    @Override
    public int update(Map rd) throws HongsException {
        Set<String> ids = Synt.asSet(rd.get(Cnst.ID_KEY));
        permit (rd, ids , 0x1096);
        for(String  id  : ids) {
            put(id, rd  );
        }
        return ids.size();
    }

    /**
     * 删除记录
     * @param rd
     * @return
     * @throws HongsException
     */
    @Override
    public int delete(Map rd) throws HongsException {
        Set<String> ids = Synt.asSet(rd.get(Cnst.ID_KEY));
        permit (rd, ids , 0x1097);
        for(String  id  : ids) {
            del(id /**/ );
        }
        return ids.size();
    }

    /**
     * 确保操作合法
     * @param rd
     * @param ids
     * @param ern
     * @throws HongsException
     */
    protected void permit(Map rd, Set ids, int ern) throws HongsException {
        if (rd  == null) {
            throw new NullPointerException( "rd can not be null" );
        }
        if (ids == null || ids.isEmpty()) {
            throw new NullPointerException("ids can not be empty");
        }

        Map wh = new HashMap();
        if (rd.containsKey(Cnst.AR_KEY)) {
            wh.put(Cnst.AR_KEY, rd.get(Cnst.AR_KEY));
        }
        if (rd.containsKey(Cnst.OR_KEY)) {
            wh.put(Cnst.OR_KEY, rd.get(Cnst.OR_KEY));
        }
        if (wh.isEmpty()) {
            return;
        }

        // 组织查询
        wh.put(Cnst.ID_KEY, ids);
        wh.put(Cnst.RB_KEY, Cnst.ID_KEY);
        Set idz = new HashSet( );
        Loop rs = search(wh,0,0);
        while  (  rs.hasNext() ) {
            Map ro = rs.next();
            idz.add( ro.get(Cnst.ID_KEY).toString());
        }

        // 对比数量, 取出多余的部分作为错误消息抛出
        if (ids.size( ) != idz.size( ) ) {
            Set    zd = new HashSet(ids);
                   zd . removeAll  (idz);
            String er = zd.toString(  );
            if (ern == 0x1096) {
                throw new HongsException(ern, "Can not update by id: " + er);
            } else
            if (ern == 0x1097) {
                throw new HongsException(ern, "Can not delete by id: " + er);
            } else
            {
                throw new HongsException(ern, "Can not search by id: " + er);
            }
        }
    }

    //** 模型方法 **/

    /**
     * 添加文档
     * @param rd
     * @return ID
     * @throws HongsException
     */
    public String add(Map rd) throws HongsException {
        String id = Synt.asString(rd.get(Cnst.ID_KEY));
        if (id != null && id.length() != 0) {
            throw new HongsException.Common("Id can not set in add");
        }
        id  =  Core.newIdentity();
        rd.put(Cnst.ID_KEY , id );
        addDoc(padDoc (rd));
        return id;
    }

    /**
     * 设置文档(无则添加)
     * @param id
     * @param rd
     * @throws HongsException
     */
    public void set(String id, Map rd) throws HongsException {
        if (id == null || id.length() == 0) {
            throw new NullPointerException("Id must be set in set");
        }
        Document doc = getDoc(id);
        if (doc == null) {
//          throw new NullPointerException("Doc#"+id+" not exists");
        } else {
            /**
             * 实际运行中发现
             * 直接往取出的 doc 里设置属性, 会造成旧值的索引丢失
             * 故只好转换成 map 再重新设置, 这样才能确保索引完整
             * 但那些 Store=NO 的数据将无法设置
             */
            Map md = padDat(doc );
                md . putAll( rd );
                rd = md;
        }
        rd.put(Cnst.ID_KEY , id );
        setDoc( id , padDoc (rd)); // 总是新建 Document
    }

    /**
     * 修改文档(局部更新)
     * @param id
     * @param rd
     * @throws HongsException
     */
    public void put(String id, Map rd) throws HongsException {
        if (id == null || id.length() == 0) {
            throw new NullPointerException("Id must be set in put");
        }
        Document doc = getDoc(id);
        if (doc == null) {
            throw new NullPointerException("Doc#"+id+" not exists");
        } else {
            /**
             * 实际运行中发现
             * 直接往取出的 doc 里设置属性, 会造成旧值的索引丢失
             * 故只好转换成 map 再重新设置, 这样才能确保索引完整
             * 但那些 Store=NO 的数据将无法设置
             */
            Map md = padDat(doc );
                md . putAll( rd );
                rd = md;
        }
        rd.put(Cnst.ID_KEY , id );
        setDoc( id , padDoc (rd)); // 总是新建 Document
    }

    /**
     * 删除文档(delDoc 的别名)
     * @param id
     * @throws HongsException
     */
    public void del(String id) throws HongsException {
        if (id == null || id.length() == 0) {
            throw new NullPointerException("Id must be set in del");
        }
        Document doc = getDoc(id);
        if (doc == null) {
            throw new NullPointerException("Doc#"+id+" not exists");
        }
        delDoc(id);
    }

    /**
     * 获取文档信息
     * @param id
     * @return
     * @throws HongsException
     */
    public Map get(String id) throws HongsException {
        Document doc = getDoc(id);
        if (doc != null) {
            return padDat ( doc );
        } else {
            return new HashMap( );
        }
    }

    /**
     * 获取单个文档
     * @param rd
     * @return
     * @throws HongsException
     */
    public Map getOne(Map rd) throws HongsException {
        Loop roll = search(rd, 0, 1);
        if   (   roll.hasNext( )) {
            return  roll.next( );
        } else {
            return new HashMap();
        }
    }

    /**
     * 获取全部文档
     * @param rd
     * @return
     * @throws HongsException
     */
    public List getAll(Map rd) throws HongsException {
        Loop roll = search(rd, 0, 0);
        List list = new ArrayList( );
        while  (  roll.hasNext()) {
            list.add(roll.next());
        }
        return list;
    }

    /**
     * 搜索查询文档
     * @param rd
     * @param begin 起始位置
     * @param limit 获取限制
     * @return
     * @throws HongsException
     */
    public Loop search(Map rd, int begin, int limit) throws HongsException {
        Query q = padQry(rd);
        Sort  s = padSrt(rd);
        Set   r = Synt.toTerms (rd.get(Cnst.RB_KEY));
        Loop  l = new Loop(this, q,s,r, begin,limit);

        if (0 < Core.DEBUG && 8 != (8 & Core.DEBUG)) {
            CoreLogger.debug("LuceneRecord.search: " + l.toString());
        }

        return l;
    }

    /**
     * 获取分页列表
     * @param rd
     * @param rn 每页行数
     * @param gn 组数
     * @param pn 页数
     * @return
     * @throws HongsException
     */
    public Map search(Map rd, int rn, int pn, int gn) throws HongsException {
        // 指定页码 0, 仅获取分页
        boolean wl = false;
        if (pn == 0) {
            wl = true;
            pn =  1;
        }
        if (gn == 0) {
            gn =  1;
        }

        // 获取页码, 计算查询区间
        int minPn = pn - (gn / 2);
        if (minPn < 1)   minPn=1 ;
        int maxPn = gn + minPn-1 ;
        int totRn = rn * maxPn+1 ;
        int minRn = rn * (pn - 1);
        int maxRn = rn + minRn   ;

        // 数量太少的话没必要估算
        int talRn = CoreConfig.getInstance().getProperty("core.search.least.limit", 65535);
        if (totRn < talRn) totRn = 1 + talRn / rn * rn ;

        Loop roll = search( rd, minRn , totRn - minRn );
        int  rc   = roll . size();
        int  pc   = ( int ) Math.ceil((double) rc / rn);
        int  st   ;

        if (rc == 0) {
            st =  0;
        } else
        if (rc <  minRn) {
            st =  0;
        } else
        if (rc <  totRn) {
            st =  1;
        } else
        {
            st =  2;
            rc -= 1;
            pc -= 1;
        }

        Map  resp = new HashMap();
        Map  page = new HashMap();
        page.put(Cnst.RN_KEY, rn);
        page.put(Cnst.GN_KEY, gn);
        page.put(Cnst.PN_KEY, pn);
        page.put("state", st );
        page.put("count", rc );
        page.put("pages", pc );
        resp.put("page", page);

        if (! wl) return resp ;

        // 提取分页片段
        List list = new ArrayList(st != 0 ? rn : 0 );
        while ( roll.hasNext() && maxRn > minRn ++ ) {
             list.add ( roll.next() );
        }
        resp.put("list", list);

        return resp;
    }

    //** 组件方法 **/

    public void addDoc(Document doc) throws HongsException {
        IndexWriter iw = getWriter();
        try {
            iw.addDocument (doc);
        } catch (IOException ex) {
            throw new HongsException.Common(ex);
        }
        if (!TRNSCT_MODE) {
            commit();
        }
    }

    public void setDoc(String id, Document doc) throws HongsException {
        IndexWriter iw = getWriter();
        try {
            iw.updateDocument (new Term("@"+Cnst.ID_KEY, id), doc);
        } catch (IOException ex) {
            throw new HongsException.Common(ex);
        }
        if (!TRNSCT_MODE) {
            commit();
        }
    }

    public void delDoc(String id) throws HongsException {
        IndexWriter iw = getWriter();
        try {
            iw.deleteDocuments(new Term("@"+Cnst.ID_KEY, id) /**/);
        } catch (IOException ex) {
            throw new HongsException.Common(ex);
        }
        if (!TRNSCT_MODE) {
            commit();
        }
    }

    public Document getDoc(String id) throws HongsException {
        IndexSearcher  ff = getFinder( );
        try {
                Query  qq = new TermQuery(new Term("@"+Cnst.ID_KEY, id));
              TopDocs  tt = ff.search(qq,  1  );
            ScoreDoc[] hh = tt.scoreDocs;
            if  ( 0 != hh.length ) {
                return ff.doc(hh[0].doc);
            } else {
                return null;
            }
        } catch ( IOException ex ) {
            throw new HongsException.Common(ex);
        }
    }

    public Document padDoc(Map map) {
        Document doc = new Document();
        padDoc(doc, map, null);
        return doc;
    }

    public Document padDoc(Map map, Set rep) {
        Document doc = new Document();
        padDoc(doc, map, rep);
        return doc;
    }

    public Map padDat(Document doc) {
        Map map = new LinkedHashMap();
        padDat(doc, map, null);
        return map;
    }

    public Map padDat(Document doc, Set rep) {
        Map map = new LinkedHashMap();
        padDat(doc, map, rep );
        return map;
    }

    public Query padQry(Map rd) throws HongsException {
        BooleanQuery.Builder qr = new BooleanQuery.Builder();

        padQry(qr, rd);

        BooleanQuery qu = qr.build();
        if (! qu.clauses().isEmpty()) {
            return qu ;
        }

        return new MatchAllDocsQuery( );
    }

    public Sort  padSrt(Map rd) throws HongsException {
        List<SortField> of = new LinkedList();

        padSrt(of, rd);

        if (of.isEmpty()) {
            of.add(SortField.FIELD_DOC);
        }

        return new Sort(of.toArray(new SortField[0]));
    }

    //** 组件封装 **/

    /**
     * 填充存储数据(将 map 填充到 doc)
     * 可覆盖此方法补充额外数据
     *
     * @param doc
     * @param map
     * @param rep
     */
    protected void padDoc(Document doc, Map map, Set rep) {
        if (rep != null && rep.isEmpty( )) {
            rep  = null;
        }

        Map<String, Map> fields = getFields();
        for(Map.Entry<String, Map> e : fields.entrySet()) {
            Map    m = e.getValue();
            String k = e.getKey  ();
            Object v = Dict.getParam(map , k);

            doc.removeFields(k);

            if (rep != null
            && !rep.contains(k)) {
                continue;
            }

            if (v == null
            ||  k == null
            ||  k.equals("@")
            ||  unstated( m )) {
                continue;
            }

            IField  f ;
            String  t = datatype(m);
            boolean r = repeated(m);
            boolean s = sortable(m);
            boolean p = srchable(m);
            boolean q = findable(m);
            boolean g =!unstored(m);

            if (Cnst.ID_KEY.equals(k)) {
                q  = true;
                g  = true;
            }

            if (t != null) switch (t)  {
            case "int":
                if ("".equals(v)) continue;
                f = new IntField();
                p = false;
                break;
            case "long":
            case "date":
                if ("".equals(v)) continue;
                f = new LongField();
                p = false;
                break;
            case "float":
                if ("".equals(v)) continue;
                f = new FloatField();
                p = false;
                break;
            case "double":
            case "number":
                if ("".equals(v)) continue;
                f = new DoubleField();
                p = false;
                break;
            case "sorted":
                if ("".equals(v)) continue;
                f = new LongField();
                s = true ;
                g = false; // 排序类型无需存储
                p = false; // 排序类型无法搜索
                q = false; // 排序类型无法筛选
                break;
            case "object":
                if ("".equals(v)) continue;
                f = new ObjectFiald();
                g = true ;
                p = false; // 对象类型无法搜索
                q = false; // 对象类型无法筛选
                s = false; // 对象类型无法排序
                break;
            case "stored":
                f = new StringFiald();
                g = true ;
                p = false; // 存储类型无法搜索
                q = false; // 存储类型无法筛选
                s = false; // 存储类型无法排序
                break;
            case "search":
                f = new StringFiald();
                p = true ;
                g = false; // 搜索类型无需存储
                q = false; // 搜索类型无法筛选
                s = false; // 搜索类型无法排序
                break;
            default:
                f = new StringFiald();
            } else {
                f = new StringFiald();
            }

            if (r) {
                if (g) {
                    if (v instanceof Object [ ]) {
                        for (Object w: (Object [ ]) v) {
                            doc.add(f.get(k, w));
                        }
                    } else
                    if (v instanceof Collection) {
                        for (Object w: (Collection) v) {
                            doc.add(f.get(k, w));
                        }
                    } else
                    {
                        Set a = Synt.asSet ( v );
                        for (Object w: a ) {
                            doc.add(f.get(k, w));
                        }
                        v = a;
                    }
                }

                // 条件类可去重
                Set a = Synt.asSet(v);

                if (s && a != null && !a.isEmpty()) {
                    Object  w = a.toArray( )[0]; // 排序值不能存多个
                        doc.add(f.odr(k, w));
                }
                if (q && a != null && !a.isEmpty()) {
                    for (Object w: a) {
                        doc.add(f.whr(k, w));
                    }
                }
                if (p && a != null && !a.isEmpty()) {
                    for (Object w: a) {
                        doc.add(f.wdr(k, w));
                    }
                }
            } else
            {
                if (g) {
                    doc.add(f.get(k, v));
                }
                if (s) {
                    doc.add(f.odr(k, v));
                }
                if (q) {
                    doc.add(f.whr(k, v));
                }
                if (p) {
                    doc.add(f.wdr(k, v));
                }
            }
        }
    }

    /**
     * 填充返回数据(将 doc 填充到 map)
     * 可覆盖此方法补充额外数据
     *
     * @param doc
     * @param map
     * @param rep
     */
    protected void padDat(Document doc, Map map, Set rep) {
        if (rep != null && rep.isEmpty( )) {
            rep  = null;
        }

        Map<String, Map> fields = getFields();
        for(Map.Entry<String, Map> e : fields.entrySet()) {
            Map    m = e.getValue();
            String k = e.getKey  ();

            if (rep != null
            && !rep.contains(k)) {
                continue;
            }

            if (k == null
            ||  k.equals("@")
            ||  unstated( m )
            ||  unstored( m )) {
                continue;
            }

            IValue  v ;
            String  t = datatype(m);
            boolean r = repeated(m);
            IndexableField[] fs = doc.getFields(k);

            if (t != null) switch (t) {
            case "search":
            case "sorted":
                continue; // 纯功能字段无可见值
            case "date":
                // 时间戳转 Date 对象时需要乘以 1000
                String  y = Synt.declare(m.get("type"), "");
                if (OBJECT_MODE) {
                    if ("time".equals(y) || "timestamp".equals(y)) {
                        v = new NumberValue( );
                    } else {
                        v = new DatimeValue(m);
                    }
                } else {
                    if ("time".equals(y) || "timestamp".equals(y)) {
                        v = new NumeraValue( );
                    } else {
                        v = new DatextValue(m);
                    }
                }
                break;
            case "int":
            case "long":
            case "float":
            case "double":
            case "number":
                if (OBJECT_MODE) {
                    v = new NumberValue();
                } else {
                    v = new NumeraValue();
                }
                break;
            case "object":
                v = new ObjectValue();
                break;
            default:
                v = new StringValue();
            } else {
                v = new StringValue();
            }

            if (r) {
                if (fs.length > 0) {
                    for(IndexableField f : fs ) {
                        Dict.put(map , v.get(f), k, null);
                    }
                } else {
                    map.put(k , new ArrayList());
                }
            } else {
                if (fs.length > 0) {
                    map.put(k , v.get ( fs[0] ));
                } else {
                    map.put(k , null);
                }
            }
        }
    }

    /**
     * 组织查询条件
     * 可覆盖此方法扩展顶层条件
     *
     * @param qr
     * @param rd
     * @throws HongsException
     */
    protected void padQry(BooleanQuery.Builder qr, Map rd) throws HongsException {
        LuceneRecord.this.padQry(qr, rd, 0);
    }

    /**
     * 组织查询条件
     * 可覆盖此方法扩展查询条件
     *
     * @param qr
     * @param rd
     * @param r 递归层级
     * @throws HongsException
     */
    protected void padQry(BooleanQuery.Builder qr, Map rd, int r) throws HongsException {
        int i = 0, j = 0; // 条件数量, 否定数量, 计数规避全否定时查不到数据

        Map<String, Map> fields = getFields();
        Set<String> ks = new LinkedHashSet(fields.keySet());
                    ks.retainAll(rd.keySet());

        for(String k : ks) {
            Object v = rd.get(k);
            Map m =fields.get(k);

            if (m == null
            ||  v == null) {
                continue;
            }

            IQuery qa ;
            String t  =  datatype (m);
            if (t != null) switch (t) {
            case "int":
                qa = new IntQuery();
                break;
            case "long":
            case "date":
                qa = new LongQuery();
                break;
            case "float":
                qa = new FloatQuery();
                break;
            case "double":
            case "number":
                qa = new DoubleQuery();
                break;
            case "string":
            case "search":
            if (!srchable(m)) {
                qa = new StringQuery();
            } else {
                SearchQuery qs;
                qa = qs = new SearchQuery();
                qs.analyser(getAnalyser(m));
                qs.settings(m);
            }
                break;
            default:
                continue;
            } else {
                continue;
            }

            //** 常规查询 **/

            Map vd;
            if (v instanceof Map) {
                vd = (Map) v ;
            } else
            if (v instanceof Collection
            ||  v instanceof Object[ ]) {
                Set  a = Synt.asSet(v);
                     a.remove("");
                if (!a.isEmpty( )) {
                    BooleanQuery.Builder qx = new BooleanQuery.Builder();
                    for( Object b : a ) {
                        qx.add(qa.whr(k, b), BooleanClause.Occur.SHOULD);
                    }
                    qr.add(qx.build(  ), BooleanClause.Occur.MUST);
                    i ++;
                }
                continue;
            } else {
                if (!v.equals("")) {
                    qr.add(qa.whr(k, v), BooleanClause.Occur.MUST);
                    i ++;
                }
                continue;
            }

            //** 条件关系 **/

            v = vd.get(Cnst.OR_REL);
            if (Cnst.OR_KEY.equals(v )
            ||  Cnst.NR_KEY.equals(v)) {
                List r2 = new ArrayList(vd.size() - 1);
                for(Object ot : vd.entrySet()) {
                    Map.Entry et = (Map.Entry) ot;
                    Object k2 = et.getKey  ();
                    if (! Cnst.OR_KEY.equals(k2)) {
                    Object v2 = et.getValue();
                        r2.add(Synt.mapOf(k2,v2));
                    }
                }
                if ( !  r2.isEmpty( )) {
                    padQry(qr, Synt.mapOf(v ,r2), r-1);
                }
                continue;
            }

            //** 空值查询 **/

            v = vd.get(Cnst.IS_REL);
            if ( v != null ) {
                String b = qa instanceof SearchQuery ? "$" : "@";
                String a = Synt.asString(v);
                Query  p ;
                try {
                    p = new QueryParser(b + k, new StandardAnalyzer()).parse("[* TO *]");
                } catch (ParseException ex) {
                    throw new HongsExemption.Common(ex);
                }
                if ("WELL".equalsIgnoreCase(a)) {
                    qr.add(p, BooleanClause.Occur.MUST);
                    i ++;
                } else
                if ("NULL".equalsIgnoreCase(a)) {
                    qr.add(p, BooleanClause.Occur.MUST_NOT);
                    i ++;  j ++;
                }
            }

            //** 精确匹配 **/

            v = vd.get(Cnst.EQ_REL);
            if ( v != null ) {
                qr.add(qa.whr(k, v), BooleanClause.Occur.MUST);
                i ++;
            }
            v = vd.get(Cnst.NE_REL);
            if ( v != null ) {
                qr.add(qa.whr(k, v), BooleanClause.Occur.MUST_NOT);
                i ++;  j ++;
            }

            //** 模糊匹配 **/

            v = vd.get(Cnst.CQ_REL);
            if ( v != null && ! "".equals(v) ) {
                qr.add(qa.wdr(k, v), BooleanClause.Occur.MUST);
                i ++;
            }
            v = vd.get(Cnst.NC_REL);
            if ( v != null && ! "".equals(v) ) {
                qr.add(qa.wdr(k, v), BooleanClause.Occur.MUST_NOT);
                i ++;  j ++;
            }

            //** 集合查询 **/

            v = vd.get(Cnst.AI_REL);
            if ( v != null ) {
                Set vs = Synt.asSet(v);
                if(!vs.isEmpty( )) {
                    for(Object vv : vs) {
                        qr.add(qa.whr(k, vv), BooleanClause.Occur.MUST);
                        i ++;
                    }
                }
            }
            v = vd.get(Cnst.NI_REL);
            if ( v != null ) {
                Set vs = Synt.asSet(v);
                if(!vs.isEmpty( )) {
                    for(Object vv : vs) {
                        qr.add(qa.whr(k, vv), BooleanClause.Occur.MUST_NOT);
                        i ++;  j ++;
                    }
                }
            }
            v = vd.get(Cnst.IN_REL);
            if ( v != null ) {
                Set vs = Synt.asSet(v);
                if(!vs.isEmpty( )) {
                    BooleanQuery.Builder  qx = new BooleanQuery.Builder();
                    for(Object vv : vs) {
                        qx.add(qa.whr(k, vv), BooleanClause.Occur.SHOULD);
                    }
                        qr.add(qx.build (  ), BooleanClause.Occur.MUST  );
                        i ++;
                }
            }

            //** 区间查询 **/

            Object  n, x;
            boolean l, g;

            n = vd.get(Cnst.GT_REL);
            if (n != null) {
                l  = false;
            } else {
            n = vd.get(Cnst.GE_REL);
            if (n != null) {
                l  = true ;
            } else {
                n  = null ;
                l  = true ;
            }}

            x = vd.get(Cnst.LT_REL);
            if (x != null) {
                g  = false;
            } else {
            x = vd.get(Cnst.LE_REL);
            if (x != null) {
                g  = true ;
            } else {
                x  = null ;
                g  = true ;
            }}

            if ((n != null && ! "".equals(n))
            ||  (x != null && ! "".equals(x))) {
                Query  qu = qa.whr( k, n, x, l, g );
                qr.add(qu,BooleanClause.Occur.MUST);
                i ++;
            }

            Set s  = null;
            v = vd.get(Cnst.RG_REL);
            if (v != null) {
                s  = Synt.asSet (v);
            }

            if (s != null && ! s.isEmpty()) {
                BooleanQuery.Builder qx = new BooleanQuery.Builder();

                for(Object o : s ) {
                    Object[] a = Synt.toRange(o);
                    if (a == null) {
                        continue ;
                    }

                    n = a[0]; l = (boolean) a[2];
                    x = a[1]; g = (boolean) a[3];
                    if (n == null
                    &&  x == null) {
                        continue ;
                    }

                       Query qu = qa.whr (k , n , x , l , g);
                    qx.add ( qu, BooleanClause.Occur.SHOULD);
                }

                BooleanQuery qz = qx.build();
                if (qz.clauses().size() > 0) {
                    qr.add ( qz, BooleanClause.Occur.MUST  );
                    i ++;
                }
            }
        }

        Object v;

        //** 全局搜索 **/

        v = rd.get(Cnst.WD_KEY);
        if ( v != null && ! "".equals(v) ) {
            Set<String> fs = getWdCols();
            if (fs.size() > 0) {
                BooleanQuery.Builder qx = new BooleanQuery.Builder();
                 SearchQuery         qs = new  SearchQuery        ();

                for(String fn : fs) {
                    Map fc  = fields.get(fn);
                    if (fc == null) continue;
                    qs.settings( /*parser*/ fc );
                    qs.analyser(getAnalyser(fc));
                    qx.add(qs.wdr(fn, v),BooleanClause.Occur.SHOULD);
                }

                BooleanQuery qa = qx.build();
                if (! qa.clauses().isEmpty()) {
                    qr.add(qa, BooleanClause.Occur.MUST  );
                    i ++;
                }
            }
        }

        //** 递归条件 **/

        v = rd.get(Cnst.AR_KEY);
        if ( v != null ) {
            if ( r > 2 ) {
                throw new HongsException.Common("Key '" + Cnst.AR_KEY + "' can not exceed 2 layers");
            }
            Set<Map> set = Synt.asSet(v);
            if (set != null && ! set.isEmpty()) {
            for(Map  map : set) {
                BooleanQuery.Builder qx = new BooleanQuery.Builder();
                padQry( qx, map, r + 1 );

                BooleanQuery qa = qx.build();
                if (! qa.clauses().isEmpty()) {
                    // 权重
                    Query qb = qa;
                    v = map.get(Cnst.WT_REL);
                    if (v != null && !"".equals(v)) {
                        qb = new BoostQuery(qa, Synt.declare(v, 1f));
                    }

                    qr.add(qb, BooleanClause.Occur.MUST);
                    i ++;
                }
            }
            }
        }

        v = rd.get(Cnst.NR_KEY);
        if ( v != null ) {
            if ( r > 2 ) {
                throw new HongsException.Common("Key '" + Cnst.NR_KEY + "' can not exceed 2 layers");
            }
            Set<Map> set = Synt.asSet(v);
            if (set != null && ! set.isEmpty()) {
            for(Map  map : set) {
                BooleanQuery.Builder qx = new BooleanQuery.Builder();
                padQry( qx, map, r + 1 );

                BooleanQuery qa = qx.build();
                if (! qa.clauses().isEmpty()) {
                    // 权重
                    Query qb = qa;
                    v = map.get(Cnst.WT_REL);
                    if (v != null && !"".equals(v)) {
                        qb = new BoostQuery(qa, Synt.declare(v, 1f));
                    }

                    qr.add(qb, BooleanClause.Occur.MUST_NOT);
                    i ++;  j ++;
                }
            }
            }
        }

        v = rd.get(Cnst.OR_KEY);
        if ( v != null ) {
            if ( r > 2 ) {
                throw new HongsException.Common("Key '" + Cnst.OR_KEY + "' can not exceed 2 layers");
            }
            Set<Map> set = Synt.asSet(v);
            if (set != null && ! set.isEmpty()) {
                BooleanQuery.Builder qz = new BooleanQuery.Builder();
            for(Map  map : set) {
                BooleanQuery.Builder qx = new BooleanQuery.Builder();
                padQry( qx, map, r + 1 );

                BooleanQuery qa = qx.build();
                if (! qa.clauses().isEmpty()) {
                    // 权重
                    Query qb = qa;
                    v = map.get(Cnst.WT_REL);
                    if (v != null && !"".equals(v)) {
                        qb = new BoostQuery(qa, Synt.declare(v, 1f));
                    }

                    qz.add(qb, BooleanClause.Occur.SHOULD);
                }
            }
                BooleanQuery qa = qz.build();
                if (! qa.clauses().isEmpty()) {
                    qr.add(qa, BooleanClause.Occur.MUST  );
                    i ++;
                }
            }
        }

        /**
         * 如果全部条件都是 MUST_NOT 会导致取不到数据
         * 故有必要增加一个 MUST all 从而规避这个问题
         */
        if (i > 0 & i == j) {
            qr.add(new MatchAllDocsQuery(), BooleanClause.Occur.MUST);
        }
    }

    /**
     * 组织排序规则
     * 可覆盖此方法进行补充排序
     *
     * @param of
     * @param rd
     * @throws HongsException
     */
    protected void padSrt(List<SortField> of, Map rd) throws HongsException {
        Set<String> ob = Synt.toTerms(rd.get(Cnst.OB_KEY));
        if (ob == null) {
            return;
        }

        Map<String, Map> fields = getFields();
        for (String fn: ob) {
            // 相关
            if (fn.equals("-")) {
                of.add(SortField.FIELD_SCORE);
                continue;
            }

            // 文档
            if (fn.equals("_")) {
                of.add(SortField.FIELD_DOC);
                continue;
            }

            // 逆序
            boolean rv = fn.startsWith("-");
            if (rv) fn = fn.substring ( 1 );

            // 自定义排序
            if (! LuceneRecord.this.padSrt (of, rd, fn, rv) ) {
                continue;
            }

            Map m  = (Map) fields.get(fn);
            if (m == null) {
                continue ;
            }
            if (! sortable(m)) {
                continue ;
            }

            SortField.Type st;
            String t = datatype(m);
            if (   "int".equals(t)) {
                st = SortField.Type.INT;
            } else
            if (  "long".equals(t)) {
                st = SortField.Type.LONG;
            } else
            if ( "float".equals(t)) {
                st = SortField.Type.FLOAT;
            } else
            if ("double".equals(t)) {
                st = SortField.Type.DOUBLE;
            } else
            if (  "date".equals(t)) {
                st = SortField.Type.LONG;
            } else
            if ("sorted".equals(t)) {
                st = SortField.Type.LONG;
            } else
            if ("search".equals(t)) {
                st = SortField.Type.STRING;
            } else
            if ("string".equals(t)) {
                st = SortField.Type.STRING;
            } else
            {
                continue;
            }

            /**
             * 因为 Lucene 5 必须使用 DocValues 才能排序
             * 在更新数据时, 默认有加 '#' 打头的排序字段
             */
            of.add(new SortField("#" + fn , st , rv));
        }
    }

    /**
     * 自定排序规则
     * 可覆盖此方法进行特殊排序
     *
     * @param of
     * @param rd
     * @param k 排序字段
     * @param r 是否逆序
     * @return 返回 false 阻断
     */
    protected boolean padSrt(List<SortField> of, Map rd, String k, boolean r) {
        return true;
    }

    //** 事务方法 **/

    /**
     * 销毁读写连接
     */
    @Override
    public void close() {
        if (reader != null ) {
            try {
                reader.close();
            } catch (IOException x) {
                CoreLogger.error(x);
            } finally {
                reader = null ;
                finder = null ;
            }

            if (0 < Core.DEBUG && 4 != (4 & Core.DEBUG)) {
                CoreLogger.trace("Close the lucene reader for " + getDbName());
            }
        }

        if (writer != null ) {
        if (writer.isOpen()) {
            // 默认退出时提交
            if (TRNSCT_MODE) {
                try {
                try {
                    commit();
                } catch (Throwable e) {
                    revert();
                    throw e ;
                }
                } catch (Throwable e) {
                    CoreLogger.error(e);
                }
            }

            // 退出时合并索引
            try {
                writer.maybeMerge();
            } catch (IOException x) {
                CoreLogger.error(x);
            }

            try {
                writer.close();
            } catch (IOException x) {
                CoreLogger.error(x);
            } finally {
                writer = null ;
            }

            if (0 < Core.DEBUG && 4 != (4 & Core.DEBUG)) {
                CoreLogger.trace("Close the lucene writer for " + getDbName());
            }
        } else {
            /**/writer = null ;
        }
        }
    }

    /**
     * 事务开始
     */
    @Override
    public void begin() {
        TRNSCT_MODE = true;
    }

    /**
     * 提交更改
     */
    @Override
    public void commit() {
        TRNSCT_MODE = TRNSCT_BASE;
        if (writer == null) {
            return;
        }
        try {
            writer.commit ( );
        } catch (IOException ex) {
            throw new HongsExemption(0x102c, ex);
        }
    }

    /**
     * 回滚操作
     */
    @Override
    public void revert() {
        TRNSCT_MODE = TRNSCT_BASE;
        if (writer == null) {
            return;
        }
        try {
            writer.rollback();
        } catch (IOException ex) {
            throw new HongsExemption(0x102d, ex);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
           this.close(   );
        } finally {
          super.finalize();
        }
    }

    //** 底层方法 **/

    protected final void setDbPath(String path) {
        dbpath = path;
    }

    protected final void setDbName(String name) {
        dbname = name;
    }

    public String getDbPath() {
        if (dbpath == null) {
            throw new NullPointerException("DB path is not set");
        }
        return dbpath;
    }

    public String getDbName() {
        if (dbname == null) {
            throw new NullPointerException("DB name is not set");
        }
        return dbname;
    }

    /**
     * 获取搜索列
     * 特别针对 wd 查询参数
     * 默认等同 getSrchable
     * @return
     */
    protected Set<String> getWdCols() {
        return getSrchable();
    }

    public IndexSearcher getFinder() throws HongsException {
        IndexReader ir = getReader(); // 见下方注释
        if (finder == null) {
            finder  = new IndexSearcher(ir);
        }
        return finder;
    }

    public IndexReader getReader() throws HongsException {
        if (reader != null) {
            try {
                // 如果有更新数据则会重新打开查询接口
                // 这可以规避提交更新后却查不到的问题
                IndexReader  nred = DirectoryReader.openIfChanged((DirectoryReader) reader);
                if ( null != nred) {
                    reader.close();
                    reader = nred ;
                    finder = null ;
                }
            } catch (IOException x) {
                throw new HongsException.Common(x);
            }
        } else {
            String path = getDbPath();

            try {
                // 目录不存在需开写并提交从而建立索引
                // 否则会抛出: IndexNotFoundException
                if (! new File(path).exists())
                    getWriter (    ).commit();

                Directory dir = FSDirectory.open(Paths.get(path));

                reader = DirectoryReader.open(dir);
            } catch (IOException x) {
                throw new HongsException.Common(x);
            }

            if (0 < Core.DEBUG && 4 != (4 & Core.DEBUG)) {
                CoreLogger.trace("Start the lucene reader for "+getDbName());
            }
        }
        return reader;
    }

    public IndexWriter getWriter() throws HongsException {
        if (writer == null || writer.isOpen() == false ) {
            String path = getDbPath();

            try {
                IndexWriterConfig iwc = new IndexWriterConfig(getAnalyzer());
                iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

                Directory dir = FSDirectory.open(Paths.get(path));

                writer = new IndexWriter(dir, iwc);
            } catch (IOException x) {
                throw new HongsException.Common(x);
            }

            if (0 < Core.DEBUG && 4 != (4 & Core.DEBUG)) {
                CoreLogger.trace("Start the lucene writer for "+getDbName());
            }
        }
        return writer;
    }

    //** 底层工具 **/

    /**
     * 存储分析器
     * @return
     * @throws HongsException
     */
    protected Analyzer getAnalyzer() throws HongsException {
        /*Default*/ Analyzer  ad = new StandardAnalyzer();
        Map<String, Analyzer> az = new HashMap();
        Map<String, Map     > fs = getFields(  );
        for(Map.Entry<String, Map> et : fs.entrySet()) {
            String fn = et.getKey(  );
            Map    fc = et.getValue();
            if (srchable(fc)) {
                // 注意: 搜索对应的字段名开头为 $
                az.put("$" + fn, getAnalyzer(fc) );
            }
        }
        return new PerFieldAnalyzerWrapper(ad, az);
    }

    /**
     * 存储分析器
     * @param fc 字段配置
     * @return
     * @throws HongsException
     */
    protected Analyzer getAnalyzer(Map fc) throws HongsException {
        try {
            CustomAnalyzer.Builder cb = CustomAnalyzer.builder();
            String kn, an, ac; Map oc;

            // 分词器
            an = Synt.declare(fc.get("lucene-tokenizer"), "");
            if (!"".equals(an)) {
                int p  = an.indexOf('{');
                if (p != -1) {
                    ac = an.substring(p);
                    an = an.substring(0, p - 1).trim();
                    oc = Synt.asMap(Dawn.toObject(ac));
                    cb.withTokenizer(an, oc);
                } else {
                    cb.withTokenizer(an/**/);
                }
            } else {
                cb.withTokenizer("Standard");
            }

            // 过滤器
            for(Object ot2 : fc.entrySet()) {
                Map.Entry et2 = (Map.Entry) ot2;
                kn = (String) et2.getKey();

                // 存储参数为 char,token
                if (kn.startsWith("lucene-char-filter")) {
                    an = (String) et2.getValue();
                    an = an.trim();
                    if ("".equals(an)) {
                        continue;
                    }
                    int p  = an.indexOf('{');
                    if (p != -1) {
                        ac = an.substring(p);
                        an = an.substring(0, p - 1).trim();
                        oc = Synt.asMap(Dawn.toObject(ac));
                        cb.addCharFilter(an, oc);
                    } else {
                        cb.addCharFilter(an/**/);
                    }
                } else
                if (kn.startsWith("lucene-token-filter")) {
                    an = (String) et2.getValue();
                    an = an.trim();
                    if ("".equals(an)) {
                        continue;
                    }
                    int p  = an.indexOf('{');
                    if (p != -1) {
                        ac = an.substring(p);
                        an = an.substring(0, p - 1).trim();
                        oc = Synt.asMap(Dawn.toObject(ac));
                        cb.addTokenFilter(an, oc);
                    } else {
                        cb.addTokenFilter(an/**/);
                    }
                }
            }

            return cb.build();
        } catch ( IOException ex) {
            throw new HongsException.Common(ex);
        } catch ( IllegalArgumentException  ex) {
            throw new HongsException.Common(ex);
        }
    }

    /**
     * 查询分析器
     * @param fc 字段配置
     * @return
     * @throws HongsException
     */
    protected Analyzer getAnalyser(Map fc) throws HongsException {
        try {
            CustomAnalyzer.Builder cb = CustomAnalyzer.builder();
            String kn, an, ac; Map oc;

            // 分词器
            an = Synt.declare(fc.get("lucene-tokenizer"), "");
            if (!"".equals(an)) {
                int p  = an.indexOf('{');
                if (p != -1) {
                    ac = an.substring(p);
                    an = an.substring(0, p - 1).trim();
                    oc = Synt.asMap(Dawn.toObject(ac));
                    cb.withTokenizer(an, oc);
                } else {
                    cb.withTokenizer(an/**/);
                }
            } else {
                cb.withTokenizer("Standard");
            }

            // 过滤器
            for(Object ot2 : fc.entrySet()) {
                Map.Entry et2 = (Map.Entry) ot2;
                kn = (String) et2.getKey();

                // 查询参数为 find,query
                if (kn.startsWith("lucene-find-filter")) {
                    an = (String) et2.getValue();
                    an = an.trim();
                    if ("".equals(an)) {
                        continue;
                    }
                    int p  = an.indexOf('{');
                    if (p != -1) {
                        ac = an.substring(p);
                        an = an.substring(0, p - 1).trim();
                        oc = Synt.asMap(Dawn.toObject(ac));
                        cb.addCharFilter(an, oc);
                    } else {
                        cb.addCharFilter(an/**/);
                    }
                } else
                if (kn.startsWith("lucene-query-filter")) {
                    an = (String) et2.getValue();
                    an = an.trim();
                    if ("".equals(an)) {
                        continue;
                    }
                    int p  = an.indexOf('{');
                    if (p != -1) {
                        ac = an.substring(p);
                        an = an.substring(0, p - 1).trim();
                        oc = Synt.asMap(Dawn.toObject(ac));
                        cb.addTokenFilter(an, oc);
                    } else {
                        cb.addTokenFilter(an/**/);
                    }
                }
            }

            return cb.build();
        } catch ( IOException ex) {
            throw new HongsException.Common(ex);
        } catch ( IllegalArgumentException  ex) {
            throw new HongsException.Common(ex);
        }
    }

    /**
     * 获取类型变体
     * 返回的类型有
     * int
     * long
     * float
     * double
     * search
     * string
     * object
     * date
     * @param fc 字段配置
     * @return
     */
    protected String datatype(Map fc) {
        String t;

        t = (String) fc.get("lucene-type");
        if (t != null) {
            return t.equals(  "text"  )
                 ?  "search"
                 : t;
        }

        t = (String) fc.get("__type__");
        if (t == null) {
            return t;
        }

        Set<String> ks;

        //** 先查特有的 **/

        ks = getSaveTypes("search");
        if (ks != null && ks.contains(t)) {
            return "search";
        }

        ks = getSaveTypes("sorted");
        if (ks != null && ks.contains(t)) {
            return "sorted";
        }

        ks = getSaveTypes("stored");
        if (ks != null && ks.contains(t)) {
            return "stored";
        }

        ks = getSaveTypes("object");
        if (ks != null && ks.contains(t)) {
            return "object";
        }

        //** 再查一般的 **/

        ks = getSaveTypes( "date" );
        if (ks != null && ks.contains(t)) {
            return  "date" ;
        }

        ks = getSaveTypes( "enum" );
        if (ks != null && ks.contains(t)) {
            return Synt.declare(fc.get("type"), "string");
        }

        ks = getSaveTypes("string");
        if (ks != null && ks.contains(t)) {
            return "string";
        }

        ks = getSaveTypes("number");
        if (ks != null && ks.contains(t)) {
            return Synt.declare(fc.get("type"), "double");
        }

        return t;
    }

    protected boolean findable(Map fc) {
        String name = Synt.declare(fc.get("__name__"), "");
        return getFindable().contains(name) || Cnst.ID_KEY.equals(name);
    }

    protected boolean sortable(Map fc) {
        String name = Synt.declare(fc.get("__name__"), "");
        return getSortable().contains(name) || Cnst.ID_KEY.equals(name);
    }

    protected boolean srchable(Map fc) {
        String name = Synt.declare(fc.get("__name__"), "");
        return getSrchable().contains(name);
    }

    protected boolean rankable(Map fc) {
        String name = Synt.declare(fc.get("__name__"), "");
        return getRankable().contains(name);
    }

    protected boolean repeated(Map fc) {
        return Synt.declare(fc.get("__repeated__"), false);
    }

    protected boolean unstored(Map fc) {
        return Synt.declare(fc.get(  "unstored"  ), false);
    }

    protected boolean unstated(Map fc) {
        return Synt.declare(fc.get(  "unstated"  ), false);
    }

    //** 辅助对象 **/

    /**
     * 查询迭代器
     */
    public static class Loop implements Iterable<Map>, Iterator<Map> {
        private final IndexSearcher finder;
        private final IndexReader   reader;
        private final LuceneRecord  that;
        private       ScoreDoc[]    docs;
        private       ScoreDoc      doc ;
        private final Query   q;
        private final Sort    s;
        private final Set     r;
        private final int     b; // 起始位置
        private final int     l; // 单次限制
        private       int     L; // 起始限制
        private       int     h; // 单次总数
        private       int     H; // 全局总数
        private       int     i; // 提取游标
        private       boolean A; // 无限查询

        /**
         * 查询迭代器
         * @param that 记录实例
         * @param q 查询对象
         * @param s 排序对象
         * @param r 返回字段
         * @param b 起始偏移
         * @param l 查询限额
         */
        public Loop(LuceneRecord that, Query q, Sort s, Set r, int b, int l) {
            this.that = that;
            this.docs = null;
            this.doc  = null;
            this.q    = q;
            this.s    = s;
            this.r    = r;
            this.b    = b;

            // 是否获取全部
            if ( l  ==  0) {
                 l = CoreConfig.getInstance().getProperty("core.search.least.limit", 65535);
                 A = true;
            }

            this.l    = l;
            this.L    = l;

            // 起始位置偏移
            if ( b  !=  0) {
                 L  +=  b;
            }

            // 获取查读对象
            try {
                finder = that.getFinder();
                reader = that.getReader();
            } catch ( HongsException ex ) {
                throw ex.toExemption(   );
            }
        }

        @Override
        public Iterator<Map> iterator() {
            return this;
        }

        @Override
        public boolean hasNext() {
            try {
                if ( docs == null) {
                     TopDocs tops;
                    if (s != null) {
                        tops = finder.search/***/(/***/q, L, s);
                    } else {
                        tops = finder.search/***/(/***/q, L);
                    }
                    docs = tops.scoreDocs;
                    h    = docs.length;
                    i    = b;
                    H    = h;
                    L    = l;
                } else
                if ( A && L <= i ) {
                     TopDocs tops;
                    if (s != null) {
                        tops = finder.searchAfter(doc, q, l, s);
                    } else {
                        tops = finder.searchAfter(doc, q, l);
                    }
                    docs = tops.scoreDocs;
                    h    = docs.length;
                    i    = 0;
                    H   += h;
                }
                return i < h;
            } catch (IOException ex) {
                throw new HongsExemption.Common(ex);
            }
        }

        @Override
        public Map next() {
            if ( i >= h ) {
                throw new NullPointerException("hasNext not run?");
            }
            try {
                /*Read*/ doc = docs[i ++ ];
                Document dox = reader.document( doc.doc );
                return  that.padDat(dox,r);
            } catch (IOException ex) {
                throw new HongsExemption.Common(ex);
            }
        }

        /**
         * 获取命中总数
         * 注意:
         * 初始化时 y 参数为 0 (即获取全部命中)
         * 则在全部循环完后获取到的数值才是对的
         * 但其实此时完全可以直接计算循环的次数
         * 此方法主要用于分页时获取查询命中总量
         * @return
         */
        public int size() {
            if (docs == null) {
                hasNext();
            }
            return H ;
        }

        public List<Map> toList() {
            List<Map> list = new ArrayList();
            while  (  hasNext() ) {
                list.add(next() );
            }
            return list;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder(that.getDbName());
            if ( q != null ) {
                sb.append(" QUERY: ");
                sb.append( q );
            }
            if ( s != null ) {
                sb.append( " SORT: ");
                sb.append( s );
            }
            if ( r != null && !r.isEmpty()) {
                sb.append(" REPLY: ");
            for(Object x : r ) {
                sb.append( x );
                sb.append(",");
            }   sb.setLength(sb.length()-1);
            }
            if ( l != 0 || b != 0 ) {
                sb.append(" LIMIT: ");
                sb.append( b );
                sb.append(",");
                sb.append( l );
            }
            return sb.toString();
        }

        /**
         * @deprecated
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported remove in lucene loop.");
        }
    }

}
