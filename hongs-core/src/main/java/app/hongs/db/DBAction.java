package app.hongs.db;

import app.hongs.Cnst;
import app.hongs.CoreLocale;
import app.hongs.HongsException;
import app.hongs.HongsExpedient;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.FormSet;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.CommitSuccess;
import app.hongs.action.anno.Preset;
import app.hongs.action.anno.Select;
import app.hongs.action.anno.Verify;
import app.hongs.db.util.FetchCase;
import app.hongs.dh.IActing;
import app.hongs.dh.IAction;
import app.hongs.util.Dict;
import app.hongs.util.Synt;
import java.util.List;
import java.util.Map;

/**
 * 基础数据动作
 * @author Hongs
 */
@Action("hongs/db")
public class DBAction implements IAction, IActing {

    protected String mod = null;
    protected String ent = null;

    @Override
    public void acting(ActionHelper helper, ActionRunner runner) throws HongsException {
        String act;
        act = runner.getHandle();
        ent = runner.getEntity();
        mod = runner.getModule();

        try {
            // 下划线开头的为内部资源, 不直接对外开放
            if (ent.startsWith("_")) {
                throw new HongsException(0x1100, "Unsupported Request!");
            }

            // 判断是否禁用了当前动作, 忽略表单不存在
            if (Dict.getValue( FormSet.getInstance( mod ).getForm( ent ),
                false, "@", "deny.call." + act)) {
                throw new HongsException(0x1100, "Unsupported Request.");
            }
        }
        catch (HongsException|HongsExpedient ex) {
            int ec  = ex.getErrno( );
            if (ec != 0x10e8 && ec != 0x10ea) {
                throw ex;
            }
        }
    }

    @Action("search")
    @Preset(conf="", form="")
    @Select(conf="", form="")
    @Override
    public void search(ActionHelper helper)
    throws HongsException {
        Model   ett = getEntity(helper);
        Map     req = helper.getRequestData();
                req = getReqMap(helper, ett, "search", req);
        Map     rsp = ett.search(req);
                rsp = getRspMap(helper, ett, "search", rsp);
        helper.reply(rsp);
    }

    @Action("create")
    @Preset(conf="", form="", deft={":create"})
    @Select(conf="", form="", mode=2)
    @Verify(conf="", form="")
    @CommitSuccess
    @Override
    public void create(ActionHelper helper)
    throws HongsException {
        Model   ett = getEntity(helper);
        Map     req = helper.getRequestData();
                req = getReqMap(helper, ett, "create", req);
        Map     rsp = ett.create(req);
                rsp = getRspMap(helper, ett, "create", rsp);
        String  msg = getRspMsg(helper, ett, "create", 1  );
        helper.reply(msg, rsp);
    }

    @Action("update")
    @Preset(conf="", form="", deft={":update"})
    @Verify(conf="", form="")
    @CommitSuccess
    @Override
    public void update(ActionHelper helper)
    throws HongsException {
        Model   ett = getEntity(helper);
        Map     req = helper.getRequestData();
                req = getReqMap(helper, ett, "update", req);
        int     num = ett.update(req);
        String  msg = getRspMsg(helper, ett, "update", num);
        helper.reply(msg, num);
    }

    @Action("delete")
    @Preset(conf="", form="", deft={":delete"})
    @CommitSuccess
    @Override
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
        FetchCase c = new FetchCase( );
        c.setOption("INCLUDE_REMOVED", Synt.declare(req.get("include-removed"), false));
        boolean val = ett.exists(req , c );
        helper.reply(null, val);
    }

    @Action("unique")
    public void isUnique(ActionHelper helper)
    throws HongsException {
        Model   ett = getEntity(helper);
        Map     req = helper.getRequestData();
                req = getReqMap(helper, ett, "unique", req);
        FetchCase c = new FetchCase( );
        c.setOption("INCLUDE_REMOVED", Synt.declare(req.get("include-removed"), false));
        boolean val = ett.unique(req , c );
        helper.reply(null, val);
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
        return DB.getInstance(mod).getModel(ent);
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
        CoreLocale lang = CoreLocale.getInstance().clone( );
        lang.fill( mod );
        String cnt = Integer.toString(num);
        String key = "fore." + opr + "." + ent + ".success";
        if (! lang.containsKey(key)) {
               key = "fore." + opr + ".success" ;
            Mview view =  new Mview(ett);
            String tit = view.getTitle();
            return lang.translate(key, tit, cnt);
        } else {
            return lang.translate(key, cnt /**/);
        }
    }

}
