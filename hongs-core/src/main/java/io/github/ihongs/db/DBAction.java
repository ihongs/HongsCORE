package io.github.ihongs.db;

import io.github.ihongs.Cnst;
import io.github.ihongs.CoreLocale;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.ActionRunner;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.action.anno.CommitSuccess;
import io.github.ihongs.action.anno.Preset;
import io.github.ihongs.action.anno.Select;
import io.github.ihongs.action.anno.Verify;
import io.github.ihongs.db.util.FetchCase;
import io.github.ihongs.dh.IActing;
import io.github.ihongs.dh.IAction;
import io.github.ihongs.util.Dict;
import io.github.ihongs.util.Synt;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 基础数据动作
 * @author Hongs
 */
@Action("hongs/db")
public class DBAction implements IAction, IActing {

    @Override
    public void acting(ActionHelper helper, ActionRunner runner) throws HongsException {
        String act = runner.getHandle();
        String ent = runner.getEntity();
        String mod = runner.getModule();

        /**
         * 配置内用表单参数 callable 可指定许可的动作;
         * 配置内下划线开头的表单不允许从外部直接访问;
         * 实体表单名与模块名相同的路径必须省略表单名,
         * 这是为了避免由于多个路径导致权限过滤被绕开,
         * 这种情况需重设模块名告知后续程序正确配置名.
         */

        if (ent.startsWith("_") || mod.endsWith("/" + ent)) {
            throw new HongsException (404, "Unsupported Request!");
        }

        Map fs = null;
        do {
            try {
                fs = FormSet.getInstance(mod).getForm(ent);
                break;
            } catch ( HongsException ex) {
            if (ex.getErrno() != 910
            &&  ex.getErrno() != 912) {
                throw ex;
            }}

            mod = mod + "/" + ent;

            try {
                fs = FormSet.getInstance(mod).getForm(ent);
                runner.setModule ( mod );
            } catch ( HongsException ex) {
            if (ex.getErrno() != 910
            &&  ex.getErrno() != 912) {
                throw ex;
            }}
        } while (false) ;
        if (fs == null) {
            return;
        }

        Set ca  = Synt.toSet(Dict.get(fs , null, "@", "callable"));
        if (ca != null && ! ca.contains(act)) {
            throw new HongsException (405, "Unsupported Request.");
        }
    }

    @Override
    @Action("search")
    @Preset(conf="", form="")
    @Select(conf="", form="")
    public void search(ActionHelper helper)
    throws HongsException {
        Model   ett = getEntity(helper);
        Map     req = helper.getRequestData();
                req = getReqMap(helper, ett, "search", req);
        Map     rsp = ett.search(req);
                rsp = getRspMap(helper, ett, "search", rsp);
        helper.reply(rsp);
    }

    @Override
    @Action("create")
    @Preset(conf="", form="", defs={":defence"})
    @Verify(conf="", form="")
    @CommitSuccess
    public void create(ActionHelper helper)
    throws HongsException {
        Model   ett = getEntity(helper);
        Map     req = helper.getRequestData();
                req = getReqMap(helper, ett, "create", req);
        String  nid = ett.create(req);
        String  msg = getRspMsg(helper, ett, "create", 1  );
        helper.reply(msg, nid);
    }

    @Override
    @Action("update")
    @Preset(conf="", form="", defs={":defence"})
    @Verify(conf="", form="")
    @CommitSuccess
    public void update(ActionHelper helper)
    throws HongsException {
        Model   ett = getEntity(helper);
        Map     req = helper.getRequestData();
                req = getReqMap(helper, ett, "update", req);
        int     num = ett.update(req);
        String  msg = getRspMsg(helper, ett, "update", num);
        helper.reply(msg, num);
    }

    @Override
    @Action("delete")
    @Preset(conf="", form="", defs={":defence"})
    @CommitSuccess
    public void delete(ActionHelper helper)
    throws HongsException {
        Model   ett = getEntity(helper);
        Map     req = helper.getRequestData();
                req = getReqMap(helper, ett, "delete", req);
        int     num = ett.delete(req);
        String  msg = getRspMsg(helper, ett, "delete", num);
        helper.reply(msg, num);
    }

    @Action("exists")
    public void isExists(ActionHelper helper)
    throws HongsException {
        Model   ett = getEntity(helper);
        Map     req = helper.getRequestData();
                req = getReqMap(helper, ett, "exists", req);
        FetchCase c = new FetchCase();
        c.setOption("INCLUDE_REMOVED", Synt.declare(req.get("include-removed"), false));
        boolean val = ett.exists(req , c );
        helper.reply(null, val ? 1 : 0);
    }

    @Action("unique")
    public void isUnique(ActionHelper helper)
    throws HongsException {
        Model   ett = getEntity(helper);
        Map     req = helper.getRequestData();
                req = getReqMap(helper, ett, "unique", req);
        FetchCase c = new FetchCase();
        c.setOption("INCLUDE_REMOVED", Synt.declare(req.get("include-removed"), false));
        boolean val = ett.unique(req , c );
        helper.reply(null, val ? 1 : 0);
    }

    /**
     * 获取模型对象
     * 注意:
     *  对象 Action 注解的命名必须为 "模型路径/实体名称"
     *  方法 Action 注解的命名只能是 "动作名称", 不得含子级实体名称
     * @param helper
     * @return
     * @throws HongsException
     */
    protected Model  getEntity(ActionHelper helper)
    throws HongsException {
        ActionRunner runner = (ActionRunner)
           helper.getAttribute(ActionRunner.class.getName());
        return DB.getInstance (runner.getModule( ))
                 .getModel    (runner.getEntity( ));
    }

    /**
     * 获取请求数据
     * @param helper
     * @param ett
     * @param opr
     * @param req
     * @return
     * @throws HongsException
     */
    protected  Map   getReqMap(ActionHelper helper, Model ett, String opr, Map req)
    throws HongsException {
        // 补充主键
        if (!Cnst.ID_KEY.equals(ett.table.primaryKey)) {
            if (req.containsKey(Cnst.ID_KEY)) {
                req.put(ett.table.primaryKey, req.get(Cnst.ID_KEY));
            }
        }
        return req;
    }

    /**
     * 整理返回数据
     * @param helper
     * @param ett
     * @param opr
     * @param rsp
     * @return
     * @throws HongsException
     */
    protected  Map   getRspMap(ActionHelper helper, Model ett, String opr, Map rsp)
    throws HongsException {
        // 补充标准 ID
        if (!Cnst.ID_KEY.equals(ett.table.primaryKey)) {
            if (rsp.containsKey("info")) {
                /**/ Map  info = (Map ) rsp.get("info");
                info.put(Cnst.ID_KEY , info.get(ett.table.primaryKey));
            }
            if (rsp.containsKey("list")) {
                List<Map> list = (List) rsp.get("list");
            for(/**/ Map  info :  list ) {
                info.put(Cnst.ID_KEY , info.get(ett.table.primaryKey));
            }
            }
        }
        return rsp;
    }

    /**
     * 获取返回消息
     * @param helper
     * @param ett
     * @param opr
     * @param num
     * @return
     * @throws HongsException
     */
    protected String getRspMsg(ActionHelper helper, Model ett, String opr, int num)
    throws HongsException {
        ActionRunner runner = (ActionRunner)
           helper.getAttribute(ActionRunner.class.getName());

        String mod = runner.getModule(   );
        String ent = runner.getEntity(   );
        String cnt = Integer.toString(num);

        CoreLocale locale = CoreLocale.getInstance().clone();
        locale.fill(mod);

        String key = "fore."+opr+"."+ent+".success";
        if (!locale.containsKey(key)) {
               key = "fore."+opr/**/+/**/".success";
        }

        return locale.translate(key, null, cnt);
    }

}
