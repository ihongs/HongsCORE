package app.hongs.serv.mesage;

import app.hongs.Core;
import app.hongs.CoreConfig;
import app.hongs.HongsException;
import app.hongs.db.DB;
import app.hongs.db.util.FetchCase;
import app.hongs.util.Synt;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 消息助手
 *
 * @author Hongs
 */
public class MesageHelper {

    public static MesageWorker getWorker() {
        CoreConfig conf = CoreConfig.getInstance("mesage");
        String defn = MesageMainWorker.class.getName();
        String clsn = conf.getProperty("core.mesage.worker.class", defn);
        return (MesageWorker) Core.getInstance(clsn);
    }

    /**
     * 获得私聊区ID
     * 当不存在时会自动建立临时聊天
     * @param db
     * @param uid
     * @param mid
     * @return
     * @throws HongsException
     */
    public static String getRoomId(DB db, String uid, String mid) throws HongsException {
        Map ro = db.fetchCase()
            .setOption("CLEVER_MODE", false)
            .from  (db.getTable("room_mate").tableName, "mate1")
            .join  (db.getTable("room"     ).tableName, "room" , "mate1.room_id = room.id AND room.level = 1", FetchCase.INNER)
            .join  (db.getTable("room_mate").tableName, "mate2", "mate2.room_id = room.id")
            .filter("mate1.user_id = ? AND mate2.user_id = ?", uid, mid)
            .select("mate1.room_id")
            .one   ();

        if (ro == null || ro.isEmpty()) {
            // TODO: 检查好友关系

            // 不存在则创建
            Set<Map> rs = new HashSet();

            ro = new HashMap();
            ro.put("user_id", mid);
            rs.add(ro);

            ro = new HashMap();
            ro.put("user_id", uid);
            rs.add(ro);

            ro = new HashMap();
            ro.put("users",rs);
            ro.put("state", 1);

            return db.getModel("room").add (ro);
        } else {
            return ro.get("room_id").toString();
        }
    }

    /**
     * 为聊天列表关联用户信息
     * 头像及说明来自用户信息
     * 名称依此按群组/好友/用户优先设置
     * @param db   必须是 mesage 数据库
     * @param rs   查到的聊天区集(无关联)
     * @param mid  当前会话用户ID
     * @param uids 仅查询这些用户
     * @throws HongsException
     */
    public static void addUserInfo(DB db, List<Map> rs, String mid, Collection uids) throws HongsException {
        Map<Object, List<Map>> ump = new HashMap();
        Map<Object, Map> fmp = new HashMap();
        Map<Object, Map> gmp = new HashMap();
        List<Map> rz; // 临时查询列表

        // 获取到 rid 映射
        for(Map ro : rs) {
            if (Synt.asserts(ro.get("level"), 1) == 1) { // 私聊
                fmp.put(ro.get("id"), ro );
            }
                gmp.put(ro.get("id"), ro );
        }
        Set rids = new HashSet( );
        rids.addAll(fmp.keySet());
        rids.addAll(gmp.keySet());

        /**
         * 获取及构建 uids => 群组/用户 的映射关系
         */
        FetchCase fc = db.fetchCase()
            .from   (db.getTable("room_mate").tableName)
            .orderBy("state DESC")
            .select ("room_id, user_id, name, level")
            .filter ("room_id IN (?)", rids);
        if (uids != null && ! uids.isEmpty()) {
          fc.filter ("user_id IN (?)", uids);
        }
        rz = fc.all ();
        for(Map rv : rz) {
            Object rid = rv.get("room_id");
            Object nid = rv.get("user_id");
            Object nam = rv.get("name" );
            Object grd = rv.get("level");

            List<Map> rx; // ump 映射列表
            List<Map> ry; // gmp 成员列表
                 Map  ro; // 聊天室信息头

            // 映射关系列表
            rx = ump.get(nid);
            if (rx == null) {
                rx  = new ArrayList();
                 ump.put(nid , rx );
            }

            // 好友聊天信息
            ro = fmp.get(rid);
            if (ro != null) {
                rx.add( ro);
                ro.put("name", nam);
            }

            // 群组成员列表
            ro = gmp.get(rid);
            if (ro != null) {
                ry = (List) ro.get("users");
                if (ry == null) {
                    ry  = new ArrayList();
                    ro.put("users" , ry );
                }
                rv = new HashMap();
                rv.put("user_id", nid);
                rv.put("name"   , nam);
                rv.put("head"   , "" );
                rv.put("note"   , "" );
                rv.put("grade"  , grd);
                rv.put("level"  , mid.equals(nid) ? "-1" : "0"); // -1 自己, 0 陌生, 1 临时, 2 好友
                rx.add( rv);
                ry.add( rv);
            }
        }
        uids = ump.keySet();

        // 补充好友名称
        rz = db.fetchCase()
            .from   (db.getTable("user_mate").tableName)
            .filter ("user_id IN (?) AND mate_id = ?", uids, mid)
            .select ("user_id AS uid, name, level")
            .all    ();
        for(Map rv : rz) {
           List<Map> rx = ump.get(rv.remove("uid"));
            for(Map  ro : rx) {
                Object nam = ro.get("name");
                if (nam == null ||  "".equals(nam)) {
                Object nem = rv.get("name");
                if (nem != null && !"".equals(nem)) {
                    ro.put("name", nem);
                }}
                ro.put("level", rv.get("level"));
            }
        }

        // 补充用户信息
        rz = db.fetchCase()
            .from   (db.getTable("user").tableName)
            .filter ("id IN (?)", uids )
            .select ("id AS uid, name, note, head")
            .all    ();
        for(Map rv : rz) {
           List<Map> rx = ump.get(rv.remove("uid"));
            for(Map  ro : rx) {
                Object nam = ro.get("name");
                if (nam == null ||  "".equals(nam)) {
                Object nem = rv.get("name");
                if (nem != null && !"".equals(nem)) {
                    ro.put("name", nem);
                }}
                ro.put("note", rv.get("note"));
                ro.put("head", rv.get("head"));
            }
        }
    }

}
