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
import app.hongs.util.Synt;
import java.util.Map;
import java.util.Set;

/**
 * Lucene 模型动作
 * @author Hongs
 */
@Action()
public class LuceneAction implements IAction, IActing {

    @Override
    public void acting(ActionHelper helper, ActionRunner runner) throws HongsException {
        String act = runner.getHandle();
        String ent = runner.getEntity();
        String mod = runner.getModule();

        try {
            // 探测实体是否为独占模块, 方便自动机处理
            if (FormSet.hasConfFile(mod + "/" + ent)) {
                mod = mod + "/" + ent  ;
                runner.setModule( mod );
            }

            // 下划线开头的为内部资源, 不直接对外开放
            if (ent.startsWith("_")) {
                throw new HongsException(0x1100, "Unsupported Request!");
            }

            // 判断是否禁用了当前动作, 忽略表单不存在
            Map fa  = FormSet.getInstance (mod).getForm (ent);
            Set ca  = Synt.toSet( Dict.getDepth( fa, "@", "callable" ) );
            if (ca != null && !ca.contains(act)) {
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
    @Preset(conf="", form="", deft={":create"})
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
    @Preset(conf="", form="", deft={":update"})
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
    @Preset(conf="", form="", deft={":delete"})
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
        ActionRunner runner = (ActionRunner)
           helper.getAttribute(ActionRunner.class.getName());
        return LuceneRecord.getInstance (runner.getModule(), runner.getEntity());
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
    protected  Map   getRspMap(ActionHelper helper, IEntity ett, String opr, Map rsp)
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
        ActionRunner runner = (ActionRunner)
           helper.getAttribute(ActionRunner.class.getName(  ));
        CoreLocale   locale = CoreLocale.getInstance().clone();

        String mod = runner.getModule(   );
        String ent = runner.getEntity(   );
        String cnt = Integer.toString(num);
        String key = "fore." + opr + "." + ent + ".success";

        locale.fill(mod);
        if ( ! locale.containsKey(key) ) {
               key = "fore." + opr + ".success";
            String tit = getTitle(locale,mod, ent);
            return locale.translate(key, tit, cnt);
        } else {
            return locale.translate(key, /**/ cnt);
        }
    }

    /**
     * 获取实体标题, 用于 getRspMsg 中
     * @param locale
     * @param mod
     * @param ent
     * @return
     * @throws HongsException
     */
    protected String getTitle(CoreLocale locale, String mod, String ent) throws HongsException {
        String  text;
        Map     item;
        do {
            // 先从表单取名字
            item = getForm(mod, ent);
            if (item != null  && item.containsKey(   "@"    )) {
                item  = (Map   ) item.get(   "@"    );
            if (item != null  && item.containsKey("__text__")) {
                text  = (String) item.get("__text__");
                break;
            }
            }

            // 再从菜单取名字
            item = getMenu(mod, ent);
            if (item != null  && item.containsKey(  "text"  )) {
                text  = (String) item.get(  "text"  );
                break;
            }

            // 最后配置取名字
            text = "core.entity."+ent+".name";
        } while (false);

        return locale.translate(text);
    }

    private Map getForm(String mod, String ent) throws HongsException {
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

    private Map getMenu(String mod, String ent) throws HongsException {
        String  cuf  = FormSet.hasConfFile(mod + "/" + ent)
                       ? mod + "/" + ent : mod ;
        NaviMap navi = NaviMap.getInstance(cuf);
        return  navi.getMenu(mod+"/"+ent+"/" );
    }

}
