package io.github.ihongs.dh.search;

import io.github.ihongs.Core;
import io.github.ihongs.CruxException;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.dh.lucene.LuceneRecord;
import io.github.ihongs.dh.lucene.conn.Lost;
import org.apache.lucene.document.Document;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 搜索记录
 *
 * 经过重构, 已与父类无差异;
 * 当查询获取连接中断异常时,
 * 将会尝试再查询并获取一次.
 *
 * @author Hongs
 */
public class SearchEntity extends LuceneRecord {

    public SearchEntity(Map form , String path , String name) {
        super(form , path , name);
    }

    /**
     * 获取实例
     * 存储为 conf/form 表单为 conf.form
     * 表单缺失则尝试获取 conf/form.form
     * 实例生命周期将交由 Core 维护
     * @param conf
     * @param form
     * @return
     * @throws CruxException
     */
    public static SearchEntity getInstance(String conf, String form) throws CruxException {
        String code = SearchEntity.class.getName() +":"+ conf +":"+ form;
        Core   core = Core.getInstance( );
        SearchEntity  inst = (SearchEntity) core.get(code);
        if (inst == null) {
            String path = conf +"/"+ form;
            String name = conf +":"+ form;
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

            inst = new SearchEntity(fxrm, path, name);
            core.set(code , inst);
        }
        return inst;
    }

    @Override
    public Map  get(String id)
    throws CruxException {
        try {
            return super.get(id);
        } catch (Lost e) {
            return super.get(id);
        }
    }

    @Override
    public Map  getOne(Map rd)
    throws CruxException {
        try {
            return super.getOne(rd);
        } catch (Lost e) {
            return super.getOne(rd);
        }
    }

    @Override
    public List getAll(Map rd)
    throws CruxException {
        try {
            return super.getAll(rd);
        } catch (Lost e) {
            return super.getAll(rd);
        }
    }

    @Override
    public Map  search(Map rd)
    throws CruxException {
        try {
            return super.search(rd);
        } catch (Lost e) {
            return super.search(rd);
        }
    }

    @Override
    protected void permit(Map rd, Set ids, int ern)
    throws CruxException {
        try {
            super.permit(rd, ids, ern);
        } catch (Lost e) {
            super.permit(rd, ids, ern);
        }
    }

    @Override
    public void commit() {
        REFLUX_MODE = false;
        Map<String, Document> writes = getWrites();
        if (writes.isEmpty()) {
            return;
        }
        try {
            // 失败重试
            try {
                getDbConn().write(writes);
            } catch (IOException ex) {
                getDbConn().write(writes);
            }
        } catch (IOException ex) {
            throw new Lost(ex, "@core.conn.lost.writer");
        } finally {
            writes.clear( );
        }
    }

}
