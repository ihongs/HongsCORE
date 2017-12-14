<%@page import="app.hongs.Cnst"%>
<%@page import="app.hongs.Core"%>
<%@page import="app.hongs.HongsException"%>
<%@page import="app.hongs.action.ActionRunner"%>
<%@page import="app.hongs.action.ActionHelper"%>
<%@page import="app.hongs.serv.matrix.Data"%>
<%@page import="app.hongs.util.Dict"%>
<%@page import="app.hongs.util.Synt"%>
<%@page import="java.util.HashSet"%>
<%@page import="java.util.HashMap"%>
<%@page import="java.util.List"%>
<%@page import="java.util.Map"%>
<%@page import="java.util.Set"%>
<%@page pageEncoding="UTF-8" trimDirectiveWhitespaces="true"%>
<%!

    public void doMyAction(String unit, String func) throws HongsException {
        ActionHelper helper = Core.getInstance(ActionHelper.class);
        Map rd = helper.getRequestData( );

        // 订单的价格等必须重新从数据库取, 避免客户端篡改
        if ("bill".equals(unit) && "create".equals(func)) {
            rd.put("stat", "待付");
            List<Map> items = Synt.asList(rd.get("items"));
            Map<Object, Map> map = new HashMap();
            for (Map item : items) {
                map.put(item.get("food_id"), item);
            }
            Set  ids = map.keySet();
            Data mod = Data.getInstance("centre/data/dining/food", "food"   );
            List<Map> foods = mod.getAll(Synt.mapOf("id", ids, "rb", "id,name,price,worth"));
            StringBuilder names = new StringBuilder();
            float total = 0;
            for (Map  food  : foods) {
                String id = (String) food.get("id");
                names.append((String) food.get("name")).append(", ");
                total += Synt.asFloat(food.get("price"));
                map.get(id).put("price", food.get("price"));
            }
            rd.put("total", total);
        }

        // 代理请求
        new ActionRunner(helper, "centre/auto/" + func).doAction();
        Map sd = helper.getResponseData();
                 helper.reply(sd );
        if (! Synt.asBool(sd.get("ok")) ) {
            return;
        }

        // 设置清单
        if (rd.containsKey("items")) {
            Data data = Data.getInstance("centre/data/dining/" + unit + "/item", "item");
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
