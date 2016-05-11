package app.hongs.serv.medium;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.HongsError;
import app.hongs.HongsException;
import app.hongs.action.UploadHelper;
import app.hongs.db.DB;
import app.hongs.db.Model;
import app.hongs.db.Mview;
import app.hongs.dh.lucene.LuceneRecord;
import app.hongs.util.Synt;
import app.hongs.util.Tool;
import app.hongs.util.sketch.Thumb;
import app.hongs.util.verify.Wrong;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 文章记录模型
 * @author Hongs
 */
public class Article extends LuceneRecord {

    protected final String   type;
    protected       String   name;
    protected       String   snap;

    private Model            model  = null;
    private Mview            mview  = null;
    private Map<String, Map> fields = null;

    public Article(String path, String type) throws HongsException {
        super(path);
        this.type = type;
        this.name = "medium.article";
        this.snap = "upload/medium/snap";
    }

    public Article(String type) throws HongsException {
        this("medium/article/" + type, type);
    }

    public static Article getInstance(String type) throws HongsException {
        Article inst;
        String  name = Article.class.getName()+":"+type;
        Core    core = Core.getInstance(  );
        if (core.containsKey(name)) {
            inst = (Article) core.got(name);
        } else {
            inst = /*get*/new Article(type);
            core.put ( name, inst );
        }
        return inst;
    }

    @Override
    public Map<String, Map> getFields() {
        if (null != fields) {
            return  fields;
        }

        Map fs , ss;
        try {
            fs =  getMveiw( ).getFields(  );
            fs =  new HashMap ( fs);
        } catch (HongsException ex) {
            throw new HongsError.Common(ex);
        }

        // href,snap 等字段均仅需存储
        ss = (Map) fs.get( "href" );
        ss.put("__type__","stored");
        fs.put(  "href"  , ss);
        ss = (Map) fs.get( "snap" );
        ss.put("__type__","stored");
        fs.put(  "snap"  , ss);

        // name,note 等字段均需可搜索
        ss = (Map) fs.get( "name" );
        ss.put("__type__","search");
        fs.put(  "name"  , ss);
        ss = (Map) fs.get( "note" );
        ss.put("__type__","search");
        fs.put(  "note"  , ss);
        ss = (Map) fs.get( "word" );
        ss.put("__type__","search");
        fs.put(  "word"  , ss);

        // 源码数据字段, 无需额外存储
        fs.remove("html");
        fs.remove("data");

        // 追加标签字段, 用于标签筛选
        ss = (Map) fs.get( "mark" );
        if (ss == null) {
            ss = new HashMap();
        }
        ss.put("__type__","string");
        fs.put(  "mark"  , ss);

        // 追加内容字段, 用于内容搜索
        ss = (Map) fs.get( "body" );
        if (ss == null) {
            ss = new HashMap();
        }
        ss.put("__type__","search");
        fs.put(  "body"  , ss);

        // 追加分类字段, 可属多个分类
        ss = new HashMap();
        ss.put("__type__","string");
        ss.put("__required__", "0");
        ss.put("__repeated__", "1");
        fs.put("sect_id" , ss);

        // 清除不必要的关联字段
        fs.remove("sections..link_id");
        fs.remove( "species..link_id");
        fs.remove( "statics..link_id");

        fields = fs;
        return   fs;
    }

    public Model getModel() throws HongsException {
        if (null != model) {
            return  model;
        }
        model = DB.getInstance().getModel(name);
        ( ( ABaseModel ) model ).setType (type);
        return model;
    }

    public Mview getMveiw() throws HongsException {
        if (null != mview) {
            return  mview;
        }
        mview = new Mview(getModel());
        return mview;
    }

    /**
     * 添加文档
     * @param rd
     * @return ID
     * @throws HongsException
     */
    @Override
    public String add(Map rd) throws HongsException {
        String id = Core.getUniqueId();
        save(id, rd);
        return id;
    }

    /**
     * 修改文档(局部更新)
     * @param id
     * @param rd
     * @throws HongsException
     */
    @Override
    public void put(String id, Map rd) throws HongsException {
        save(id, rd);
    }

    /**
     * 设置文档(无则添加)
     * @param id
     * @param rd
     * @throws HongsException
     */
    @Override
    public void set(String id, Map rd) throws HongsException {
        save(id, rd);
    }

    /**
     * 删除文档
     * @param id
     * @throws HongsException
     */
    @Override
    public void del(String id) throws HongsException {
        save(id, null);
    }

    public void save(String id, Map rd) throws HongsException {
        Map<String, Map> fields = getFields();
        Model            model  = getModel( );

        // 删除当前数据
        if (rd == null) {
            model.del(id);
            super.del(id);
            return;
        }

        // 获取旧的数据
        Map dd = model.table.fetchCase().where("`id` = ?", id).one();
        boolean cr = dd.isEmpty();

        // 合并新旧数据
        for(String fn : fields.keySet()) {
            if (   "id".equals(fn)) {
                dd.put(fn, id);
            } else
            if ( "word".equals(fn)) {
                ASubsModel sm = (ASubsModel) model.db.getModel("species");
                String fr = Synt.declare(rd.get(fn), "");
                sm.setLink("article");
                sm.setSubs (id , Synt.asTerms(fr).toArray(new String[]{}));
                dd.put(fn, fr);
            } else
            if ( "snap".equals(fn)) {
                String fv = Synt.declare(dd.get(fn), "");
                String fr = Synt.declare(rd.get(fn), "");
                fr = saveSnap(fr, fv);
                rd.put(fn, fr);
                dd.put(fn, fr);
            } else
            if (rd.containsKey(fn)) {
                String fr = Synt.declare(rd.get(fn), "");
                dd.put(fn, fr);
            }
        }

        saveToDb(id, rd, cr);

        saveToLc(id, dd/**/);
    }

    public void saveToDb(String id, Map rd, boolean cr) throws HongsException {
        getModel();

        // 关联到分类表
        if (rd.containsKey("sections")) {
            List<Map> lst = Synt.declare(rd.get("sections"), List.class);
            if (lst != null) {
                for(Map row : lst) {
                    row.put("link", "article");
                }
            }
        }

        // 保存到数据库
        if (cr) {
            rd.put( "id" , id );
            rd.put("type",type);
            // 存入主数据
            model.table.insert/* new */(rd);
            // 存入子数据
            model.table.insertSubValues(rd);
        } else {
            model.put(id , rd );
        }
    }

    public void saveToLc(String id, Map dd) throws HongsException {
        getModel();

        // 获取查询分类
        Set<String> ss = new HashSet();
        List<Map>   ls = model.db.fetchCase()
            .from   (    model.db.getTable( "segment" ).tableName )
            .where  ("`link_id` = ? AND `link` = ?", id, "article")
            .select ("`sect_id`"   )
            .orderBy("`seria` DESC")
            .all();
        for (Map ln : ls) {
            ss.add(ln.get("sect_id").toString());
        }
        dd.put("sect_id" , ss );

        // 清理内容文本
        String hs = (String) dd.get("html");
        if (hs != null) {
            hs  = Tool.cleanHTM(hs );
        }
        dd.put("body"    , hs );

        // 保存到索引库
        dd.put("type"    ,type);
        dd.put(     "id" , id );
        dd.put(Cnst.ID_KEY,id );
        setDoc(id, map2Doc(dd));
    }

    protected String saveSnap(String nv, String ov) throws Wrong, HongsException {
        if (null == nv || "".equals(nv)) {
            return  ov;
        }

        // 移动图片
        if (nv.indexOf('/') < 0 ) {
            UploadHelper uh = new UploadHelper();
            uh.setUploadPath( snap );
            uh.setUploadHref( snap );
            uh.upload(nv);

            // 生成缩略图
            String fv;
            fv = uh.getResultPath( );
            nv = uh.getResultHref( );
            try {
                nv = Thumb.toThumbs( fv, nv )[1][0];
            } catch (IOException ex) {
                throw new HongsException.Common(ex);
            }
        }

        if (null == ov || nv.equals(ov)) {
            return  nv;
        }

        // 删除旧图
        if (ov.startsWith(snap) ) {
            File fo = new File(Core.BASE_PATH+"/"+ov);
            do {
                if (!fo.exists()) {
                    break;
                }
                fo.delete();
                fo = fo.getParentFile();
            } while (fo.getName().length() == 2
                  && fo.list(   ).length   == 0);
        }

        return  nv;
    }

}
