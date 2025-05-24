package io.github.ihongs.dh.search;

import io.github.ihongs.Core;
import io.github.ihongs.CruxException;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.dh.lucene.LuceneRecord;

import java.util.Map;
import org.apache.lucene.document.Document;

/**
 * 搜索记录
 *
 * 经过重构, 已与父类无差异;
 * 当查询或提交时连接被中断,
 * 将会尝试重新执行一遍操作.
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
    public Document getDoc(String id)
    throws CruxException {
        try {
            return super.getDoc(id);
        } catch (Lost e) {
            return super.getDoc(id);
        }
    }

    @Override
    public Loop search(Map rd, int begin, int limit)
    throws CruxException {
        try {
            return super.search(rd, begin, limit);
        } catch (Lost e) {
            return super.search(rd, begin, limit);
        }
    }

    @Override
    public void commit() {
        try {
            super.commit();
        } catch (Lost e) {
            super.commit();
        }
    }

}
