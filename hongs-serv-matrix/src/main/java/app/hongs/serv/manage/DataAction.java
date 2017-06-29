package app.hongs.serv.manage;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.ActionRunner;
import app.hongs.action.FormSet;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.CommitSuccess;
import app.hongs.action.anno.Preset;
import app.hongs.action.anno.Select;
import app.hongs.action.anno.Spread;
import app.hongs.action.anno.Verify;
import app.hongs.dh.lucene.LuceneRecord;
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

/**
 * 数据存储动作
 * @author Hongs
 */
@Action("manage/auto")
public class DataAction extends SearchAction {

    public DataAction() {
        sub.add("export");
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
            Map fcs = getEntity(helper).getFields();
            helper.setAttribute("form:"+mod+"/"+ent+"."+ent, fcs);
        }

        // 放入当前用户ID
        helper.getRequestData().put("cuid", helper.getSessibute(Cnst.UID_SES));
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
    public LuceneRecord getEntity(ActionHelper helper)
    throws HongsException {
        return Data.getInstance(mod, ent);
    }

    /**
     * 后台总是能指定 id, 有则更新, 无则添加
     * @param helper
     * @throws HongsException
     */
    @Action("save")
    @Preset(conf="", envm="", used={":defence", ":create"})
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

        helper.reply(ss, Synt.asMap("id",id));
    }

    @Action("redo")
    @CommitSuccess
    public void redo(ActionHelper helper) {
        // TODO: Redo history updates
    }

    @Action("logs")
    public void list(ActionHelper helper) {
        // TODO: Get the history logs
    }

    @Action("export/search")
    @Preset(conf="", envm="")
    public void export(ActionHelper helper) throws HongsException, IOException {
        Data    sr = (Data) getEntity(helper);
        Map     rd = helper.getRequestData( );
                rd = getReqMap(helper, sr, "export", rd);
        Loop    lp = sr.search(  rd  , 0 , 0);

        /**
         * 逐行输出
         */

        Map     fs = sr.getFields();
        Map     es = new HashMap( );
        Writer  ot = helper.getOutputWriter();

        String csv = ent + "_" + new SimpleDateFormat( "yyyyMMddHHmmss" ).format( new Date(  ) );
        helper.getResponse().setHeader("Content-Disposition", "attachment;filename="+csv+".csv");
        helper.getResponse().setHeader("Content-Type", "application/octet-stream;charset=UTF-8");

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
                fx = (String) mt.get("text");
                ft = (String) mt.get("type");
                ft = (String) ts.get(ft);
                if ("enum".equals(ft)) {
                    String xonf = (String) mt.get("conf");
                    String xame = (String) mt.get("enum");
                    if (null == xonf || "".equals( xonf )) xonf = mod;
                    if (null == xame || "".equals( xame )) xame = ent;
                    ex = FormSet.getInstance(xonf).getEnum(xame);
                    es.put(fn, ex);
                }

                sf.append(",\"").append(escape(fx,null)).append("\"");
                sb.append(",\"").append(escape(fv, ex )).append("\"");
            }

            ot.write(sf.append("\r\n").substring(1));
            ot.write(sb.append("\r\n").substring(1));
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
