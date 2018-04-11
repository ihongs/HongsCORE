package app.hongs.dh;

import app.hongs.CoreLocale;
import app.hongs.HongsException;
import app.hongs.HongsExpedient;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.FormSet;
import app.hongs.action.NaviMap;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.CommitSuccess;
import app.hongs.action.anno.Preset;
import app.hongs.action.anno.Select;
import app.hongs.action.anno.Spread;
import app.hongs.action.anno.Verify;
import app.hongs.util.Dict;
import app.hongs.util.Synt;
import java.util.Map;
import java.util.Set;

/**
 * 表单通用接口
 *
 * 通过重写 getEntity 提供实体对象,
 * 亦可重写 getReqMap 等来修改数据.
 *
 * @author Hongs
 */
abstract public class JointGate implements IActing, IAction {

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

    @Override
    @Action("search")
    @Preset(conf="", form="")
    @Select(conf="", form="")
    @Spread(conf="", form="")
    public void search(ActionHelper helper) throws HongsException {
        IEntity sr = getEntity(helper);
        Map     rd = helper.getRequestData();
                rd = getReqMap(helper, sr, "search", rd);
        Map     sd = sr.search( rd );
                sd = getRspMap(helper, sr, "search", sd);
        helper.reply(sd);
    }

    @Override
    @Action("create")
    @Preset(conf="", form="", deft={":defence"})
    @Verify(conf="", form="")
    @Select(conf="", form="")
    @Spread(conf="", form="")
    @CommitSuccess
    public void create(ActionHelper helper) throws HongsException {
        IEntity sr = getEntity(helper);
        Map     rd = helper.getRequestData();
                rd = getReqMap(helper, sr, "create", rd);
        Map     sd = sr.create(  rd  );
                sd = getRspMap(helper, sr, "create", sd);
        String  ss = getRspMsg(helper, sr, "create", 1 );
        helper.reply(ss, sd);
    }

    @Override
    @Action("update")
    @Preset(conf="", form="", deft={":defence"})
    @Verify(conf="", form="")
    @CommitSuccess
    public void update(ActionHelper helper) throws HongsException {
        IEntity sr = getEntity(helper);
        Map     rd = helper.getRequestData();
                rd = getReqMap(helper, sr, "update", rd);
        int     sn = sr.update(  rd  );
        String  ss = getRspMsg(helper, sr, "update", sn);
        helper.reply(ss, sn);
    }

    @Override
    @Action("delete")
    @Preset(conf="", form="", deft={":defence"})
    @CommitSuccess
    public void delete(ActionHelper helper) throws HongsException {
        IEntity sr = getEntity(helper);
        Map     rd = helper.getRequestData();
                rd = getReqMap(helper, sr, "delete", rd);
        int     sn = sr.delete(  rd  );
        String  ss = getRspMsg(helper, sr, "delete", sn);
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
    abstract public IEntity getEntity(ActionHelper helper)
    throws HongsException;

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
               ent = getName(locale,ett, mod, ent);
            return locale.translate(key, ent, cnt);
        } else {
            return locale.translate(key, /**/ cnt);
        }
    }

    private String getName(CoreLocale locale, IEntity ett, String mod, String ent) throws HongsException {
        String  text;
        Map     item;
        do {
            // 先从表单取名字
            if ( ett instanceof IVolume ) {
                item  = (( IVolume ) ett).getFields();
            } else {
                item  = getForm(mod, ent);
            }
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
        try {
            String  cuf  = FormSet.hasConfFile(mod+"/"+ent)
                               ? mod+"/"+ent : mod ;
            FormSet form = FormSet.getInstance(cuf);
            return  form.getForm(ent);
        } catch ( HongsException ex ) {
        if (ex.getErrno() != 0x10e8
        ||  ex.getErrno() != 0x10ea ) {
            throw   ex  ;
        }
            return  null;
        }
    }

    private Map getMenu(String mod, String ent) throws HongsException {
        try {
            String  cuf  = FormSet.hasConfFile(mod+"/"+ent)
                               ? mod+"/"+ent : mod ;
            NaviMap navi = NaviMap.getInstance(cuf);
            return  navi.getMenu(mod+"/"+ent + "/");
        } catch ( HongsException ex ) {
        if (ex.getErrno() != 0x10e0 ) {
            throw   ex  ;
        }
            return  null;
        }
    }

}
