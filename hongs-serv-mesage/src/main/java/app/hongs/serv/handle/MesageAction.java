package app.hongs.serv.handle;

import app.hongs.Cnst;
import app.hongs.CoreConfig;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.VerifyHelper;
import app.hongs.action.anno.Action;
import app.hongs.action.anno.Permit;
import app.hongs.action.anno.Select;
import app.hongs.db.DB;
import app.hongs.db.FetchCase;
import app.hongs.db.Model;
import app.hongs.util.Synt;
import app.hongs.util.verify.Wrongs;
import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Properties;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

/**
 * 消息处理器
 * @author Hongs
 */
@Action("handle/mesage")
public class MesageAction {

    @Action("retrieve")
    @Permit(conf="$", role={"", "handle", "manage"})
    @Select(conf="mesage", form="message")
    public void retrieve(ActionHelper helper) {
        Map    req = helper.getRequestData( );
        String tid = Synt.declare(req.get("tid"), "");
        long   bid = Synt.declare(req.get("bid"), 0L);
        int     rn = Synt.declare(req.get("rn" ), Cnst.RN_DEF);

        String top = getPrefix()+".m:"+tid;
        Consumer c = newConsumer(rn);

        // 一个 tid 相同 key，所以只有一个分区
        TopicPartition      tp = null;
        List<PartitionInfo> ps = c.partitionsFor(top);
        for (PartitionInfo  pi : ps) {
            tp = new TopicPartition(top, pi.partition( ));
        }

        // 指定 bid 上移 rn 位置, 否则从开始读
        if (tp != null) {
            if (bid == 0L) {
                bid = c.position(tp);
            }
            c.seek(tp, bid > rn ? bid - rn : 0);
        } else {
            c.seek(tp, 1L);
        }

        // 读取 rn 条记录
        List<Map> ls = new ArrayList();
            ConsumerRecords<String, Map> rs = c.poll(0);
        for(ConsumerRecord <String, Map> rd : rs) {
            Map  sd  =  rd.value ( );
            sd.put("id",rd.offset());
            ls.add( sd );
            if (0 == -- rn) {
                break;
            }
        }

        Map sd = new HashMap();
        sd.put("list", ls);
        helper.reply ( sd);
    }

    @Action("room/retrieve")
    @Permit(conf="$", role={"", "handle", "manage"})
    public void retrieveRoom(ActionHelper helper) throws HongsException {
        DB      db = DB.getInstance( "mesage" );
        Map     rd = helper.getRequestData(   );
        Set    uid = Synt.asTerms(rd.get("uid"));
        String mid = (String) helper.getSessibute(Cnst.UID_SES);

        FetchCase fc = new FetchCase(   );
        // 禁用关联
        fc.setOption("ASSOC_TYPES", new HashSet( ) );
        fc.setOption("ASSOC_JOINS", new HashSet( ) );
        // 自己在内
        fc.join (db.getTable("room_mate").tableName)
          .by   (FetchCase.LEFT )
          .on   (".rid = :id"   )
          .where(".uid = ?", mid);
        // 全部字段
        rd.remove ( Cnst.RB_KEY );

        Model   md = db.getModel ("room");
        Map     ro = md.retrieve (rd, fc);
        List    rs = (List)ro.get("list");

        // 追加用户信息
        joinUserInfo(db , rs , mid , uid);

        helper.reply(ro);
    }

    @Action("user/retrieve")
    @Permit(conf="$", role={"", "handle", "manage"})
    public void retrieveUser(ActionHelper helper) throws HongsException {
        DB      db = DB.getInstance( "mesage" );
        String uid = helper.getParameter("id" );
        String mid = (String) helper.getSessibute(Cnst.UID_SES);
        Map     ro ;
        Map     rx ;

        if (uid == null || "".equals(uid)) {
            helper.fault( "ID 不能为空" );
        }

        /**
         * 任何两个用户需要聊天
         * 都必须创建一个聊天室
         */
        ro = db.fetchCase()
            .from  (db.getTable("room_mate").tableName)
            .filter("uid = ? AND state = ?" , uid , 1 )
            .select("rid")
            .one   ();
        if (ro == null || ro.isEmpty()) {
            // 检查好友关系
            // TODO:

            // 不存在则创建
            Set<Map> rs = new HashSet();

            ro = new HashMap();
            ro.put("uid", mid);
            rs.add(ro);

            ro = new HashMap();
            ro.put("uid", uid);
            rs.add(ro);

            ro = new HashMap();
            ro.put("users",rs);
            ro.put("state", 1);

            uid = db.getModel("room").add(ro);
        } else {
            uid = ( String )  ro.get( "rid" );
        }

        // 查询会话信息
        ro = db.fetchCase()
            .from  (db.getTable("room").tableName)
            .filter("id = ? AND state > ?", uid,0)
            .one   ();
        if (ro == null || ro.isEmpty()) {
            helper.fault("会话不存在");
        }

        // 追加好友信息
        List<Map> rs = new ArrayList();
                  rs.add(ro /* Usr */);
        joinUserInfo(db, rs, mid,null);

        rx = new HashMap();
        rx.put("info", ro);

        helper.reply(rx);
    }

    @Action("create")
    @Permit(conf="$", role={"", "handle", "manage"})
    @Select(conf="mesage", form="message", mode=2)
    public void create(ActionHelper helper) throws HongsException {
        VerifyHelper ver = new VerifyHelper( );
        Map      dat = helper.getRequestData();
        byte     mod = Synt.declare(dat.get("md"), (byte) 0);
        Producer pcr = newProducer();
        String   pre = getPrefix  ();
        String   tid ;
        Map      tmp ;

        ver.isUpdate( false );
        ver.isPrompt(mod <= 0);

        try {
            // 验证连接数据
            ver.addRulesByForm("mesage", "connect");
            tmp = ver.verify(dat);

            // 提取主题并将连接数据并入消息数据, 清空规则为校验消息做准备
            tid = (String) tmp.get("tid");
            ver.getRules().clear();
            dat.putAll(tmp);

            // 验证消息数据
            ver.addRulesByForm("mesage", "message");
            tmp = ver.verify(dat);

            // 送入消息队列
            pcr.send(new ProducerRecord<>(pre+".m:"+tid, tid, tmp)); // 消息
            pcr.send(new ProducerRecord<>(pre+".n:"/**/, tid, tmp)); // 通知

            dat = new HashMap(  );
            dat.put("info" , tmp);
        } catch (Wrongs ex ) {
            dat = ex.toReply(mod);
        }

        helper.reply(dat);
    }

    @Action("file/create")
    @Permit(conf="$", role={"", "handle", "manage"})
    public void createFile(ActionHelper helper) {
        helper.reply("",helper.getRequestData());
    }

    protected String getPrefix() {
        Properties prop = new CoreConfig("mesage");
        return prop.getProperty("core.mesage.topic.prefix");
    }

    protected Producer newProducer() {
        Properties prop = new CoreConfig("mesage_consumer").clone();
        prop.put(  "key.deserializer", StringDeserializer.class);
        prop.put("value.deserializer", StringDeserializer.class);
        return new KafkaProducer(prop);
    }

    protected Consumer newConsumer(int rn) {
        Properties prop = new CoreConfig("mesage_consumer").clone();
        prop.put(  "key.deserializer", StringDeserializer.class);
        prop.put("value.deserializer", StringDeserializer.class);
        prop.setProperty("max.poll.records", String.valueOf(rn));
        return new KafkaConsumer(prop);
    }

    /**
     * 为聊天列表关联用户信息
     * 头像及说明来自用户信息
     * 名称依此按群组/好友/用户优先设置
     * @param db   必须是 mesage 数据库
     * @param rs   查到的聊天室结果集合
     * @param mid  当前会话用户ID
     * @param uids 仅查询这些用户
     * @throws HongsException
     */
    public static void joinUserInfo(DB db, List<Map> rs, String mid, Collection uids) throws HongsException {
        Map<Object, List<Map>> ump = new HashMap();
        Map<Object, Map> fmp = new HashMap();
        Map<Object, Map> gmp = new HashMap();
        List<Map> rz; // 临时查询列表

        // 获取到 tid 映射
        for(Map ro : rs) {
            if (Synt.asserts(ro.get("state"), 0) == 1) {
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
            .select ("rid, uid, name, state")
            .filter ("rid IN (?)", rids);
        if (uids != null && ! uids.isEmpty()) {
          fc.filter ("uid IN (?)", uids);
        }
        rz = fc.all ();
        for(Map rv : rz) {
            Object rid = rv.get("rid" );
            Object nid = rv.get("uid" );
            Object nam = rv.get("name");

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
                rv = new HashMap( );
                rv.put("uid" , nid);
                rv.put("name", nam);
                rv.put("head", "" );
                rv.put("note", "" );
                rv.put("isMe", mid.equals(nid));
                rx.add( rv);
                ry.add( rv);
            }
        }
        uids = ump.keySet();

        // 补充好友名称
        rz = db.fetchCase()
            .from   (db.getTable("user_mate").tableName)
            .filter ("uid IN (?) AND mid = ? AND name != ? AND name IS NOT NULL", uids, mid, "")
            .select ("uid, name")
            .all    ();
        for(Map rv : rz) {
           List<Map> rx = ump.get(rv.remove("uid"));
            for(Map  ro : rx) {
             Object nam = ro.get("name");
                if (nam != null && !"".equals(nam)) {
                    continue ; // 已有别名则不设置
                }
                ro.putAll(rv);
            }
        }

        // 补充用户信息
        rz = db.fetchCase()
            .from   (db.getTable("user").tableName)
            .filter ("id IN (?)", uids )
            .select ("id AS uid, name, head, note")
            .all    ();
        for(Map rv : rz) {
           List<Map> rx = ump.get(rv.remove("uid"));
            for(Map  ro : rx) {
             Object nam = ro.get("name");
                if (nam != null && !"".equals(nam)) {
                    continue ; // 已有别名则不设置
                }
                ro.putAll(rv);
            }
        }
    }

}
