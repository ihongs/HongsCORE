<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@page import="app.hongs.Cnst"%>
<%@page import="app.hongs.Core"%>
<%@page import="app.hongs.HongsException"%>
<%@page import="app.hongs.action.ActionRunner"%>
<%@page import="app.hongs.action.ActionHelper"%>
<%@page import="app.hongs.util.Synt"%>
<%@page import="app.hongs.serv.matrix.Data"%>
<%@page pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%!

    public void doMyAction(String unit, String func) throws HongsException {
        // 代理请求
        ActionHelper helper = Core.getInstance(ActionHelper.class);
        new ActionRunner(helper, "centra/auto/" + func).doAction();

        Map rd = helper.getRequestData( );
        Map sd = helper.getResponseData();
        helper.reply(sd);

        // 设置清单
        if (rd.containsKey("items")) {
            Data data = Data.getInstance("centra/data/dining/" + unit + "/item", "item");
            if ("food".equals(unit)) {
                 unit = "unit";
            }

            // 获取关联 ID
            Object id = rd.get("id");
            if ( "create".equals(func)) {
                   sd = Synt.asMap(sd.get("info"));
                   id = sd.get("id");
            }

            // 清空旧的菜单
            if (!"create".equals(func)) {
                List<Map> list = data.getAll(Synt.mapOf(
                    unit+"_id", id,
                    "rb", "id"
                ));
                for (Map  info : list ) {
                    data.del(Synt.asString(info.get("id")));
                }
            }

            // 添加新的菜单
            if (!"delete".equals(func)) {
                Object    uid  = helper.getSessibute(Cnst.UID_SES);
                List<Map> list = Synt.declare(rd.get("items"), List.class);
                for (Map  info : list ) {
                    data.add(Synt.mapOf(
                         unit+"_id", id,
                         "user_id", uid,
                         "food_id", info.get("food_id"),
                         "price"  , info.get("price"),
                         "count"  , info.get("count")
                    ));
                }
            }
        }
    }

%>
