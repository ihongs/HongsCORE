package io.github.ihongs.serv.centra;

import io.github.ihongs.Cnst;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.ActionRunner;
import io.github.ihongs.action.FormSet;
import io.github.ihongs.action.SelectHelper;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.action.anno.CommitSuccess;
import io.github.ihongs.action.anno.CustomReplies;
import io.github.ihongs.action.anno.Preset;
import io.github.ihongs.action.anno.Select;
import io.github.ihongs.action.anno.Verify;
import io.github.ihongs.db.Model;
import io.github.ihongs.dh.IEntity;
import io.github.ihongs.dh.lucene.LuceneRecord.Loop;
import io.github.ihongs.dh.search.SearchAction;
import io.github.ihongs.serv.matrix.Data;
import io.github.ihongs.util.Synt;
import io.github.ihongs.util.Tool;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;

/**
 * 数据存储动作
 * @author Hongs
 */
@Action("centra/auto")
public class DataAction extends SearchAction {

    public DataAction() {
        sub.add("revert");
        sub.add("stream");
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
        ActionRunner  runner  =  (ActionRunner)
              helper.getAttribute(ActionRunner.class.getName());
        return  Data.getInstance (runner.getModule(), runner.getEntity());
    }

    @Override
    public void acting(ActionHelper helper, ActionRunner runner)
    throws HongsException {
        super.acting(helper, runner);

        String ent = runner.getEntity();
        String mod = runner.getModule();
        Method met = runner.getMethod();

        // 绑定特制的表单
        if (met.isAnnotationPresent(Select.class)
        ||  met.isAnnotationPresent(Verify.class)) {
            Data dat = (Data) getEntity(helper);
            Map  fcs =  dat.getFields();
            helper.setAttribute("form:"+ mod +"."+ ent, fcs);
        }

        // 放入当前用户ID
        Object uid = helper.getSessibute(Cnst.UID_SES);
        helper.getRequestData().put("user_id", uid);
        helper.getRequestData().put("form_id", ent);
    }

    @Action("revert/update")
    @CommitSuccess
    public void revert(ActionHelper helper) throws HongsException {
        Data  sr = (Data) getEntity(helper);
        Map   rd = helper.getRequestData( );
            sr.revert(rd);
        helper.reply ("");
    }

    @Action("revert/search")
    public void review(ActionHelper helper) throws HongsException {
        ActionRunner runner = (ActionRunner)
           helper.getAttribute(ActionRunner.class.getName());
        String ent = runner.getEntity();
        String mod = runner.getModule();

        Data    sr = (Data) getEntity(helper);
        Model   mo = sr.getModel();
        Map     rd = helper.getRequestData( );
        rd.remove("user_id");
        rd.put   ("form_id" , sr.getFormId());
        Map     sd = mo.search(rd, mo.fetchCase()
            .setOption ("INCLUDE_REMOVED", true));

        // 详情数据转换
        if (sd.containsKey("info")) {
            Map df = (Map) sd.remove("info" );
            Map dt = (Map) io.github.ihongs.util.Data.toObject(
                  (String) df.remove("data"));
            sd.put("logs", df);
            sd.put("info", dt);

            // 补充枚举和关联
            Set ab = Synt.toTerms(rd.get( Cnst.AB_KEY ));
            if (ab != null) {
                byte md = 0;
                if (ab.contains("_text")) md += SelectHelper.TEXT;
                if (ab.contains("_time")) md += SelectHelper.TIME;
                if (ab.contains("_link")) md += SelectHelper.LINK;
                if (ab.contains("_fork")) md += SelectHelper.FORK;
                if (ab.contains(".form")) md += SelectHelper.FORM;
                if (md != 0) {
                    new SelectHelper().addItemsByForm(mod, ent).select(sd, md);
                }
            }
        }

        helper.reply(sd);
    }

    @Action("stream/search")
    @Preset(conf="", form="")
    @CustomReplies
    public void export(ActionHelper helper) throws HongsException, IOException {
        ActionRunner runner = (ActionRunner)
           helper.getAttribute(ActionRunner.class.getName());
        String ent = runner.getEntity();
        String mod = runner.getModule();

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

    protected String escape(Object v, Map e) {
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
            Collection c = ( Collection ) v ;
            if (c.isEmpty( )) {
                return "";
            }
            StringBuilder  s = new StringBuilder();
            for(Object x : c) {
                s.append(",").append(escape(x, e));
            }
            v = s.substring (1);
        } else
        if (e != null) {
            Object x = e.get(v); // 得到标签
            if (x == null) {
                x =  e.get("-"); // 未知选项
            if (x == null) {
                x =  v;
            }
            }
            v = x;
        }
        // CSV 文本特殊处理
        return v.toString( )
                .replace ("\"", "\"\"")
                .replace ("\r\n", "\n");
    }

}
