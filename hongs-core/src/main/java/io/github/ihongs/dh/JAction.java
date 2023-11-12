package io.github.ihongs.dh;

import io.github.ihongs.CoreLocale;
import io.github.ihongs.CruxException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.ActionRunner;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.action.anno.CommitSuccess;
import io.github.ihongs.action.anno.Preset;
import io.github.ihongs.action.anno.Select;
import io.github.ihongs.action.anno.Verify;
import io.github.ihongs.util.Dict;
import io.github.ihongs.util.Synt;
import java.util.Map;
import java.util.Set;

/**
 * 表单模型通用接口
 *
 * 通过重写 getEntity 提供实体对象,
 * 亦可重写 getReqMap 等来修改数据.
 *
 * @author Hongs
 */
abstract public class JAction implements IActing, IAction {

    @Override
    public void acting(ActionHelper helper, ActionRunner runner) throws CruxException {
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
            throw new CruxException(404 , "Unsupported Request!");
        }

        Map fs = null;
        do {
            try {
                fs = FormSet.getInstance(mod).getForm(ent);
                break;
            } catch (CruxException ex) {
            if (ex.getErrno( ) != 910
            &&  ex.getErrno( ) != 912) { // 非表单缺失
                throw ex;
            }}

            mod = mod + "/" + ent;

            try {
                fs = FormSet.getInstance(mod).getForm(ent);
                runner.setModule (mod);
            } catch (CruxException ex) {
            if (ex.getErrno( ) != 910
            &&  ex.getErrno( ) != 912) { // 非表单缺失
                throw ex;
            }}
        } while (false) ;
        if (fs == null) {
            return;
        }

        Set ca  = Synt.toSet(Dict.get(fs, null, "@", "callable"));
        if (ca != null && ! ca.contains(act)) {
            throw new CruxException(405 , "Unsupported Request.");
        }
    }

    @Action("recipe")
    @Select(conf="", form="")
    public void recipe(ActionHelper helper) throws CruxException {
        helper.reply("");
    }

    @Override
    @Action("search")
    @Preset(conf="", form="", defs={"defense"})
    @Select(conf="", form="")
    public void search(ActionHelper helper) throws CruxException {
        IEntity sr = getEntity(helper);
        Map     rd = helper.getRequestData();
                rd = getReqMap(helper, sr, "search", rd);
        Map     sd = sr.search( rd );
                sd = getRspMap(helper, sr, "search", sd);
        helper.reply(sd);
    }

    @Override
    @Action("recite")
    @Preset(conf="", form="", defs={"defense"})
    @Select(conf="", form="")
    public void recite(ActionHelper helper) throws CruxException {
        IEntity sr = getEntity(helper);
        Map     rd = helper.getRequestData();
                rd = getReqMap(helper, sr, "recite", rd);
        Map     sd = sr.recite( rd );
                sd = getRspMap(helper, sr, "recite", sd);
        helper.reply(sd);
    }

    @Override
    @Action("create")
    @Preset(conf="", form="", deft={".default"})
    @Verify(conf="", form="")
    @CommitSuccess
    public void create(ActionHelper helper) throws CruxException {
        IEntity sr = getEntity(helper);
        Map     rd = helper.getRequestData();
                rd = getReqMap(helper, sr, "create", rd);
        String  sn = sr.create(  rd  );
        String  ss = getRspMsg(helper, sr, "create", 1 );
        helper.reply(ss, sn);
    }

    @Override
    @Action("update")
    @Preset(conf="", form="", defs={".defence"})
    @Verify(conf="", form="")
    @CommitSuccess
    public void update(ActionHelper helper) throws CruxException {
        IEntity sr = getEntity(helper);
        Map     rd = helper.getRequestData();
                rd = getReqMap(helper, sr, "update", rd);
        int     sn = sr.update(  rd  );
        String  ss = getRspMsg(helper, sr, "update", sn);
        helper.reply(ss, sn);
    }

    @Override
    @Action("delete")
    @Preset(conf="", form="", defs={".defence"})
    @CommitSuccess
    public void delete(ActionHelper helper) throws CruxException {
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
     * @throws CruxException
     */
    abstract public IEntity getEntity(ActionHelper helper)
    throws CruxException;

    /**
     * 获取请求数据
     * @param helper
     * @param ett
     * @param opr
     * @param req
     * @return
     * @throws CruxException
     */
    protected  Map   getReqMap(ActionHelper helper, IEntity ett, String opr, Map req)
    throws CruxException {
        return req;
    }

    /**
     * 整理返回数据
     * @param helper
     * @param ett
     * @param opr
     * @param rsp
     * @return
     * @throws CruxException
     */
    protected  Map   getRspMap(ActionHelper helper, IEntity ett, String opr, Map rsp)
    throws CruxException {
        return rsp;
    }

    /**
     * 获取返回消息
     * @param helper
     * @param ett
     * @param opr
     * @param num
     * @return
     * @throws CruxException
     */
    protected String getRspMsg(ActionHelper helper, IEntity ett, String opr, int num)
    throws CruxException {
        ActionRunner runner = (ActionRunner)
           helper.getAttribute(ActionRunner.class.getName());

        String mod = runner.getModule(   );
        String ent = runner.getEntity(   );
        String cnt = Integer.toString(num);

        CoreLocale locale = CoreLocale.getMultiple(mod, "default");

        String key = "fore."+opr+"."+ent+".success";
        if (locale.getProperty(key) == null) {
               key = "fore."+opr/**/+/**/".success";
        }

        return locale.translate(key, null, cnt);
    }

}
