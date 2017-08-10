package app.hongs.serv.manage;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.FormSet;
import app.hongs.action.SelectHelper;
import app.hongs.action.SpreadHelper;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.CommitSuccess;
import app.hongs.action.anno.CustomReplies;
import app.hongs.action.anno.Preset;
import app.hongs.action.anno.Select;
import app.hongs.action.anno.Spread;
import app.hongs.action.anno.Verify;
import app.hongs.db.DB;
import app.hongs.db.Model;
import app.hongs.dh.IEntity;
import app.hongs.dh.lucene.LuceneRecord.Loop;
import app.hongs.dh.search.SearchAction;
import app.hongs.serv.matrix.Data;
import app.hongs.util.Synt;
import app.hongs.util.Tool;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

/**
 * 数据存储动作
 * @author Hongs
 */
@Action("manage/auto")
public class DataAction extends SearchAction {

    public DataAction() {
        sub.add("revert");
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
    @Override
    public IEntity getEntity(ActionHelper helper)
    throws HongsException {
        return Data.getInstance(mod, ent);
    }

    @Override
    public void acting(ActionHelper helper, ActionRunner runner)
    throws HongsException {
        super.acting(helper , runner);

        // 绑定特制的表单
        Method m = runner.getMethod();
        if (m.isAnnotationPresent(Select.class)
        ||  m.isAnnotationPresent(Spread.class)
        ||  m.isAnnotationPresent(Verify.class)) {
            Data dat = (Data) getEntity(helper);
            Map fcs = dat.getFields();
            helper.setAttribute("form:"+mod+"/"+ent+"."+ent, fcs);
        }

        // 放入当前用户ID
        Object uid = helper.getSessibute(Cnst.UID_SES);
        helper.getRequestData().put("user_id", uid);
        helper.getRequestData().put("form_id", ent);
    }

    /**
     * 后台总是能指定 id, 有则更新, 无则添加
     * @param helper
     * @throws HongsException
     */
    @Action("save")
    @Preset(conf="", form="", deft={":create"})
    @Verify(conf="", form="")
    @CommitSuccess
    public void save(ActionHelper helper) throws HongsException {
        String  id = (String) helper.getParameter("id");
        if (id == null || "".equals(id)) {
            id  = Core.newIdentity (  );
        }

        Data    sr = (Data) getEntity(helper);
        Map     rd = helper.getRequestData( );
                rd = getReqMap(helper, sr, "update", rd);
                sr.set(id, rd);
        String  ss = getRspMsg(helper, sr, "update", 1 );

        helper.reply(ss, Synt.mapOf("id",id));
    }

    @Action("revert/update")
    @CommitSuccess
    public void redo(ActionHelper helper) throws HongsException {
        String  id = (String) helper.getParameter("id");
        if (id == null || "".equals(id)) {
            throw new HongsException(0x1100, "id required");
        }
        Data    sr = (Data) getEntity(helper);
        Map     rd = helper.getRequestData( );
        sr.redo(id , rd);
        helper.reply("");
    }

    @Action("revert/search")
    public void list(ActionHelper helper) throws HongsException {
        Model   mo = DB.getInstance("matrix").getModel("data");
        Map     rd = helper.getRequestData( );
        Map     sd = mo.search(rd);

        // 详情数据转换
        if (sd.containsKey("info")) {
            Map df = (Map) sd.get("info");
            Map dt = (Map) app.hongs.util.Data.toObject((String) df.get("data"));
            df.putAll(dt);
            new SelectHelper()
                .addEnumsByForm(mod, ent)
                .select ( df , (short) 2);
            new SpreadHelper()
                .addItemsByForm(mod, ent)
                .spread ( df );
        }

        helper.reply(sd);
    }

    @Action("stream")
    @Preset(conf="", form="")
    @CustomReplies
    public void export(ActionHelper helper) throws HongsException, IOException {
        Data    sr = (Data) getEntity(helper);
        Map     rd = helper.getRequestData( );
                rd = getReqMap(helper, sr, "export", rd);
        int     rn = Synt.declare(rd.get("rn"), 0);
        int     pn = Synt.declare(rd.get("pn"), 0);
        Loop    lp = sr.search( rd , pn , rn );

        /**
         * 逐行输出
         */

        HttpServletResponse rs = helper.getResponse();
        String csv = ent + "_" + new SimpleDateFormat( "yyyyMMddHHmmss" ).format( new Date(  ) );
        rs.setHeader("Content-Disposition", "attachment;filename="+csv+".csv");
        rs.setHeader("Content-Type", "application/octet-stream;charset=UTF-8");
        rs.setHeader("Transfer-Encoding", "chunked" );

        Writer  ot = rs.getWriter();
        Map     fs = sr.getFields();
        Map     es = new HashMap ();

        if (lp.hasNext()) {
            Map<String, Object> ds = lp.next();
            StringBuilder sf = new StringBuilder();
            StringBuilder sb = new StringBuilder();
            Map ts = FormSet.getInstance("default").getEnum("__types__");

            for(Map.Entry<String, Object> et : ds.entrySet()) {
                Object fv = et.getValue();
                String fn = et.getKey(  );
                Map mt = (Map) fs.get(fn);
                Map ex = null;

                String fx, ft;
                fx = (String) mt.get("__text__");
                ft = (String) mt.get("__type__");
                ft = (String) ts.get( ft );
                if ("enum".equals(ft)) {
                    String xonf = (String) mt.get("conf");
                    String xame = (String) mt.get("enum");
                    if (null == xonf || "".equals( xonf )) xonf = mod;
                    if (null == xame || "".equals( xame )) xame = fn ;
                    ex = FormSet.getInstance(xonf).getEnum(xame);
                    es.put(fn, ex);
                }

                sf.append(",\"").append(escape(fx,null)).append("\"");
                sb.append(",\"").append(escape(fv, ex )).append("\"");
            }

            ot.write(sf.append("\r\n").substring(1));
            ot.write(sb.append("\r\n").substring(1));
            ot.flush();
        }

        while (lp.hasNext()) {
            Map<String, Object> ds = lp.next();
            StringBuilder sb = new StringBuilder();

            for(Map.Entry<String, Object> et : ds.entrySet()) {
                Object fv = et.getValue();
                String fn = et.getKey(  );
                Map ex = (Map) es.get(fn);

                sb.append(",\"").append(escape(fv, ex )).append("\"");
            }

            ot.write(sb.append("\r\n").substring(1));
        }
    }

    protected String escape(Object v, Map es) {
        if (v == null) {
            return "";
        }
        if (v instanceof Date  ) {
            return v.toString( );
        }
        if (v instanceof Number) {
            return Tool.toNumStr((Number) v);
        }
        if (v instanceof Collection) {
            StringBuilder sb = new StringBuilder();
            for(Object x : ((Collection) v)) {
                x = escape(v, es);
                sb.append( "," ).append( x );
            }
            v = sb.substring(1, sb.length());
        } else
        if (es != null) {
            Object x = es.get(v);
            if (x == null) {
                x = es.get ("*");
            if (x == null) {
                x = v;
            }
            }
            v = x;
        }
        return v.toString().replace("\"", "\"\"").replace("\r\n", "\n");
    }

}
