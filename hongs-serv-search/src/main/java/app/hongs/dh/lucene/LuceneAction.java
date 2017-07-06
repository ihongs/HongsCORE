package app.hongs.dh.lucene;

import app.hongs.CoreLocale;
import app.hongs.HongsException;
import app.hongs.HongsExpedient;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.FormSet;
import app.hongs.action.NaviMap;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.Select;
import app.hongs.action.anno.Verify;
import app.hongs.action.anno.Spread;
import app.hongs.action.anno.CommitSuccess;
import app.hongs.action.anno.Preset;
import app.hongs.dh.IActing;
import app.hongs.dh.IAction;
import app.hongs.dh.IEntity;
import app.hongs.util.Dict;
import java.util.Map;

/**
 * Lucene 模型动作
 * @author Hongs
 */
@Action()
public class LuceneAction implements IAction, IActing {

    protected String mod = null;
    protected String ent = null;

    @Override
    public void acting(ActionHelper helper, ActionRunner runner) throws HongsException {
        String act;
        act = runner.getHandle();
        ent = runner.getEntity();
        mod = runner.getModule();

        try {
            // 方便自动机处理
            if (FormSet.hasConfFile(mod + "/" + ent)) {
                mod  =  mod + "/" + ent;
            }

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
    @Preset(conf="", envm="")
    @Select(conf="", form="")
    @Spread(conf="", form="")
    @Override
    public void search(ActionHelper helper) throws HongsException {
        IEntity sr = getEntity(helper);
        Map     rd = helper.getRequestData();
                rd = getReqMap(helper, sr, "search", rd);
        Map     sd = sr.search( rd );
                sd = getRspMap(helper, sr, "search", sd);
//              sr.destroy( );
        helper.reply(sd);
    }

    @Action("create")
    @Preset(conf="", envm="", used={":defence", ":create"})
    @Select(conf="", form="", mode=2)
    @Verify(conf="", form="")
    @CommitSuccess
    @Override
    public void create(ActionHelper helper) throws HongsException {
        IEntity sr = getEntity(helper);
        Map     rd = helper.getRequestData();
                rd = getReqMap(helper, sr, "create", rd);
        Map     sd = sr.create(  rd  );
                sd = getRspMap(helper, sr, "create", sd);
        String  ss = getRspMsg(helper, sr, "create", 1 );
//              sr.destroy();
        helper.reply(ss, sd);
    }

    @Action("update")
    @Preset(conf="", envm="", used={":defence", ":update"})
    @Verify(conf="", form="")
    @CommitSuccess
    @Override
    public void update(ActionHelper helper) throws HongsException {
        IEntity sr = getEntity(helper);
        Map     rd = helper.getRequestData();
                rd = getReqMap(helper, sr, "update", rd);
        int     sn = sr.update(  rd  );
        String  ss = getRspMsg(helper, sr, "update", sn);
//              sr.destroy();
        helper.reply(ss, sn);
    }

    @Action("delete")
    @Preset(conf="", envm="", used={":defence", ":delete"})
    @CommitSuccess
    @Override
    public void delete(ActionHelper helper) throws HongsException {
        IEntity sr = getEntity(helper);
        Map     rd = helper.getRequestData();
                rd = getReqMap(helper, sr, "delete", rd);
        int     sn = sr.delete(  rd  );
        String  ss = getRspMsg(helper, sr, "delete", sn);
//              sr.destroy();
        helper.reply(ss, sn);
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
    public IEntity getEntity(ActionHelper helper)
    throws HongsException {
        return LuceneRecord.getInstance(mod, ent);
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
    protected  Map   getReqMap(ActionHelper helper, IEntity ett, String opr, Map req)
    throws HongsException {
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
    protected  Map   getRspMap(ActionHelper helper, IEntity ett,  String opr, Map rsp)
    throws HongsException {
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
    protected String getRspMsg(ActionHelper helper, IEntity ett, String opr, int num)
    throws HongsException {
        CoreLocale lang = CoreLocale.getInstance().clone( );
        lang.fill( mod );
        String cnt = Integer.toString(num);
        String key = "fore." + opr + "." + ent + ".success";
        if (! lang.containsKey(key)) {
               key = "fore." + opr + ".success" ;
            String tit = getTitle(lang);
            return lang.translate(key, tit, cnt);
        } else {
            return lang.translate(key, cnt /**/);
        }
    }

    /**
     * 获取实体标题, 用于 getRspMsg 中
     * @param lang
     * @return
     * @throws HongsException
     */
    protected String getTitle(CoreLocale lang) throws HongsException {
        String  text;
        Map     item;
        do {
            // 先从表单取名字
            item = getForm();
            if (item != null  && item.containsKey(   "@"    )) {
                item  = (Map   ) item.get(   "@"    );
            if (item != null  && item.containsKey("__text__")) {
                text  = (String) item.get("__text__");
                break;
            }
            }

            // 再从菜单取名字
            item = getMenu();
            if (item != null  && item.containsKey(  "text"  )) {
                text  = (String) item.get(  "text"  );
                break;
            }

            // 最后配置取名字
            text = "core.entity."+ent+".name";
        } while (false);

        return lang.translate(text);
    }

    private Map getForm() throws HongsException {
        String  cuf  = FormSet.hasConfFile(mod + "/" + ent)
                       ? mod + "/" + ent : mod ;
        FormSet form = FormSet.getInstance(cuf);
        try {
            return form.getFormTranslated (ent);
        } catch (HongsException ex ) {
        if (ex.getErrno() == 0x10ea) {
            return null;
        } else {
            throw  ex  ;
        }
        }
    }

    private Map getMenu() throws HongsException {
        String  cuf  = FormSet.hasConfFile(mod + "/" + ent)
                       ? mod + "/" + ent : mod ;
        NaviMap navi = NaviMap.getInstance(cuf);
        return  navi.getMenu(mod+"/"+ent+"/" );
    }

}
