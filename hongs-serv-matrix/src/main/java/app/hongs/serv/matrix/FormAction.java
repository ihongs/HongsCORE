package app.hongs.serv.matrix;

import app.hongs.CoreLocale;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.FormSet;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.CommitSuccess;
import app.hongs.action.anno.Select;
import app.hongs.db.DB;
import app.hongs.db.Table;
import app.hongs.db.link.Loop;
import app.hongs.util.Synt;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Hongs
 */
@Action("centra/matrix/form")
public class FormAction {

    protected final Form model;

    public FormAction() throws HongsException {
        model = (Form) DB.getInstance("matrix").getModel("form");
    }

    @Action("list")
    @Select(conf="matrix", form="form")
    public void getList(ActionHelper helper)
    throws HongsException {
        Map data = model.getList(helper.getRequestData());
        helper.reply(data);
    }

    @Action("info")
    @Select(conf="matrix", form="form")
    public void getInfo(ActionHelper helper)
    throws HongsException {
        Map data = model.getInfo(helper.getRequestData());
        helper.reply(data);
    }

    @Action("save")
    public void doSave(ActionHelper helper)
    throws HongsException {
        Map  data = helper.getRequestData();
        String id = model.set(data);
        Map  info = new HashMap();
        info.put( "id" , id);
        info.put("name", data.get("name") );
        CoreLocale  lang = CoreLocale.getInstance().clone( );
                    lang.load("matrix");
        String ms = lang.translate("core.save.form.success");
        helper.reply(ms, info);
    }

    @Action("delete")
    @CommitSuccess
    public void doDelete(ActionHelper helper)
    throws HongsException {
        Map  data = helper.getRequestData();
        int  rows = model.delete(data );
        CoreLocale  lang = CoreLocale.getInstance().clone( );
                    lang.load("matrix");
        String ms = lang.translate("core.delete.form.success", Integer.toString(rows));
        helper.reply(ms, rows);
    }

    @Action("unique")
    public void isUnique(ActionHelper helper)
    throws HongsException {
        boolean v = model.unique(helper.getRequestData());
        helper.reply(null, v ? 1 : 0);
    }

    @Action("fork/info")
    public void getForkInfo(ActionHelper helper) throws HongsException {
        String id = helper.getParameter(  "id"  );
        Map  form = FormSet.getInstance("matrix").getFormTranslated("form_forks");
        Map  info = (Map) form.get(id);
        Map  data = new HashMap();

        if (info != null && !info.isEmpty()) {
            info  = Synt.mapOf(
                "id"  , id  ,
                "name", info.get("__text__")
            );
        } else {
            info  = model.fetchCase()
                .filter("id = ?", id)
                .select("id,name")
                .one();
        }

        data.put("info", info);
        helper.reply(data);
    }

    @Action("fork/list")
    public void getForkList(ActionHelper helper) throws HongsException {
        Map  rd = helper.getRequestData(   );
        Set  ab = Synt.toTerms(rd.get("ab"));
        Map  data = new  HashMap ();
        List list = new ArrayList();
        data.put  ( "list" , list );

        // 增加预定列表
        if (ab == null || ab.isEmpty() || ab.contains("with-base")) {
            Map form = FormSet.getInstance("matrix").getFormTranslated("form_forks");
            for(Map.Entry et : ( Set<Map.Entry> ) form.entrySet( )) {
                Map item = new HashMap( );
                item.put("data-conf","-");
                item.put("data-form",et.getKey());
                item.put("__name__" ,et.getKey());
                item.putAll( (Map) et.getValue());
                list.add(item);
            }
        }

        // 获取全部表单
        if (ab == null || ab.isEmpty() || ab.contains("with-form")) {
            Table ft = model.table;
            Table ut = model.db.getTable ("unit");
            getForkList( ft, ut, list, null, "" );
        }

        helper.reply(data);
    }

    public void getForkList(Table ft, Table ut, List list, String pid, String pre) throws HongsException {
        Loop units = pid == null
                   ? ut.fetchCase()
                .select("`"+ut.name+"`.`id`, `"+ut.name+"`.`name`")
                .filter("`"+ut.name+"`.`pid` IS NULL")
                .oll()
                   : ut.fetchCase()
                .select("`"+ut.name+"`.`id`, `"+ut.name+"`.`name`")
                .filter("`"+ut.name+"`.`pid` = ?",pid)
                .oll();
        while ( units.hasNext(  ) ) {
            Map unit = units.next();

            Loop forms = ft.fetchCase()
                .select("`"+ft.name+"`.`id`, `"+ft.name+"`.`name`")
                .filter("`"+ft.name+"`.unit_id" , pid)
                .oll();
            while ( forms.hasNext(  ) ) {
                Map form = forms.next ( );
                Map item = new HashMap( );
                item.put("__name__", /**/form.get( "id" ));
                item.put("__text__", pre+form.get("name"));
                item.put("data-vk", "id");
                item.put("data-tk", "name");
                item.put("data-at", "centra/auto/search!" // 虚拟路径需要代理
                                  + "centra/data/"+form.get("id")+"/search");
                item.put("data-al", "centra/data/"+form.get("id")+"/list4fork.html");
                list.add( item );
            }

            getForkList(ft, ut, list, (String) unit.get("id"), (String) unit.get("name") + "/");
        }
    }

}
