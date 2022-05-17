<%@page import="io.github.ihongs.serv.matrix.Data"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.Set"%>
<%@page import="io.github.ihongs.Cnst"%>
<%@page import="io.github.ihongs.Core"%>
<%@page import="io.github.ihongs.HongsException"%>
<%@page import="io.github.ihongs.action.ActionHelper"%>
<%@page import="io.github.ihongs.action.ActionRunner"%>
<%@page import="io.github.ihongs.action.NaviMap"%>
<%@page import="io.github.ihongs.db.DB"%>
<%@page import="io.github.ihongs.util.Dict"%>
<%@page import="io.github.ihongs.util.Synt"%>
<%@page extends="io.github.ihongs.jsp.Proclet"%>
<%@page contentType="application/json" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%!
    /**
     * 获取用户所属的全部部门ID
     */
    private Set getUserUnitIds(Object uid) throws HongsException {
        List<Map> rows = DB
            .getInstance("master")
            .getTable("user_unit")
            .fetchCase()
            .field("unit_id")
            .where("user_id = ?" , uid )
            .getAll();
        Set dids = Synt.setOf();
        for(Map row : rows) {
            dids.add(row.get("unit_id"));
        }
        if (dids.isEmpty()) {
            dids.add( "-" );
        }
        return dids;
    }
%>
<%
    ActionHelper helper = ActionHelper.getInstance( );
    Map    req = helper.getRequestData();
    String act = Core.ACTION_NAME.get();
    String met = act.substring(act.lastIndexOf('/')+1,
                               act.lastIndexOf('.') );

    if ("search".equals(met)) {
        /**
         * 限制用户可见的主题
         * 用户部门均无则开放
         */
        if (! NaviMap.getInstance("centra").chkAuth("centra/data/upland/admin")) {
            Object uid = helper.getSessibute(Cnst.UID_SES);
            Dict.put(req , Synt.listOf(
                Synt.mapOf("owner", uid),
                Synt.mapOf("users", uid),
                Synt.mapOf("units", getUserUnitIds( uid )),
                Synt.mapOf("users", Synt.mapOf(Cnst.IS_REL, "null"),
                           "units", Synt.mapOf(Cnst.IS_REL, "null"))
            ), Cnst.AR_KEY, "x", Cnst.OR_KEY);
        }
    } else
    if ("update".equals(met)
    ||  "delete".equals(met)) {
        /**
         * 限制用户管理的主题
         */
        if (! NaviMap.getInstance("centra").chkAuth("centra/data/upland/admin")) {
            Object uid = helper.getSessibute(Cnst.UID_SES);
            Dict.put(req , Synt.listOf(
                Synt.mapOf("owner", uid)
            ), Cnst.AR_KEY, "x", Cnst.OR_KEY);
        }
    } else
    if ("create".equals(met)
    ||  "revert".equals(met)) {
        /**
         * 限制用户发新的主题
         */
        if (! NaviMap.getInstance("centra").chkAuth("centra/data/upland/admin")) {
            throw new HongsException(403, "您无权新建/重建主题");
        }
    }

    ActionRunner.newInstance(helper, "centra/data/upland/theme/" + met).doAction();
%>