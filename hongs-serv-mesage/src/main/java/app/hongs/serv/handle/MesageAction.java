package app.hongs.serv.handle;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.VerifyHelper;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.Permit;
import app.hongs.action.anno.Select;
import app.hongs.action.anno.Verify;
import app.hongs.db.DB;
import app.hongs.db.util.FetchCase;
import app.hongs.db.Model;
import app.hongs.db.Table;
import app.hongs.serv.mesage.Mesage;
import app.hongs.serv.mesage.MesageHelper;
import app.hongs.serv.mesage.MesageWorker;
import app.hongs.util.Data;
import app.hongs.util.Synt;
import app.hongs.util.verify.Wrongs;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 消息处理器
 * @author Hongs
 */
@Action("handle/mesage")
public class MesageAction {

    @Action("retrieve")
    @Permit(conf="$", role={"", "handle", "manage"})
    @Select(conf="mesage", form="message")
    public void retrieve(ActionHelper helper) throws HongsException {
        DB      db = DB.getInstance("mesage");
        Model  mod = db.getModel   ( "note" );
        Map    req = helper.getRequestData( );

        // 别名映射
        if (req.containsKey("uid")) {
            req.put("user_id", req.get("uid"));
        }
        if (req.containsKey("rid")) {
            req.put("room_id", req.get("rid"));
        }
        if (req.containsKey("time")) {
            req.put("stime", req.get("time"));
        }

        // rid 不能为空
        String rid = helper.getParameter("rid");
        if (rid == null || "".equals(rid)) {
            helper.fault("Parameter 'rid' can not be empty");
            return;
        }
        // 检查操作权限
        // TODO:

        Map rsp = mod.retrieve(req);

        /**
         * 直接取出消息数据列表
         * 扔掉用于索引的数据项
         */
        List<Map> list = null;
        if (rsp.containsKey("list")) {
            list = (List<Map>) rsp.get("list");
        } else
        if (rsp.containsKey("info")) {
            Map info = ( Map ) rsp.get("info");
            list = new ArrayList();
            list.add(info);
        }
        if (list != null) {
            for (Map info : list ) {
                String msg = (String) info.get("msg");
                info.clear();
                info.putAll((Map) Data.toObject(msg));
            }
        }

        helper.reply(rsp);
    }

    @Action("room/retrieve")
    @Permit(conf="$", role={"", "handle", "manage"})
    public void retrieveRoom(ActionHelper helper) throws HongsException {
        DB      db = DB.getInstance( "mesage" );
        Map     rd = helper.getRequestData(   );
        Set    uid = Synt.asTerms(rd.get("uid"));
        String mid = (String) helper.getSessibute(Cnst.UID_SES);

        FetchCase fc = new FetchCase(FetchCase.STRICT);
        // 禁用关联
        fc.setOption("ASSOCS", new HashSet());
        // 自己在内
        Table rm = db.getTable("room_mate");
        fc.join  (rm.tableName, rm.name)
          .by    (FetchCase.LEFT)
          .on    (rm.name+".room_id = room.id")
          .filter(rm.name+".user_id = ?", mid );
        // 全部字段
        rd.remove(Cnst.RB_KEY);

        Model md = db.getModel ("room");
        Map   ro = md.retrieve (rd, fc);
        List  rs = (List)ro.get("list");

        // 追加用户信息
        MesageHelper.addUserInfo(db, rs, mid, uid);

        helper.reply(ro);
    }

    @Action("user/retrieve")
    @Permit(conf="$", role={"", "handle", "manage"})
    public void retrieveUser(ActionHelper helper) throws HongsException {
        DB      db = DB.getInstance( "mesage" );
        String uid = helper.getParameter("id" );
        String mid = (String) helper.getSessibute(Cnst.UID_SES);
        String rid ;
        Map     ro ;
        Map     rx ;

        if (uid == null || "".equals(uid)) {
            helper.fault( "ID 不能为空" );
        }

        /**
         * 任何两个用户需要聊天
         * 都必须创建一个聊天室
         */
        rid = MesageHelper.getRoomId(db, uid, mid);

        // 查询会话信息
        ro = db.fetchCase()
            .from  (db.getTable("room").tableName)
            .filter("id = ? AND state > ?", rid,0)
            .one   ();
        if (ro == null || ro.isEmpty()) {
            helper.fault("会话不存在");
        }

        // 追加好友信息
        MesageHelper.addUserInfo(db, Synt.asList(ro), mid, null);

        rx = new HashMap();
        rx.put("info", ro);

        helper.reply(rx);
    }

    @Action("create")
    @Permit(conf="$", role={"", "handle", "manage"})
    @Select(conf="mesage", form="message", mode=2)
    public void create(ActionHelper helper) throws HongsException {
        MesageWorker que = MesageHelper.getWorker();
        VerifyHelper ver = new VerifyHelper();
        Map    dat = helper.getRequestData( );
        byte   mod = Synt.declare(dat.get("md"), (byte) 0);
        Map    tmp ;

        ver.isUpdate(  false );
        ver.isPrompt(mod <= 0);

        try {
            // 验证连接数据
            ver.addRulesByForm("mesage", "connect");
            tmp = ver.verify(dat);

            // 提取主题并将连接数据并入消息数据, 清空规则为校验消息做准备
            ver.getRules().clear();
            dat.putAll(tmp);

            // 验证消息数据
            ver.addRulesByForm("mesage", "message");
            tmp = ver.verify(dat);

            String uid = (String) tmp.get("uid");
            String rid = (String) tmp.get("rid");
            String kd = (String) tmp.get("kind");
            long   st = Synt.declare(dat.get("time"), 0L);
            String id = Core.newIdentity();
            tmp.put("id", id);

            // 送入消息队列
            que.add(new Mesage(id, uid, rid, kd, Data.toString(dat), st));

            dat = new HashMap(  );
            dat.put("info" , tmp);
        } catch (Wrongs ex ) {
            dat = ex.toReply(mod);
        }

        helper.reply(dat);
    }

    @Action("file/create")
    @Permit(conf="$", role={"", "handle", "manage"})
    @Verify(conf="mesage", form="file")
    public void createFile(ActionHelper helper) {
        helper.reply("", helper.getRequestData());
    }

    @Action("image/create")
    @Permit(conf="$", role={"", "handle", "manage"})
    @Verify(conf="mesage", form="file_image")
    public void createImage(ActionHelper helper) {
        helper.reply("", helper.getRequestData());
    }

    @Action("video/create")
    @Permit(conf="$", role={"", "handle", "manage"})
    @Verify(conf="mesage", form="file_video")
    public void createVideo(ActionHelper helper) {
        helper.reply("", helper.getRequestData());
    }

    @Action("voice/create")
    @Permit(conf="$", role={"", "handle", "manage"})
    @Verify(conf="mesage", form="file_voice")
    public void createVoice(ActionHelper helper) {
        helper.reply("", helper.getRequestData());
    }
}
