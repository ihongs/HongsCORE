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
import app.hongs.dh.IEntity;
import app.hongs.dh.lucene.LuceneAction;
import app.hongs.dh.lucene.LuceneRecord;
import app.hongs.dh.lucene.LuceneRecord.Loop;
import app.hongs.dh.search.SearchHelper;
import app.hongs.serv.matrix.Data;
import app.hongs.util.Synt;
import app.hongs.util.Tool;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 数据存储动作
 * @author Hongs
 */
@Action("manage/auto")
public class DataAction extends LuceneAction {

    protected Set<String> sub = Synt.asSet("counts", "statis", "export");

    @Override
    public void acting(ActionHelper helper, ActionRunner runner)
    throws HongsException {
        // 特别扩展的资源
        ent = runner.getEntity();
        mod = runner.getModule();
        if (sub.contains(ent)) {
            int pos = mod.lastIndexOf("/");
            ent = mod.substring(1+pos);
            mod = mod.substring(0,pos);
            runner.setEntity(ent);
            runner.setModule(mod);
        }

        super.acting(helper, runner);

        // 绑定特制的表单
        Method m = runner.getMethod( );
        if (m.isAnnotationPresent(Select.class)
        ||  m.isAnnotationPresent(Spread.class)
        ||  m.isAnnotationPresent(Verify.class)) {
            helper.setAttribute("form:"+mod+"/"+ent+"."+ent, getEntity(helper).getFields());
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

    @Action("counts/search")
    @Preset(conf="", envm="")
    public void counts(ActionHelper helper) throws HongsException {
        LuceneRecord sr = getEntity(helper);
        SearchHelper sh = new SearchHelper(sr);
        Map rd = helper.getRequestData();
            rd = getReqMap (helper, sr, "counts", rd);
        Map sd = sh.counts (rd);
            sd = getRspMap (helper, sr, "counts", sd);

        /**
         * 追加枚举名称
         */
        Map xd = (Map) sd.get("info");
       byte md = Synt.declare(helper.getParameter("md") , (byte) 0);
        if (md != 0 && xd != null && mod != null && ent != null) {
            if (FormSet.hasConfFile( mod )) {
                sh.addLabel(xd , md, mod, ent);
            }
        }

        helper.reply(sd);
    }

    @Action("statis/search")
    @Preset(conf="", envm="")
    public void statis(ActionHelper helper) throws HongsException {
        LuceneRecord sr = getEntity(helper);
        SearchHelper sh = new SearchHelper(sr);
        Map rd = helper.getRequestData();
            rd = getReqMap (helper, sr, "statis", rd);
        Map sd = sh.statis (rd);
            sd = getRspMap (helper, sr, "statis", sd);

        /**
         * 追加枚举名称
         */
        Map xd = (Map) sd.get("info");
       byte md = Synt.declare(helper.getParameter("md") , (byte) 0);
        if (md != 0 && xd != null && mod != null && ent != null) {
            if (FormSet.hasConfFile( mod )) {
                sh.addLabel(xd , md, mod, ent);
            }
        }

        helper.reply(sd);
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

        Writer  ot = helper.getOutputWriter();
        Map     fs = sr.getFields();
        Map     es = new HashMap( );

        if (lp.hasNext()) {
            Map<String, Object> ds = lp.next();
            if (ds.isEmpty()) {
                return;
            }
            Map ts = FormSet.getInstance("default").getForm("__types__");

            StringBuilder sf = new StringBuilder();
            for(String fn : ds.keySet( )) {
                String ft ;
                Map mt = (Map) fs.get(fn);
                fn = ( String) mt.get("text");
                ft = ( String) mt.get("type");
                ft = ( String) ts.get(ft);
                if ("enum".equals(ft)) {
                    String xonf = (String) mt.get("conf");
                    String xame = (String) mt.get("enum");
                    if (null == xonf || "".equals( xonf )) xonf = mod;
                    if (null == xame || "".equals( xame )) xame = ent;
                    es.put(fn, FormSet.getInstance(xonf).getEnum(xame));
                }
                
                sf.append(",\"").append(quotes(fn, es)).append("\"");
            }
            sf.append("\r\n");
            ot.write(sf.substring(1));

            StringBuilder sb = new StringBuilder();
            for(Object fv : ds.values( )) {
                sb.append(",\"").append(quotes(fv, es)).append("\"");
            }
            sb.append("\r\n");
            ot.write(sb.substring(1));
        }

        for(Map ds : lp ) {
            StringBuilder sb = new StringBuilder();
            for(Object fv : ds.values( )) {
                sb.append(",\"").append(quotes(fv, es)).append("\"");
            }
            sb.append("\r\n");
            ot.write(sb.substring(1));
        }
    }

    protected final String quotes(Object v, Map es) {
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
                x = quotes(v, es);
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
