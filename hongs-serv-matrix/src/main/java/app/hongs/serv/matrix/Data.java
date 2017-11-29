package app.hongs.serv.matrix;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.HongsException;
import app.hongs.HongsExpedient;
import app.hongs.action.FormSet;
import app.hongs.db.DB;
import app.hongs.db.Model;
import app.hongs.db.Table;
import app.hongs.dh.search.SearchEntity;
import app.hongs.util.Dict;
import app.hongs.util.Synt;
import app.hongs.util.Tool;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.io.File;
import java.util.Collection;
import org.apache.lucene.document.Document;

/**
 * 数据存储模型
 * @author Hongs
 */
public class Data extends SearchEntity {

    protected final String comf;
    protected final String conf;
    protected final String form;
    protected long time = 0;

    /**
     * 数据实例基础构造方法
     * @param path 数据存放路径
     * @param comf 基础配置文件
     * @param conf 当前配置文件
     * @param form 表单配置名称
     * @throws HongsException
     */
    public Data(String path, String comf, String conf, String form) throws HongsException {
        super(path);
        this.comf = comf;
        this.conf = conf;
        this.form = form;
    }

    /**
     * 数据实例快捷构造方法
     * @param conf 当前配置文件
     * @param form 表单配置名称
     * @throws HongsException
     */
    public Data(String conf, String form) throws HongsException {
        this(conf.replaceFirst("^.*?/data/",        "data/"),
             conf.replaceFirst("^.*?/data/", "gerent/data/"),
             conf, form);
    }

    /**
     * 获取实例
     * 生命周期将交由 Core 维护
     * @param conf
     * @param form
     * @return
     * @throws HongsException
     */
    public static Data getInstance(String conf, String form) throws HongsException {
        Data   inst;
        Core   core = Core.getInstance();
        String name = Data.class.getName() +":"+ conf +":"+ form;
        if (core.containsKey(name)) {
            inst = (Data) core.got(name);
        } else {
            inst = new Data( conf, form);
            core.put ( name, inst);
        }
        return inst;
    }

    /**
     * 获取字段
     * 当表单不在管理区域时
     * 会用当前表单覆盖管理表单
     * 配置文件不存在则抛出异常 0x1104
     * @return
     */
    @Override
    public Map getFields() {
        try {
            return super.getFields();
        } catch (NullPointerException ex) {
            // Nothing todo
        }

        Map fields, fieldx;

        /**
         * 字段以 gerent/data 的字段为基础
         * 但可在 global/data 重设部分字段
         *
         * 配置文件不得放在资源包里面
         * 此处会校验表单文件是否存在
         */
        try {
            if (! new File(
                Core.CONF_PATH + "/"+ conf + Cnst.FORM_EXT +".xml"
            ).exists()) {
                throw new HongsExpedient(0x1104, "Data form conf '" + conf + "' is not exists")
                    .setLocalizedOptions(conf);
            }

            fields = FormSet.getInstance(conf).getForm(form);

        if (! comf.equals(conf)) {
            if (! new File(
                Core.CONF_PATH + "/"+ comf + Cnst.FORM_EXT +".xml"
            ).exists()) {
                throw new HongsExpedient(0x1104, "Data form conf '" + comf + "' is not exists")
                    .setLocalizedOptions(comf);
            }

            fieldx = FormSet.getInstance(comf).getForm(form);

            // 补充上额外的字段设置
            fieldx = new HashMap( fieldx );
            fieldx.putAll( fields );
            fields = fieldx;
        }
        } catch (HongsException ex) {
            throw ex.toExpedient( );
        }

        setFields(fields);

        return fields;
    }

    public String getFormId() {
        return Synt.declare(getParams().get("form_id"), form );
    }

    public Model getModel() throws HongsException {
        String tn = Synt.declare(getParams().get("table.name"), "matrix.data");
        if ("".equals(tn) || "none".equals(tn)) {
            return null;
        }
        return DB.getInstance("matrix").getModel(tn);
    }

    public Table getTable() throws HongsException {
        String tn = Synt.declare(getParams().get("table.name"), "matrix.data");
        if ("".equals(tn) || "none".equals(tn)) {
            return null;
        }
        return DB.getInstance("matrix").getTable(tn);
    }

    /**
     * 添加文档
     * @param rd
     * @return ID
     * @throws HongsException
     */
    @Override
    public String add(Map rd) throws HongsException {
        String id = Core.newIdentity();
        save(id, rd);
        call(id, "create");
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
        call(id, "update");
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
        call(id, "update");
    }

    /**
     * 删除文档
     * @param id
     * @throws HongsException
     */
    @Override
    public void del(String id) throws HongsException {
        save(id, null);
        call(id, "delete");
    }

    public void save(String id, Map rd) throws HongsException {
        String   fid   = getFormId();
        Table    table = getTable( );
        String   where = "`id`=? AND `form_id`=? AND `etime`=?";
        Object[] param = new String[]{id, fid, "0"};
        long     ctime = System.currentTimeMillis() / 1000;
        time  =  ctime ;

        // 删除当前数据
        if (rd == null) {
            if (table != null) {
                Map ud = new HashMap();
                ud.put("etime", ctime);
                ud.put("state",   0  );
                table.update(ud, where, param);
            }

            super.del(id);

            return;
        }

        // 获取旧的数据
        Map dd;
        if (table != null) {
            dd = table.fetchCase( )
                    .filter(where, param)
                    .select("data,ctime")
                    .one();
            if(!dd.isEmpty()) {
                if (ctime <= Synt.declare ( dd.get( "ctime" ) , 0L ) ) {
                    throw new HongsException(0x1100, "等会儿, 不要急");
                }
                dd = (Map) app.hongs.util.Data.toObject(dd.get("data").toString());
            }
        } else {
            dd = get( id );
        }

        // 合并新旧数据
        int i = 0;
        Map<String,Map> fields = getFields();
        for(String fn : fields.keySet()) {
            if (  "id". equals(fn)) {
                dd.put(fn , id);
            } else
            if (rd.containsKey(fn)) {
                Object fr = Synt.defoult(rd.get(fn), "");
                Object fo = Synt.defoult(dd.get(fn), "");
                dd.put(fn , fr);

                if (!equals(fr,fo)
                &&  !fn.equals("muid" )
                &&  !fn.equals("mtime")) {
                    i ++; // 需要排除修改环境数据
                }
            }
        }

        // 无更新不存储
        if (i == 0) {
            return;
        }

        dd.put("name", getName(dd));
        dd.put("find", getFind(dd));

        //** 保存到数据库 **/

        if (table != null) {
            Map ud = new HashMap();
            ud.put("etime", ctime);

            Map nd = new HashMap();
            nd.put("ctime", ctime);
            nd.put("etime",   0  );
            nd.put(/***/"id", id );
            nd.put("form_id", fid);
            nd.put("user_id", rd.get("user_id"));
            nd.put("name", dd.get("name"));
            nd.put("note", rd.get("note"));
            nd.put("data", app.hongs.util.Data.toString(dd));

            table.update(ud, where, param);
            table.insert(nd);
        }

        //** 保存到索引库 **/

        Document doc = new Document();
        dd.put(Cnst.ID_KEY, id);
        docAdd(doc, dd);
        setDoc(id, doc);
    }

    public void redo(String id, Map rd) throws HongsException {
        long ctime = System.currentTimeMillis() / 1000;
        long rtime = Synt.declare(rd.get("rtime"), 0L);

        //** 获取旧的数据 **/

        String   fid   = getFormId();
        Table    table = getTable( );
        String   where = "`id`=? AND `form_id`=? AND `ctime`=?" ;
        Object[] param = new Object[] {id, fid, rtime};

        if (table == null) {
            throw new HongsException(0x1100, "此资源不支持恢复");
        }

        Map dd = table.fetchCase( )
                .filter (where, param)
                .select ("data, name, etime")
                .orderBy("ctime DESC")
                .one();

        if (dd.isEmpty()) {
            throw new HongsException(0x1100, "恢复数据源不存在");
        }
        if (Synt.declare(dd.get("etime"), 0L) == 0L) {
            throw new HongsException(0x1100, "活跃数据不可操作");
        }

        //** 保存到数据库 **/

        Map ud = new HashMap();
        ud.put("etime", ctime);

        rd.put("ctime", ctime);
        rd.put("rtime", rtime);
        rd.put("etime",   0  );
        rd.put("name" , dd.get("name"));
        rd.put("data" , dd.get("data"));

        where = "`id`=? AND `form_id`=? AND `etime`=?";
        param = new Object[]{id, fid,0};
        table.update(ud , where, param);
        table.insert(rd);

        //** 保存到索引库 **/

        dd = (Map) app.hongs.util.Data.toObject(dd.get("data").toString());

        Document doc = new Document(  );
        dd.put(Cnst.ID_KEY, id);
        docAdd(doc, dd);
        setDoc(id, doc);
    }

    public void call(String id, String act) throws HongsException {
        String url = (String) getParams().get("callback");
        if (url == null || "".equals(url)) {
            return;
        }

        String fid = getFormId();
        url = Tool.inject(url,Synt.mapOf(
            "id"    , id ,
            "action", act,
            "entity", fid,
            "time"  , time
        ));
        DataCaller.getInstance().add(url);
    }

    protected boolean equals(Object fo, Object fr) {
        return fo.equals(fr);
    }

    private Set<String> wdCols = null;
    private Set<String> nmCols = null;

    @Override
    public Set<String> getFindable() {
        if (null != wdCols) {
            return  wdCols;
        }
        if (Dict.getValue(getFields(), true, "find", "editable")) {
            wdCols = Synt.setOf  (  "find"  );
        } else {
            wdCols = getCaseNames("findable");
            wdCols.remove("find");
        }
        return wdCols;
    }

    public Set<String> getNameable() {
        if (null != nmCols) {
            return  nmCols;
        }
        if (Dict.getValue(getFields(), true, "name", "editable")) {
            nmCols = Synt.setOf  (  "name"  );
        } else {
            nmCols = getCaseNames("nameable");
            nmCols.remove("name");
        }
        return nmCols;
    }

    /**
     * 获取搜索串
     * @param dd
     * @return
     * @throws HongsException
     */
    protected String getFind(Map dd) throws HongsException {
        StringBuilder nn = new StringBuilder();
        Set < String> ns = getFindable( );
        for ( String  fn : ns ) {
              Object  fv = dd.get(fn);
            if (fv == null) continue ;
            if (fv instanceof Collection)
            for (Object fw : (Collection) fv ) {
                nn.append(fw).append(' ');
            } else {
                nn.append(fv).append(' ');
            }
        }
        String nm = nn.toString().trim( );

        if (! ns.contains("find")
        &&  ! ns.contains("id") ) {
            return dd.get("id") +" "+ nm ;
        } else {
            return nm;
        }
    }

    /**
     * 获取名称串
     * @param dd
     * @return
     * @throws HongsException
     */
    protected String getName(Map dd) throws HongsException {
        StringBuilder nn = new StringBuilder();
        Set < String> ns = getNameable( );
        for ( String  fn : ns  ) {
              Object  fv = dd.get(fn);
            if (fv == null) continue ;
            if (fv instanceof Collection)
            for (Object fw : (Collection) fv ) {
                nn.append(fw).append(' ');
            } else {
                nn.append(fv).append(' ');
            }
        }
        String nm = nn.toString().trim( );

        if (! ns.contains("name")
        &&    99 < nm.length( ) ) {
            return nm.substring(0, 99) + "...";
        } else {
            return nm;
        }
    }

}
