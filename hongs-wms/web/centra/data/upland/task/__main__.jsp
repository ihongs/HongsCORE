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
    private static final String PLAN_ID_FN = "plan_id";
    private static final String TASK_ID_FN = "task_id";

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
    if (! NaviMap.getInstance("centra").chkAuth("centra/data/upland/lead")) {
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
                    .getInstance("centra/data/upland", "task")
                    .getOne(Synt.mapOf(
                        Cnst.RB_KEY, Synt.setOf(PLAN_ID_FN),
                        Cnst.ID_KEY, oid
                    ));
                if (row == null || row.isEmpty()) {
                    throw  new  CruxException(404, "不存在对应的任务" );
                }
                tid = Synt.asString(row.get(PLAN_ID_FN));

                break ID;
            }

            oid = req.get(PLAN_ID_FN);
            if (oid != null && !"".equals(oid)) {
                if (!(oid instanceof String)
                ||  !(oid instanceof Number)) {
                    throw new CruxException(400, "仅支持单个 "+PLAN_ID_FN+", 不支持 eq,in 等方式");
                }

                tid = Synt.asString(oid);

                break ID;
            }

            throw new CruxException(400, "缺少必要的 "+Cnst.ID_KEY+" 或 "+PLAN_ID_FN);
        }

        row = Data
            .getInstance("centra/data/upland", "plan")
            .getOne(Synt.mapOf(
                Cnst.RB_KEY, Synt.setOf("owner","users","units"),
                Cnst.ID_KEY, tid
            ));
        if (row == null || row.isEmpty()) {
            throw  new  CruxException(404, "不存在对应的计划" );
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

            throw  new  CruxException(403, "无权访问上级计划" );
        }

        if (! owned
        && ("update".equals(met)
        ||  "delete".equals(met))) {
            oid = req.get(Cnst.ID_KEY);
            row = Data
                .getInstance("centra/data/upland", "task")
                .getOne(Synt.mapOf(
                    Cnst.ID_KEY, oid,
                    "owner"    , uid
                ));
            if (row == null || row.isEmpty()) {
                throw  new  CruxException(403, "任务没有指派给您" );
            }
        }
    }

    if ("create".equals(met)) {
        /**
         * 默认管理员设为自己
         */
        Set owner  = Synt.asSet(req.get("owner"));
        if (owner != null )  owner.remove("");
        if (owner == null || owner.isEmpty()) {
            owner  = Synt.setOf(helper.getSessibute(Cnst.UID_SES));
            req.put("owner", owner);
        }
    }

    ActionRunner.newInstance(helper, "centra/data/upland/task/" + met).doAction();

    if ("craete".equals(met)
    ||  "update".equals(met)) {
        /**
         * 将备注作为评论写入
         */
        Map  rsp  = helper.getResponseData();
        Set  ids  = Synt.asSet(
             rsp.containsKey(Cnst.ID_KEY)
          ?  rsp.get(Cnst.ID_KEY)
          :  req.get(Cnst.ID_KEY)
        );
        String memo = Synt.asString(req.get("memo"));
        if ( ids != null && !  ids.isEmpty()
        &&  memo != null && ! memo.isEmpty()) {
            if (Synt.declare(rsp.get("ok"), false )
            &&  Synt.declare(rsp.get("rn"), 0) > 0) {
                Object uid = helper.getSessibute(Cnst.UID_SES);
                long   now = System.currentTimeMillis() / 1000;
                Data   mod = Data.getInstance("centra/data/upland", "tick");
                for(Object oid : ids) {
                    mod.create(Synt.mapOf(
                        TASK_ID_FN, oid,
                         "note", memo,
                        "cuser", uid ,
                        "ctime", now ,
                        "boost", 0
                    ));
                }
            }
        }
    }
%>