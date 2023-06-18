package io.github.ihongs.dh.search;

import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.dh.lucene.LuceneRecord;

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
     * @throws HongsException
     */
    public static SearchEntity getInstance(String conf, String form) throws HongsException {
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
    protected void permit(Map rd, Set ids, int ern)
    throws HongsException {
        /**
         * 遇到中途关闭情况再查一遍
         * 还那么倒霉只好就这样算了
         * 下同此
         */
        try {
            super.permit(rd, ids, ern);
        } catch (Lost ex) {
            super.permit(rd, ids, ern);
        }
    }

    @Override
    public Map  search(Map rd)
    throws HongsException {
        try {
            return super.search(rd);
        } catch (Lost ex) {
            return super.search(rd);
        }
    }

    @Override
    public Map  get(String id)
    throws HongsException {
        try {
            return super.get(id);
        } catch (Lost ex) {
            return super.get(id);
        }
    }

    @Override
    public Map  getOne(Map rd)
    throws HongsException {
        try {
            return super.getOne(rd);
        } catch (Lost ex) {
            return super.getOne(rd);
        }
    }

    @Override
    public List getAll(Map rd)
    throws HongsException {
        try {
            return super.getAll(rd);
        } catch (Lost ex) {
            return super.getAll(rd);
        }
    }

}
