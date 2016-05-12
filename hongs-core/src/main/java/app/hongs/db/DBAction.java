package app.hongs.db;

import app.hongs.Cnst;
import app.hongs.CoreLocale;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.FormSet;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.CommitSuccess;
import app.hongs.action.anno.Preset;
import app.hongs.action.anno.Select;
import app.hongs.action.anno.Verify;
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

    protected String cnf = null;
    protected String ent = null;

    @Override
    public void initiate(ActionHelper helper, ActionRunner runner) throws HongsException {
        String act;
        act = runner.getHandle();
        ent = runner.getEntity();
        cnf = runner.getModule();

        try {
            // 下划线开头的为内部资源, 不直接对外开放
            if (ent.startsWith("_")) {
                throw new HongsException(0x1100, "Unsupported Request!");
            }

            // 判断是否禁用了当前动作, 忽略表单不存在
            if ( Dict.getValue(FormSet.getInstance( cnf ).getForm( ent ),
                false, "@", "cant.call." + act)) {
                throw new HongsException(0x1100, "Unsupported Request.");
            }
        }
        catch  (HongsException  ex ) {
            int ec  = ex.getCode(  );
            if (ec != 0x10e8 && ec != 0x10ea) {
                throw ex;
            }
        }
    }

    @Action("retrieve")
    @Preset(conf="", envm="")
    @Select(conf="", form="")
    @Override
    public void retrieve(ActionHelper helper)
    throws HongsException {
        Model   mod = getEntity(helper);
        Map     req = helper.getRequestData();
                req = getReqMap(helper, mod, "retrieve", req);
        Map     rsp = mod.retrieve(req);
                rsp = getRspMap(helper, mod, "retrieve", rsp);
        helper.reply(rsp);
    }

    @Action("create")
    @Preset(conf="", envm="", used={":defence", ":create"})
    @Select(conf="", form="", mode=2)
    @Verify(conf="", form="")
    @CommitSuccess
    @Override
    public void create(ActionHelper helper)
    throws HongsException {
        Model   mod = getEntity(helper);
        Map     req = helper.getRequestData();
                req = getReqMap(helper, mod, "create", req);
        Map     rsp = mod.create(req);
                rsp = getRspMap(helper, mod, "create", rsp);
        String  msg = getRspMsg(helper, mod, "create", 1  );
        helper.reply(msg, rsp);
    }

    @Action("update")
    @Preset(conf="", envm="", used={":defence", ":update"})
    @Verify(conf="", form="")
    @CommitSuccess
    @Override
    public void update(ActionHelper helper)
    throws HongsException {
        Model   mod = getEntity(helper);
        Map     req = helper.getRequestData();
                req = getReqMap(helper, mod, "update", req);
        int     num = mod.update(req);
        String  msg = getRspMsg(helper, mod, "update", num);
        helper.reply(msg, num);
    }

    @Action("delete")
    @Preset(conf="", envm="", used={":defence", ":delete"})
    @CommitSuccess
    @Override
    public void delete(ActionHelper helper)
    throws HongsException {
        Model   mod = getEntity(helper);
        Map     req = helper.getRequestData();
                req = getReqMap(helper, mod, "delete", req);
        int     num = mod.delete(req);
        String  msg = getRspMsg(helper, mod, "delete", num);
        helper.reply(msg, num);
    }

    @Action("exists")
    public void isExists(ActionHelper helper)
    throws HongsException {
        Model   mod = getEntity(helper);
        Map     req = helper.getRequestData();
                req = getReqMap(helper, mod, "exists", req);
        FetchCase c = new FetchCase( );
        c.setOption("INCLUDE_REMOVED", Synt.declare(req.get("include-removed"), false));
        boolean val = mod.exists(req , c );
        helper.reply(null, val);
    }

    @Action("unique")
    public void isUnique(ActionHelper helper)
    throws HongsException {
        Model   mod = getEntity(helper);
        Map     req = helper.getRequestData();
                req = getReqMap(helper, mod, "unique", req);
        FetchCase c = new FetchCase( );
        c.setOption("INCLUDE_REMOVED", Synt.declare(req.get("include-removed"), false));
        boolean val = mod.unique(req , c );
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
        Model  mod = DB.getInstance(this.cnf).getModel(ent);
        new Mview(mod).getFields();
        return mod;
    }

    /**
     * 获取请求数据
     * @param helper
     * @param mod
     * @param opr
     * @param req
     * @return
     * @throws HongsException
     */
    protected  Map   getReqMap(ActionHelper helper, Model mod, String opr, Map req)
    throws HongsException {
        if (!Cnst.ID_KEY.equals(mod.table.primaryKey)) {
            if (req.containsKey(Cnst.ID_KEY)) {
                req.put(mod.table.primaryKey, req.get(Cnst.ID_KEY));
            }
        }
        return req;
    }

    /**
     * 整理返回数据
     * @param helper
     * @param mod
     * @param opr
     * @param rsp
     * @return
     * @throws HongsException
     */
    protected  Map   getRspMap(ActionHelper helper, Model mod, String opr, Map rsp)
    throws HongsException {
        if (!Cnst.ID_KEY.equals(mod.table.primaryKey)) {
            if (rsp.containsKey("info")) {
                /**/ Map  info = (Map ) rsp.get("info");
                info.put(Cnst.ID_KEY , info.get(mod.table.primaryKey));
            }
            if (rsp.containsKey("list")) {
                List<Map> list = (List) rsp.get("list");
            for(/**/ Map  info :  list ) {
                info.put(Cnst.ID_KEY , info.get(mod.table.primaryKey));
            }
            }
        }
        return rsp;
    }

    /**
     * 获取返回消息
     * @param helper
     * @param mod
     * @param opr
     * @param num
     * @return
     * @throws HongsException
     */
    protected String getRspMsg(ActionHelper helper, Model mod, String opr, int num)
    throws HongsException {
        CoreLocale lang = CoreLocale.getInstance().clone( );
        lang.loadIgnrFNF(this.cnf );
        String cnt = Integer.toString(num);
        String key = "fore." + opr + "." + ent + ".success";
        if (! lang.containsKey(key)) {
               key = "fore." + opr + ".success" ;
            Mview view = new  Mview(mod);
            String tit = view.getTitle();
            return lang.translate(key, tit, cnt);
        } else {
            return lang.translate(key, cnt /**/);
        }
    }

}
