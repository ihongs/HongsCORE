package app.hongs.serv.matrix;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.HongsException;
import app.hongs.HongsExpedient;
import app.hongs.action.FormSet;
import app.hongs.db.DB;
import app.hongs.db.Model;
import app.hongs.dh.search.SearchEntity;
import app.hongs.util.Synt;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.lucene.document.Document;

/**
 * 数据存储模型
 * @author Hongs
 */
public class Data extends SearchEntity {

    /**
     * 默认的配置目录
     */
    protected String base = "manage/data";

    protected final String conf;
    protected final String form;

    public Data(String path, String conf, String form) throws HongsException {
        super(path);

        this.conf = conf;
        this.form = form;
    }

    public Data(String conf, String form) throws HongsException {
        this("data/" + form, conf , form);
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
        String comf = base + "/" + form;

        /**
         * 字段以 manage/data 的字段为基础
         * 但可在 handle/data 重设部分字段
         *
         * 配置文件不得放在资源包里面
         * 此处会校验表单文件是否存在
         */
        try {
            if (! new File(
                Core.CONF_PATH + "/"+ conf + Cnst.FORM_EXT +".xml"
            ).exists()) {
                throw new HongsExpedient(0x1104)
                    .setLocalizedOptions(conf);
            }

            fields = FormSet.getInstance(conf).getForm(form);

        if (! comf.equals(conf)) {
            if (! new File(
                Core.CONF_PATH + "/"+ comf + Cnst.FORM_EXT +".xml"
            ).exists()) {
                throw new HongsExpedient(0x1104)
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
        Model    model = DB.getInstance("matrix").getModel("data");
        String   where = "`id`= ? AND `form_id`= ? AND `etime`= ?";
        Object[] param = new String[ ] { id , form , "0" };
        long     ctime = System.currentTimeMillis() / 1000;

        // 删除当前数据
        if (rd == null) {
            Map ud = new HashMap();
            ud.put("etime", ctime);
            ud.put("state",   0  );
            model.table.update(ud, where, param);

            super.del(id);

            return;
        }

        boolean saveToDb = !Synt.asserts(getParams().get("dont.save.to.db"),false);

        // 获取旧的数据
        Map dd;
        if (saveToDb) {
            dd = model.table.fetchCase( )
                    .filter(where, param)
                    .select("data,ctime")
                    .one();
            if(!dd.isEmpty()) {
                if (ctime <= Synt.asserts ( dd.get( "ctime" ) , 0L ) ) {
                    throw new HongsException(0x1100, "等会儿, 不要急");
                }
                dd = (Map) app.hongs.util.Data.toObject(dd.get("data").toString());
            }
        } else {
            dd = get( id );
        }

        // 合并新旧数据
        int i = 0;
        Map<String,Map> fields = getFields (  );
        for(String fn : fields.keySet()) {
            String fr = Synt.declare(rd.get(fn), "");
            String fo = Synt.declare(dd.get(fn), "");
            if (   rd.containsKey( fn )) {
                dd.put( fn , fr );
            } else
            if ( "id".equals(fn)) {
                dd.put( fn , id );
            }
            if ( ! fr.equals(fo)) {
                i += 1;
            }
        }

        // 无更新不操作
        if (i == 0) {
            return;
        }

        //** 保存到数据库 **/

        if (saveToDb) {
            // 拼接展示字段
            StringBuilder nn = new  StringBuilder();
            Set  <String> ns = new  HashSet( getListable() );
                               ns.retainAll( getFindable() );
            for ( String  fn : ns  ) {
                if ( "id".equals(fn) ) continue ;
                nn.append(dd.get(fn).toString()).append(' ');
            }
            String nm;
            if (nn.length() > 120 + 1) {
                nm = nn.substring(0, 120)+ "...";
            } else {
                nm = nn.toString().trim();
            }

            Map ud = new HashMap();
            ud.put("etime", ctime);

            Map nd = new HashMap();
            nd.put("ctime", ctime);
            nd.put("etime", 0);
            nd.put( "id" , id);
            nd.put("form_id", rd.get( "form_id" ));
            nd.put("user_id", rd.get( "user_id" ));
            nd.put("name", nm);
            nd.put("note", rd.get("note"));
            nd.put("data", app.hongs.util.Data.toString(dd));

            model.table.update(ud , where , param);
            model.table.insert(nd);
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

        Model    model = DB.getInstance("matrix").getModel("data");
        String   where = "`id`= ? AND `form_id`= ? AND `ctime`= ?";
        Object[] param = new String [ ] { id , form , rtime + "" };

        Map dd = model.table.fetchCase()
                .filter (where, param)
                .select ("data, name, etime")
                .orderBy("ctime DESC")
                .one();

        if (dd.isEmpty()) {
            throw new HongsException(0x1100, "恢复数据源不存在");
        }
        if (Synt.asserts(dd.get("etime"), 0L) == 0L) {
            throw new HongsException(0x1100, "活跃数据不可操作");
        }

        //** 保存到数据库 **/

        where = "`id` = ? AND `form_id` = ? AND `etime` = ?";
        param = new Object[] {id, form, 0};

        Map ud = new HashMap();
        ud.put("etime", ctime);

        rd.put("ctime", ctime);
        rd.put("rtime", rtime);
        rd.put("etime", 0);
        rd.put("name" , dd.get("name"));
        rd.put("data" , dd.get("data"));

        model.table.update(ud, where, param);
        model.table.insert(rd);

        //** 保存到索引库 **/

        dd = (Map) app.hongs.util.Data.toObject(dd.get("data").toString());

        Document doc = new Document(  );
        dd.put(Cnst.ID_KEY, id);
        docAdd(doc, dd);
        setDoc(id, doc);
    }

}
