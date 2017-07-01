package app.hongs.dh.lucene;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.CoreConfig;
import app.hongs.CoreLocale;
import app.hongs.CoreLogger;
import app.hongs.HongsException;
import app.hongs.HongsExpedient;
import app.hongs.action.FormSet;
import app.hongs.dh.FormBean;
import app.hongs.dh.IEntity;
import app.hongs.dh.ITrnsct;
import app.hongs.dh.lucene.field.*;
import app.hongs.dh.lucene.query.*;
import app.hongs.dh.lucene.value.*;
import app.hongs.util.Data;
import app.hongs.util.Dict;
import app.hongs.util.Synt;
import app.hongs.util.Tool;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BoostQuery;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

/**
 * Lucene 记录模型
 *
 * 可选字段配置参数:
 *  lucene-fieldtype    Lucene 的字段类型(string,search,stored,int,long,float,double)
 *  lucene-tokenizer
 *  lucene-char-filter
 *  lucene-token-filter
 *  lucene-find-filter
 *  lucene-query-filter
 *
 * @author Hongs
 */
public class LuceneRecord extends FormBean implements IEntity, ITrnsct, Cloneable, AutoCloseable {

    protected boolean TRNSCT_MODE = false;
    protected boolean OBJECT_MODE = false;

    private IndexSearcher finder  = null ;
    private IndexReader   reader  = null ;
    private IndexWriter   writer  = null ;
    private String        dbpath  = null ;

    /**
     * 构造方法
     * @param path 存储路径, 可覆盖 getDataPath
     * @param form 字段配置, 可覆盖 getFields
     * @throws HongsException
     */
    public LuceneRecord(String path, Map form)
    throws HongsException {
        // 可在表单配置中指定数据路径
        if (form != null) {
            Map c = (Map) form.get("@");
            if (c!= null) {
                String p = (String) c.get("data-path");
                if (p != null && p.length() != 0) {
                    path = p;
                }
            }
        }
        super.setFields(form);

        // 保存路径
        if (path != null) {
            Map m = new HashMap();
            m.put("CORE_PATH", Core.CORE_PATH);
            m.put("CONF_PATH", Core.CORE_PATH);
            m.put("DATA_PATH", Core.DATA_PATH);
            path = Tool.inject(path, m);
            if (! new File(path).isAbsolute()) {
               path = Core.DATA_PATH + "/lucene/" + path;
            }
        }
        this.dbpath = path;

        // 模式标识
        CoreConfig  conf = CoreConfig.getInstance( );
        this.TRNSCT_MODE = Synt.declare(
            Core.getInstance().got(Cnst.TRNSCT_MODE),
            conf.getProperty("core.in.trnsct.mode", false));
        this.OBJECT_MODE = Synt.declare(
            Core.getInstance().got(Cnst.OBJECT_MODE),
            conf.getProperty("core.in.object.mode", false));
    }

    public LuceneRecord(String path)
    throws HongsException {
        this(path, null);
    }

    public LuceneRecord(  Map  form)
    throws HongsException {
        this(null, form);
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
        LuceneRecord  inst;
        Core   core = Core.getInstance();
        String name = LuceneRecord.class.getName( ) + ":" +  conf + "." + form;
        if ( ! core.containsKey( name )) {
            String path = conf + "/" +  form;
            String canf = FormSet.hasConfFile(path) ? path : conf ;
            Map    farm = FormSet.getInstance(canf).getForm( form);
            inst =  new LuceneRecord(path , farm);
            core.put( name, inst );
        } else {
            inst =  (LuceneRecord) core.got(name);
        }
        return inst;
    }

    public String getDataPath() {
        if (null != dbpath) {
            return  dbpath;
        }
        throw new NullPointerException("Data path is not set");
    }

    //** 实体方法 **/

    /**
     * 获取数据
     *
     * 以下参数为特殊参数, 可在 default.properties 中配置:
     * id   ID, 仅指定单个 id 时则返回详情(info)
     * rn   行数, 明确指定为 0 则不分页
     * gn   分页
     * pn   页码
     * wd   搜索
     * ob   排序
     * rb   字段
     * or   多组"或"关系条件
     * ar   串联多组关系条件
     * sr   附加多组"或"关系, LuceneRecord 特有
     * 请注意尽量避免将其作为字段名(id,wd除外)
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

        // 获取行数, 默认依从配置
        int rn;
        if (rd.containsKey(Cnst.RN_KEY)) {
            rn = Synt.declare(rd.get(Cnst.RN_KEY), 0);
        } else {
            rn = CoreConfig.getInstance().getProperty("fore.rows.per.page", Cnst.RN_DEF);
        }

        // 指定行数 0, 则走 getAll
        if (rn == 0) {
            Map  data = new HashMap();
            List list = getAll(rd);
            data.put("list", list);
            return data;
        }

        // 获取链数, 默认依从配置
        int gn;
        if (rd.containsKey(Cnst.GN_KEY)) {
            gn = Synt.declare(rd.get(Cnst.GN_KEY), 0);
        } else {
            gn = CoreConfig.getInstance().getProperty("fore.pags.for.page", Cnst.GN_DEF);
        }

        // 获取页码, 计算查询区间
        int pn = Synt.declare(rd.get(Cnst.PN_KEY), 1);
        if (pn < 1) pn = 1;
        if (gn < 1) gn = 1;
        int minPn = pn - (gn / 2 );
        if (minPn < 1)   minPn = 1;
        int maxPn = gn + minPn - 1;
        int limit = rn * maxPn + 1;
        int minRn = rn * (pn - 1 );
        int maxRn = rn + minRn;

        // 获取列表
        List list = getAll(rd, limit, minRn, maxRn);
        int rc = (int) list.remove(0);
        int pc = (int) Math.ceil( (double) rc / rn);

        // 记录分页
        Map  resp = new HashMap();
        Map  page = new HashMap();
        resp.put("list", list);
        resp.put("page", page);
        page.put("page", pn);
        page.put("pags", gn);
        page.put("rows", rn);
        page.put("pagecount", pc);
        page.put("rowscount", rc);
        page.put("uncertain", rc == limit); // 为 true 表示总数不确定
        if (rc == 0) {
            page.put("ern", 1);
        } else
        if (list.isEmpty()) {
            page.put("ern", 2);
        }

        return  resp;
    }

    /**
     * 创建记录
     * @param rd
     * @return id,name等(由dispCols指定)
     * @throws HongsException
     */
    @Override
    public Map create(Map rd) throws HongsException {
        String id = add(rd);
        Set<String> fs = getListable();
        if (fs != null && !fs.isEmpty()) {
            Map sd = new LinkedHashMap();
            for(String  fn : getListable()) {
            if (  !  fn.contains( "." )) {
                sd.put( fn , rd.get(fn));
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
        Set<String> ids = Synt.declare(rd.get(Cnst.ID_KEY), new HashSet());
        Map         wh  = Synt.declare(rd.get(Cnst.WR_KEY), new HashMap());
        for(String  id  : ids) {
            if(!permit(wh,id)) {
                throw new HongsException(0x1096, "Can not update for id '"+id+"'");
            }
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
        Set<String> ids = Synt.declare(rd.get(Cnst.ID_KEY), new HashSet());
        Map         wh  = Synt.declare(rd.get(Cnst.WR_KEY), new HashMap());
        for(String  id  : ids) {
            if(!permit(wh,id)) {
                throw new HongsException(0x1097, "Can not delete for id '"+id+"'");
            }
            del(id /**/ );
        }
        return ids.size();
    }

    /**
     * 确保操作合法
     * @param wh
     * @param id
     * @return
     * @throws HongsException
     */
    protected boolean permit(Map wh, String id) throws HongsException {
        if (id == null || "".equals(id)) {
            throw new NullPointerException("Param id for permit can not be empty");
        }
        if (wh == null) {
            throw new NullPointerException("Param wh for permit can not be null.");
        }
        Set<String> rb ;
        wh = new HashMap(wh);
        rb = new HashSet(  );
        rb.add( "id"  );
        wh.put(Cnst.ID_KEY, id);
        wh.put(Cnst.RB_KEY, rb);
        wh = getOne(wh);
        return wh != null && !wh.isEmpty();
    }

    //** 模型方法 **/

    /**
     * 添加文档
     * @param rd
     * @return ID
     * @throws HongsException
     */
    public String add(Map rd) throws HongsException {
        String id = Synt.declare(rd.get(Cnst.ID_KEY), String.class);
        if (id != null && id.length() != 0) {
            throw new HongsException.Common("Id can not set in add");
        }
        id = Core.newIdentity();
        rd.put(Cnst.ID_KEY, id);
        addDoc(map2Doc(rd));
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
            doc =  new Document();
        } else {
            /**
             * 实际运行中发现
             * 直接往取出的 doc 里设置属性, 会造成旧值的索引丢失
             * 故只好转换成 map 再重新设置, 这样才能确保索引完整
             * 但那些 Store=NO 的数据将无法设置
             */
            setView(new HashMap());
            Map  md = doc2Map(doc);
            md.putAll(rd);
            rd = md;
            doc =  new Document();
        }
        rd.put(Cnst.ID_KEY, id);
        docAdd(doc, rd);
        setDoc(id, doc);
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
            setView(new HashMap());
            Map  md = doc2Map(doc);
            md.putAll(rd);
            rd = md;
        }
        rd.put(Cnst.ID_KEY, id);
        docAdd(doc, rd);
        setDoc(id, doc);
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
           setView(new HashMap());
            return doc2Map( doc );
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
        List list = new LinkedList();
        while  (  roll.hasNext()) {
            list.add(roll.next());
        }
        return list;
    }

    /**
     * 获取部分文档
     * @param rd
     * @param total 总数限制
     * @param begin 起始位置
     * @param end   结束位置(不含), 给定 0 则取到最后
     * @return      首位为实际总数, 请用 .poll() 取出
     * @throws HongsException
     */
    public List getAll(Map rd, int total, int begin, int end) throws HongsException {
        Loop roll = search(rd, begin, total - begin);
        List list = new LinkedList();
        int  idx  = begin ;
        if ( end == 0 ) {
             end  = total - begin;
        }
        list.add( roll.size(  ) );
        while  (  roll.hasNext()) {
            list.add(roll.next());
            if (  ++idx >= end  ) {
                break ;
            }
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
        Query q = getQuery(rd);
        Sort  s = getSort (rd);
                  setView (rd);
        Loop  r = new Loop(this, q, s, begin, limit);

        if (0 < Core.DEBUG && 8 != (8 & Core.DEBUG)) {
            CoreLogger.debug("LuceneRecord.search: " + r.toString());
        }

        return r ;
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
            iw.updateDocument (new Term(Cnst.ID_KEY, id), doc);
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
            iw.deleteDocuments(new Term(Cnst.ID_KEY, id) /**/);
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
                Query  qq = new TermQuery(new Term(Cnst.ID_KEY, id));
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

    public Document map2Doc(Map map) throws HongsException {
        Document doc = new Document();
        docAdd(doc, map);
        return doc;
    }

    public Map doc2Map(Document doc) {
        Map map = new LinkedHashMap();
        mapAdd(map, doc);
        return map;
    }

    //** 事务方法 **/

    /**
     * 初始化读操作
     * @throws HongsException
     */
    public void init() throws HongsException {
        if (reader != null) {
            return;
        }

        String dbpath = getDataPath();

        try {
            // 索引目录不存在则先写入一个并删除
            if (! (new File(dbpath)).exists() ) {
                String id = Core.newIdentity( );
                Map rd = new HashMap( );
                rd.put(Cnst.ID_KEY, id);
                addDoc(map2Doc(rd));
                delDoc(id);
                commit(  );
            }

            Path p = Paths.get(dbpath );
            Directory dir = FSDirectory.open(p);

            reader = DirectoryReader.open (dir);
            finder = new IndexSearcher (reader);
        } catch (IOException x) {
            throw new HongsException.Common (x);
        }

        if (0 < Core.DEBUG && 4 != (4 & Core.DEBUG)) {
            CoreLogger.trace("Connect to lucene reader, data path: " + dbpath);
        }
    }

    /**
     * 连接写数据库
     * @throws HongsException
     */
    public void open() throws HongsException {
        if (writer != null && writer.isOpen()) {
            return;
        }

        String dbpath = getDataPath();

        try {
            IndexWriterConfig iwc = new IndexWriterConfig(getAnalyzer());
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);

            Path d = Paths.get(dbpath );
            Directory dir = FSDirectory.open(d);

            writer = new IndexWriter(dir , iwc);
        } catch (IOException x) {
            throw new HongsException.Common (x);
        }

        if (0 < Core.DEBUG && 4 != (4 & Core.DEBUG)) {
            CoreLogger.trace("Connect to lucene writer, data path: " + dbpath);
        }
    }

    /**
     * 销毁读写连接
     */
    @Override
    public void close() {
        if (writer != null) {
            // 默认退出时提交
            if (TRNSCT_MODE) {
                try {
                    try {
                        commit();
                    } catch (Error er) {
                        revert();
                        throw er;
                    }
                } catch (Error e) {
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
                writer = null;
            }
        }

        if (reader != null) {
            try {
                reader.close();
            } catch (IOException x) {
                CoreLogger.error(x);
            } finally {
                reader = null;
            }
        }

        if (0 < Core.DEBUG && 4 != (4 & Core.DEBUG)) {
            CoreLogger.trace("Close lucene connection, data path: " + getDataPath());
        }
    }

    @Override
    public LuceneRecord clone() {
        try {
            return (LuceneRecord) super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new InternalError(ex.getMessage());
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
        if (writer == null) {
            return;
        }
        TRNSCT_MODE = Synt.declare(Core.getInstance().got(Cnst.TRNSCT_MODE), false);
        try {
            writer.commit(  );
        } catch (IOException ex) {
            throw new HongsExpedient(0x102c, ex);
        }
    }

    /**
     * 回滚操作
     */
    @Override
    public void revert() {
        if (writer == null) {
            return;
        }
        TRNSCT_MODE = Synt.declare(Core.getInstance().got(Cnst.TRNSCT_MODE), false);
        try {
            writer.rollback();
        } catch (IOException ex) {
            throw new HongsExpedient(0x102d, ex);
        }
    }

    //** 底层方法 **/

    public IndexSearcher getFinder() throws HongsException {
        init();
        return finder;
    }

    public IndexReader getReader() throws HongsException {
        init();
        return reader;
    }

    public IndexWriter getWriter() throws HongsException {
        open();
        return writer;
    }

    /**
     * 查询分析
     * @param rd
     * @return
     * @throws HongsException
     */
    public Query getQuery(Map rd) throws HongsException {
        Map<String, Map> fields = getFields();
        BooleanQuery.Builder qr = new BooleanQuery.Builder();

        for (Object o : rd.entrySet()) {
            Map.Entry e = (Map.Entry) o;
            Object fv = e.getValue( );
            String fn = (String) e.getKey();

            // 自定义查询
            if (queried(qr, fn, fv )) {
                continue;
            }

            Map m = (Map ) fields.get( fn );
            if (m == null) {
                continue;
            }
            if (filtable(m)==false) {
                continue;
            }

            IQuery aq;
            String t = getTypeMutant(m);
            if (   "int".equals(t)) {
                aq = new IntQuery();
            } else
            if (  "long".equals(t)) {
                aq = new LongQuery();
            } else
            if ( "float".equals(t)) {
                aq = new FloatQuery();
            } else
            if ("double".equals(t)) {
                aq = new DoubleQuery();
            } else
            if (  "date".equals(t)) {
                aq = new LongQuery();
            } else
            if ("string".equals(t)) {
                aq = new StringQuery();
            } else
            if ("search".equals(t)) {
                aq = new SearchQuery();
            } else
            {
                continue;
            }

            qryAdd(qr, fn, fv, aq);
        }

        // 关键词
        if (rd.containsKey(Cnst.WD_KEY)) {
            Object fv = rd.get (Cnst.WD_KEY);
                   fv = Synt.asserts(fv, "");
            Set<String> fs = getFindable(  );

            if (fv != null && !"".equals(fv)) {
                if (fs.size() > 1) {
                    // 当设置了多个搜索字段时
                    // 将条件整理为: +(fn1:xxx fn2:xxx)

                    Map fw = new HashMap(  );
                    fw.put(Cnst.OR_REL , fv);
                    BooleanQuery.Builder qx = new BooleanQuery.Builder();

                    for(String fk : fs) {
                        qryAdd(qx , fk , fw , new SearchQuery());
                    }

                    qr.add(qx.build(), BooleanClause.Occur.MUST);
                } else {
                    for(String fk : fs) {
                        qryAdd(qr , fk , fv , new SearchQuery());
                    }
                }
            }
        }

        // 或条件
        if (rd.containsKey(Cnst.OR_KEY)) {
            BooleanQuery.Builder qx = new BooleanQuery.Builder( );
            Set<Map> set = Synt.declare(rd.get(Cnst.OR_KEY), Set.class);
            for(Map  map : set) {
                qx.add(getQuery(map), BooleanClause.Occur.SHOULD);
            }
            qr.add(qx.build(), BooleanClause.Occur.MUST);
        }

        // 附条件
        if (rd.containsKey(Cnst.SR_KEY)) {
            Set<Map> set = Synt.declare(rd.get(Cnst.SR_KEY), Set.class);
            for(Map  map : set) {
                qr.add(getQuery(map), BooleanClause.Occur.SHOULD);
            }
        }

        // 并条件
        if (rd.containsKey(Cnst.AR_KEY)) {
            Set<Map> set = Synt.declare(rd.get(Cnst.AR_KEY), Set.class);
            for(Map  map : set) {
                qr.add(getQuery(map), BooleanClause.Occur.MUST);
            }
        }

        // 没有条件则查询全部
        BooleanQuery query = qr.build(   );
        if ( query.clauses( ).isEmpty( ) ) {
            return new MatchAllDocsQuery();
        }

        return query;
    }

    /**
     * 排序分析
     * @param rd
     * @return
     * @throws HongsException
     */
    public Sort getSort(Map rd) throws HongsException {
        Map<String, Map> fields = getFields();
        List<SortField> of = new LinkedList();
        Object xb = rd.get(Cnst.OB_KEY);
        Set<String> ob  =  xb  !=  null
                  ? Synt.asTerms ( xb )
                  : new LinkedHashSet();

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
            if (sorted(of, fn, rv)) {
                continue;
            }

            Map m = (Map ) fields.get ( fn);
            if (m == null) {
                continue;
            }
            if (sortable(m)==false) {
                continue;
            }

            SortField.Type st;
            String t = getTypeMutant(m);
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
            if ("string".equals(t)) {
                st = SortField.Type.STRING;
            } else
            if (  "date".equals(t)) {
                st = SortField.Type.LONG;
            } else
            if ("sorted".equals(t)) {
                st = SortField.Type.LONG;
            } else
            {
                continue;
            }

            /**
             * 因为 Lucene 5 必须使用 DocValues 才能排序
             * 在更新数据时, 默认有加 '.' 打头的排序字段
             */
            of.add(new SortField("." + fn , st , rv));
        }

        // 未指定则按文档顺序
        if (of.isEmpty()) {
            of.add(SortField.FIELD_DOC);
        }

        return new Sort(of.toArray(new SortField[0]));
    }

    /**
     * 返回字段
     * @param rd
     */
    public void setView(Map rd) {
        Object fz = rd.get(Cnst.RB_KEY);
        Set<String> fs = fz != null
                  ? Synt.asTerms ( fz )
                  : new LinkedHashSet();
        Map<String, Map> fields = getFields();

        if (fs != null && !fs.isEmpty()) {
            Set<String> cf = new HashSet();
            Set<String> sf = new HashSet();
            for (String fn : fs) {
                if (fn.startsWith("-")) {
                    fn= fn.substring(1);
                    cf.add(fn);
                } else {
                    sf.add(fn);
                }
            }
            if (!sf.isEmpty()) {
                cf.addAll(fields.keySet());
                cf.removeAll(sf);
            }
            cf.add("@"); // Skip form conf;

            for(Map.Entry<String, Map> me : fields.entrySet()) {
                Map fc = me.getValue();
                String f = me.getKey();
                fc.put   ( "--unwanted--" , cf.contains(f) || unstored(fc));
            }
        } else {
            for(Map.Entry<String, Map> me : fields.entrySet()) {
                Map fc = me.getValue();
                fc.remove( "--unwanted--" );
            }
        }
    }

    //** 底层工具 **/

    /**
     * 写入分析器
     * @return
     * @throws HongsException
     */
    protected Analyzer getAnalyzer() throws HongsException {
        Map<String, Analyzer> az = new HashMap();
        Map<String, Map>  fields = getFields(  );
        Analyzer ad = new StandardAnalyzer();
        for(Object ot : fields.entrySet( ) ) {
            Map.Entry et = (Map.Entry) ot;
            Map    fc = (Map ) et.getValue();
            String fn = (String) et.getKey();
            String ft =  getTypeMutant(fc);
            if ("search".equals( ft )) {
                az.put(fn, getAnalyzer(fc, false));
            }
        }
        return new PerFieldAnalyzerWrapper(ad, az);
    }

    /**
     * 构建分析器
     * @param fc 字段配置
     * @param iq 是否查询
     * @return
     * @throws HongsException
     */
    protected Analyzer getAnalyzer(Map fc, boolean iq) throws HongsException {
        try {
            CustomAnalyzer.Builder cb = CustomAnalyzer.builder();
            String kn, an, ac; Map oc;

            // 分词器
            an = Synt.declare(fc.get("lucene-tokenizer"), "");
            if (!"".equals(an)) {
                int p  = an.indexOf('{');
                if (p != -1) {
                    ac = an.substring(p);
                    an = an.substring(0, p - 1).trim( );
                    oc = Synt.declare(Data.toObject(ac), Map.class);
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
                if (iq) {
                    if (kn.startsWith("lucene-find-filter")) {
                        an = (String) et2.getValue();
                        an = an.trim();
                        if ("".equals(an)) {
                            continue;
                        }
                        int p  = an.indexOf('{');
                        if (p != -1) {
                            ac = an.substring(p);
                            an = an.substring(0, p - 1).trim( );
                            oc = Synt.declare(Data.toObject(ac), Map.class);
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
                            an = an.substring(0, p - 1).trim( );
                            oc = Synt.declare(Data.toObject(ac), Map.class);
                            cb.addTokenFilter(an, oc);
                        } else {
                            cb.addTokenFilter(an/**/);
                        }
                    }
                } else {
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
                            oc = Synt.declare(Data.toObject(ac), Map.class);
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
                            oc = Synt.declare(Data.toObject(ac), Map.class);
                            cb.addTokenFilter(an, oc);
                        } else {
                            cb.addTokenFilter(an/**/);
                        }
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
     * 获取时间格式
     * @param fc
     * @return
     */
    protected DateFormat getDateFormat(Map fc) {
        String  fm = Synt.asserts(fc.get( "format" ), "");
        if ( "".equals(fm)) {
                fm = Synt.declare(fc.get("__type__"), "datetime");
                fm = CoreLocale.getInstance()
                     .getProperty("core.default."+ fm +".format");
            if (fm == null) {
                fm = "yyyy/MM/dd HH:mm:ss";
            }
        }
        SimpleDateFormat fd = new SimpleDateFormat(fm);
        fd.setTimeZone(Core.getTimezone());
        return  fd;
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
    protected String getTypeMutant(Map fc) {
        String t = Synt.declare(fc.get("lucene-type"), String.class);
        if (null != t) {
            if (t.equals("text")) {
                return "search"; // Lucene 的 text 在这里叫 search
            }
            return  t;
        }

        t = (String) fc.get("__type__");
        Set <String> ks;

        //** 先查特有的 **/

        ks = getSaveTypes("search");
        if (ks != null && ks.contains(t) ) {
            return "search";
        }

        ks = getSaveTypes("stored");
        if (ks != null && ks.contains(t) ) {
            return "stored";
        }

        ks = getSaveTypes("sorted");
        if (ks != null && ks.contains(t) ) {
            return "sorted";
        }

        ks = getSaveTypes("object");
        if (ks != null && ks.contains(t) ) {
            return "object";
        }

        //** 再查一般的 **/

        ks = getSaveTypes("number");
        if (ks != null && ks.contains(t) ) {
            return Synt.declare(fc.get("type"), "double");
        }

        ks = getSaveTypes("string");
        if (ks != null && ks.contains(t) ) {
            return "string";
        }

        ks = getSaveTypes( "date" );
        if (ks != null && ks.contains(t) ) {
            return  "date" ;
        }

        return t;
    }

    protected boolean sortable(Map fc) {
        return getSortable().contains(Synt.asserts(fc.get("__name__"), ""));
    }

    protected boolean filtable(Map fc) {
        return getFiltable().contains(Synt.asserts(fc.get("__name__"), ""));
    }

    protected boolean repeated(Map fc) {
        return Synt.asserts(fc.get("__repeated__"), false);
    }

    protected boolean unstored(Map fc) {
        return Synt.asserts(fc.get(  "unstored"  ), false);
    }

    protected boolean unwanted(Map fc) {
        return Synt.asserts(fc.get("--unwanted--"), false);
    }

    protected boolean ignored (Map fc, String k) {
        return "".equals( k)  ||  "@".equals( k)
            || "Ignore".equals(fc.get( "rule" ));
    }

    protected boolean queried (BooleanQuery.Builder qb, String k, Object v) {
        return false;
    }

    protected boolean sorted  (List <  SortField  > sf, String k,boolean r) {
        return false;
    }

    protected void mapAdd(Map map, Document doc) {
        Map<String, Map> fields = getFields( );
        for(Object o : fields.entrySet()) {
            Map.Entry e = (Map.Entry) o;
            Map    m = (Map) e.getValue();
            String k = (String)e.getKey();

            if (unwanted(m)
            ||  unstored(m)
            ||  ignored (m, k)) {
                continue;
            }

            IValue  v ;
            Object  u ;
            boolean r = repeated(m);
            String  t = getTypeMutant(m);
            IndexableField[] fs = doc.getFields(k);

            if ("sorted".equals(t)) {
                continue; // 排序字段没有可见值
            } else
            if (  "date".equals(t)) {
                // 时间戳转 Date 对象时需要乘以 1000
                String typ = Synt.asserts(m.get("type"), "");
                int    mul = "datestamp".equals( typ  )
                          || "timestamp".equals( typ  )
                           ? 1000 : 1;

                if (OBJECT_MODE) {
                    if ("time".equals(typ) || "timestamp".equals(typ)) {
                        v = new NumberValue();
                        u =  0  ;
                    } else {
                        v = new DatimeValue();
                        u = null;
                        ((DatimeValue) v).mul = mul;
                    }
                } else {
                    if ("time".equals(typ) || "timestamp".equals(typ)) {
                        v = new NumeraValue();
                        u = "0" ;
                    } else {
                        v = new DatextValue();
                        u =  "" ;
                        ((DatextValue) v).mul = mul;
                        ((DatextValue) v).sdf = getDateFormat(m);
                    }
                }
            } else
            if (   "int".equals(t)
            ||    "long".equals(t)
            ||   "float".equals(t)
            ||  "double".equals(t)
            ||  "number".equals(t)) {
                if (OBJECT_MODE) {
                    v = new NumberValue();
                    u =  0 ;
                } else {
                    v = new NumeraValue();
                    u = "0";
                }
            } else
            if ("object".equals(t)) {
                v = new ObjectValue();
                u = new HashMap( );
            } else
            {
                v = new StringValue();
                u = "";
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
                    map.put(k , v.get( fs[0] ) );
                } else {
                    map.put(k , u);
                }
            }
        }
    }

    protected void docAdd(Document doc, Map map) {
        Map<String, Map> fields = getFields();
        for(Object o : fields.entrySet()) {
            Map.Entry e = (Map.Entry) o;
            Map    m = (Map) e.getValue();
            String k = (String)e.getKey();
            Object v = Dict.getParam(map , k);

            if (null == v
            ||  ignored(m , k)) {
                continue;
            }

            IField  f ;
            boolean s = sortable(m);
            boolean q = filtable(m);
            boolean u = unstored(m);
            boolean r = repeated(m);
            boolean g = true; // 是否要存储, 数值类型过滤与存储是分离的, 故其等同于 !unstored
            String  t = getTypeMutant(m);

            /**
             * 日期和排序均是长整型
             * 排序字段仅排序不存储
             */
            if (  "date".equals (t)) {
                t = "long";
            }
            if ("sorted".equals (t)) {
                t = "long";
                s =  true ;
                u =  true ;
            }

            if (   "int".equals (t)) {
                f = new IntField( );
                g = !u;
            } else
            if (  "long".equals (t)) {
                f = new LongField();
                g = !u;
            } else
            if ( "float".equals (t)) {
                f = new FloatField();
                g = !u;
            } else
            if ("double".equals (t)) {
                f = new DoubleField();
                g = !u;
            } else
            if ("string".equals (t)) {
                f = new StringFiald();
                q = false; // 字符类型自带筛选
            } else
            if ("search".equals (t)) {
                f = new SearchFiald();
                q = false; // 文本类型自带筛选
                s = false; // 文本类型无法排序
            } else
            if ("object".equals (t)) {
                f = new ObjectFiald();
                q = false; // 对象类型无法筛选
                s = false; // 对象类型无法排序
            } else
            {
                f = new StoredFiald();
                q = false; // 对象类型无法筛选
                s = false; // 对象类型无法排序
            }

            doc.removeFields(k);

            if (r) {
                if (g) {
                    if (v instanceof Object[ ] ) {
                        for (Object w: (Object[ ] ) v) {
                            doc.add(f.get(k, w, u));
                        }
                    } else
                    if (v instanceof Collection) {
                        for (Object w: (Collection) v) {
                            doc.add(f.get(k, w, u));
                        }
                    } else
                    {
                        Set a = Synt.declare(v, Set.class);
                        for (Object w: a) {
                            doc.add(f.get(k, w, u));
                        }
                        v = a;
                    }
                }

                // 筛选和排序前去重
                Set a = Synt.declare(v, Set.class);
                if (q && a != null && !a.isEmpty()) {
                    for (Object w: a) {
                        doc.add(f.got(k, w));
                    }
                }
                if (s && a != null && !a.isEmpty()) {
                    for (Object w: a) {
                        doc.add(f.srt(k, w));
                    }
                }
            } else
            {
                doc.add(f.get( k, v, u));

                if (q) {
                    doc.add(f.got(k, v));
                }
                if (s) {
                    doc.add(f.srt(k, v));
                }
            }
        }
    }

    /**
     * 组织查询条件
     *
     * 操作符:
     * !eq 等于
     * !ne 不等于
     * !lt 小于
     * !le 小于或等于
     * !gt 大于
     * !ge 大于或等于
     * !rg 区间
     * !in 包含
     * !ni 不包含
     * 以下为 Lucene 特有的操作符:
     * !or 或匹配, 有则优先
     * !oi 或包含, 有则优先
     * !ai 全包含, 此为目标真子集
     * !wt 优先度, 设定查询的权重
     * 注意: 默认情况下查询参数不给值则忽略, 如果指定了操作符则匹配空串
     *
     * @param qry
     * @param k
     * @param v
     * @param q
     * @throws HongsException
     */
    protected void qryAdd(BooleanQuery.Builder qry, String k, Object v, IQuery q)
    throws HongsException {
        Map m;
        if (v instanceof Map) {
            m = new HashMap();
            m.putAll((Map) v);
        } else {
            if (null==v || "".equals(v)) {
                return ;
            }
            m = new HashMap();
            if (v instanceof Collection) {
                Collection c = (Collection) v;
                    c.remove("");
                if (c.isEmpty()) {
                    return;
                }
                m.put(Cnst.IN_REL, c);
            } else
            {
                m.put(Cnst.EQ_REL, v);
            }
        }

        // 对 text 类型指定分词器
        if (q instanceof SearchQuery) {
            Map<String, Map> fields = getFields( );
            Map         fc = ( Map ) fields.get(k);
            SearchQuery sq = ( SearchQuery ) q;
            sq.analyzer(getAnalyzer(fc,true) );

            // 额外的一些细微配置
            sq.phraseSlop (Synt.declare(fc.get("lucene-parser-phraseSlop" ), Integer.class));
            sq.fuzzyPreLen(Synt.declare(fc.get("lucene-parser-fuzzyPreLen"), Integer.class));
            sq.fuzzyMinSim(Synt.declare(fc.get("lucene-parser-fuzzyMinSim"),   Float.class));
            sq.advanceAnalysisInUse(Synt.declare(fc.get("lucene-parser-advanceAnalysisInUse"), Boolean.class));
            sq.defaultOperatorIsAnd(Synt.declare(fc.get("lucene-parser-defaultOperatorIsAnd"), Boolean.class));
            sq.allowLeadingWildcard(Synt.declare(fc.get("lucene-parser-allowLeadingWildcard"), Boolean.class));
            sq.lowercaseExpandedTerms(Synt.declare(fc.get("lucene-parser-lowercaseExpandedTerms"), Boolean.class));
            sq.enablePositionIncrements(Synt.declare(fc.get("lucene-parser-enablePositionIncrements"), Boolean.class));
        }

        float bst = 1F;
        BooleanQuery.Builder src = null;
        if (m.containsKey(Cnst.WT_REL)) {
            Object n = m.remove(Cnst.WT_REL);
            bst = Synt.declare (n , 1F);
            src = qry;
            qry = new BooleanQuery.Builder();
        }

        if (m.containsKey(Cnst.EQ_REL)) {
            Object n = m.remove(Cnst.EQ_REL);
            qry.add(q.get(k, n), BooleanClause.Occur.MUST);
        }

        if (m.containsKey(Cnst.NE_REL)) {
            Object n = m.remove(Cnst.NE_REL);
            qry.add(q.get(k, n), BooleanClause.Occur.MUST_NOT);
        }

        if (m.containsKey(Cnst.OR_REL)) {
            Object n = m.remove(Cnst.OR_REL);
            qry.add(q.get(k, n), BooleanClause.Occur.SHOULD);
        }

        if (m.containsKey(Cnst.IN_REL)) { // In
            BooleanQuery.Builder qay = new BooleanQuery.Builder();
            Set a = Synt.declare(m.remove(Cnst.IN_REL), new HashSet());
            for(Object x : a) {
                qay.add(q.get(k, x), BooleanClause.Occur.SHOULD);
            }
            qry.add(qay.build(), BooleanClause.Occur.MUST);
        }

        if (m.containsKey(Cnst.AI_REL)) { // All In
            Set a = Synt.declare(m.remove(Cnst.AI_REL), new HashSet());
            for(Object x : a) {
                qry.add(q.get(k, x), BooleanClause.Occur.MUST);
            }
        }

        if (m.containsKey(Cnst.NI_REL)) { // Not In
            Set a = Synt.declare(m.remove(Cnst.NI_REL), new HashSet());
            for(Object x : a) {
                qry.add(q.get(k, x), BooleanClause.Occur.MUST_NOT);
            }
        }

        if (m.containsKey(Cnst.OI_REL)) { // Or In
            Set a = Synt.declare(m.remove(Cnst.OI_REL), new HashSet());
            for(Object x : a) {
                qry.add(q.get(k, x), BooleanClause.Occur.SHOULD);
            }
        }

        //** 区间查询 **/

        Object  n, x;
        boolean l, g;

        if (m.containsKey(Cnst.RG_REL)) {
            Object[] a = Synt.asRange(m.remove(Cnst.RG_REL));
            if (null  !=  a) {
                n = a[0]; l = (boolean) a[2];
                x = a[1]; g = (boolean) a[3];
            } else {
                n = null; l = false;
                x = null; g = false;
            }
        } else {
            if (m.containsKey(Cnst.GE_REL)) {
                n = m.remove (Cnst.GE_REL); l = true ;
            } else
            if (m.containsKey(Cnst.GT_REL)) {
                n = m.remove (Cnst.GT_REL); l = false;
            } else
            {
                n = null; l = false;
            }

            if (m.containsKey(Cnst.LE_REL)) {
                x = m.remove (Cnst.LE_REL); g = true ;
            } else
            if (m.containsKey(Cnst.LT_REL)) {
                x = m.remove (Cnst.LT_REL); g = false;
            } else
            {
                x = null; g = false;
            }
        }

        if (n != null || x != null) {
            qry.add(q.get(k, n, x, l, g), BooleanClause.Occur.MUST);
        }

        //** 其他查询 **/

        if (!m.isEmpty()) {
            Set s = new HashSet();
            s.addAll(m.values( ));
            qryAdd(qry, k, s, q );
        }

        //** 权重设置 **/

        if (src != null ) {
            BooleanQuery qay = qry.build();
            if (qay.clauses().size() > 0 ) {
                src.add(new BoostQuery(qay, bst), BooleanClause.Occur.MUST);
            }
        }
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
         * @param b 起始偏移
         * @param l 查询限额
         */
        public Loop(LuceneRecord that, Query q, Sort s, int b, int l) {
            this.that = that;
            this.docs = null;
            this.doc  = null;
            this.q    = q;
            this.s    = s;
            this.b    = b;

            // 是否获取全部
            if ( l  ==  0) {
                 l = 1000;
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
                throw ex.toExpedient(   );
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
                throw new HongsExpedient.Common(ex);
            }
        }

        @Override
        public Map next() {
            if ( i >= h ) {
                throw new NullPointerException("hasNext not run?");
            }
            try {
                /*Read*/ doc = docs[i++];
                Document dox = reader.document( doc.doc );
                return that.doc2Map(dox);
            } catch (IOException ex) {
                throw new HongsExpedient.Common(ex);
            }
        }

        /**
         * 获取命中总数
         * 注意:
         * 初始化时 l 参数为 0 (即获取全部命中)
         * 则在全部循环完后获取到的数值才是对的
         * 但其实此时完全可以直接计算循环的次数
         * 此方法主要用于分页时获取查询命中总量
         * @return
         */
        public int size() {
            hasNext();
            return H ;
        }

        /**
         * @deprecated
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException("Not supported remove in lucene loop.");
        }

        @Override
        public String toString() {
            hasNext();
            StringBuilder sb = new StringBuilder(q.toString());
            if ( s != null ) {
                sb.append( " Sort: ");
                sb.append( s );
            }
            if ( l != 0 || b != 0 ) {
                sb.append(" Limit: ");
                sb.append( b );
                sb.append(",");
                sb.append( l );
            }
            return sb.toString();
        }
    }

}
