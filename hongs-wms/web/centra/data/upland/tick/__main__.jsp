<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.Set"%>
<%@page import="io.github.ihongs.Cnst"%>
<%@page import="io.github.ihongs.Core"%>
<%@page import="io.github.ihongs.CruxException"%>
<%@page import="io.github.ihongs.action.ActionHelper"%>
<%@page import="io.github.ihongs.action.ActionRunner"%>
<%@page import="io.github.ihongs.action.NaviMap"%>
<%@page import="io.github.ihongs.db.DB"%>
<%@page import="io.github.ihongs.serv.matrix.Data"%>
<%@page import="io.github.ihongs.util.Synt"%>
<%@page extends="io.github.ihongs.jsp.Proclet"%>
<%@page contentType="application/json" pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%!
    private static final String THEME_ID_FN = "theme_id";
    private static final String TOPIC_ID_FN = "topic_id";

    /**
     * 获取用户所属的全部部门ID
     */
    private Set getUserUnitIds(Object uid) throws CruxException {
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

    /**
     * 限制用户可见的主题
     * 用户部门均无则开放
     */
    if (! NaviMap.getInstance("centra").chkAuth("centra/data/upland/admin")) {
        String tid = null;
        Object oid ;
        Map    row ;

        ID: {
            oid = req.get(Cnst.ID_KEY);
            if (oid != null && !"".equals(oid)) {
                if (!(oid instanceof String)
                ||  !(oid instanceof Number)) {
                    throw new CruxException(400, "仅支持单个 "+Cnst.ID_KEY+", 不支持 eq,in 等方式");
                }

                row = Data
                    .getInstance("centra/data/upland", "tweet")
                    .getOne(Synt.mapOf(
                        Cnst.RB_KEY, Synt.setOf(TOPIC_ID_FN),
                        Cnst.ID_KEY, oid
                    ));
                if (row == null || row.isEmpty()) {
                    throw  new  CruxException(404, "不存在对应的评论" );
                }
                tid = Synt.asString(row.get(TOPIC_ID_FN));

                row = Data
                    .getInstance("centra/data/upland", "topic")
                    .getOne(Synt.mapOf(
                        Cnst.RB_KEY, Synt.setOf(THEME_ID_FN),
                        Cnst.ID_KEY, tid
                    ));
                if (row == null || row.isEmpty()) {
                    throw  new  CruxException(404, "不存在对应的话题" );
                }
                tid = Synt.asString(row.get(THEME_ID_FN));

                break ID;
            }

            oid = req.get(TOPIC_ID_FN);
            if (oid != null && !"".equals(oid)) {
                if (!(oid instanceof String)
                ||  !(oid instanceof Number)) {
                    throw new CruxException(400, "仅支持单个 "+TOPIC_ID_FN+", 不支持 eq,in 等方式");
                }

                row = Data
                    .getInstance("centra/data/upland", "topic")
                    .getOne(Synt.mapOf(
                        Cnst.RB_KEY, Synt.setOf(THEME_ID_FN),
                        Cnst.ID_KEY, tid
                    ));
                if (row == null || row.isEmpty()) {
                    throw  new  CruxException(404, "不存在对应的话题" );
                }
                tid = Synt.asString(row.get(THEME_ID_FN));

                break ID;
            }

            throw new CruxException(400, "缺少必要的 "+Cnst.ID_KEY+" 或 "+TOPIC_ID_FN);
        }

        row = Data
            .getInstance("centra/data/upland" , "theme")
            .getOne(Synt.mapOf(
                Cnst.RB_KEY, Synt.setOf("owner","users","units"),
                Cnst.ID_KEY, tid
            ));
        if (row == null || row.isEmpty()) {
            throw  new  CruxException(404, "不存在对应的主题" );
        }

        /**
         * 逐一检查是否为主题管理员或成员,
         * 主题未限定用户和部门的可以访问.
         */
        Object  uid = helper.getSessibute(Cnst.UID_SES);
        Set     did = getUserUnitIds(uid);
        boolean owned  = false;
        DO: {
            Set owner  = Synt.asSet(row.get("owner"));
            if (owner != null && owner.contains(uid)) {
                owned  = true ;
                break DO;
            }

            Set users  = Synt.asSet(row.get("users"));
            if (users != null && users.contains(uid)) {
                break DO;
            }

            Set units  = Synt.asSet(row.get("units"));
            if (units != null) {
                units.retainAll(did);
                if (units.isEmpty()) {
                    break DO;
                }
            }

            if ((users == null || users.isEmpty())
            &&  (units == null || units.isEmpty())) {
                break DO;
            }

            throw  new  CruxException(403, "无权访问上级主题" );
        }

        if (! owned
        && ("update".equals(met)
        ||  "delete".equals(met))) {
            oid = req.get(Cnst.ID_KEY);
            row = Data
                .getInstance("centra/data/upland", "tweet")
                .getOne(Synt.mapOf(
                    Cnst.ID_KEY, oid,
                    "cuser"    , uid
                ));
            if (row == null || row.isEmpty()) {
                throw  new  CruxException(403, "无权修改当前消息" );
            }
        }
    }

    ActionRunner.newInstance(helper, "centra/data/upland/tweet/" + met).doAction();
%>