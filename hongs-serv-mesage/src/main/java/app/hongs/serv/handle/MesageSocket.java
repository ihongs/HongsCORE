package app.hongs.serv.handle;

import app.hongs.Core;
import app.hongs.CoreLogger;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.SocketHelper;
import app.hongs.action.VerifyHelper;
import app.hongs.serv.mesage.MesageChatWorker;
import app.hongs.serv.mesage.MesageHelper;
import app.hongs.util.Data;
import app.hongs.util.Synt;
import app.hongs.util.verify.Wrongs;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.websocket.OnOpen;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

/**
 * 消息连通器
 * @author Hongs
 */
@ServerEndpoint(
        value = "/handle/mesage/socket/{rid}",
        configurator = SocketHelper.Config.class)
public class MesageSocket {

    @OnOpen
    public void onOpen(Session sess) {
        SocketHelper hepr = SocketHelper.getInstance(sess);
        try {
            Map           prop = sess.getUserProperties( );
            Map           data;
            VerifyHelper  veri;
            Producer      prod;
            String        pipe;

            pipe = MesageHelper.getProperty("core.mesage.chat.topic");
            prod = MesageHelper.newProducer();
            veri = new VerifyHelper();
            data = new HashMap();
            veri.isPrompt(true );

            /**
             * 这里相较 Action 的校验不同
             * 不能使用 ActionHelper 获取请求数据
             * 只能通过 VerifyHelper 的值传递会话
             * 校验过程中以此提取请求、会话数据等
             */
            try {
                data.put(Session.class.getName(), sess ); // 会话

                veri.addRulesByForm("mesage", "connect");
                data = veri.verify(data);
                veri.getRules( ).clear();
                veri.addRulesByForm("mesage", "message");
            } catch (Wrongs wr ) {
                hepr.reply( wr.toReply(( byte ) 9 ));
                return;
            } catch (HongsException ex) {
                hepr.fault(ex.getLocalizedMessage());
                return;
            }

            // 注入环境备后用
            prop.put("data", data);
            prop.put("pipe", pipe);
            prop.put(Producer.class.getName(), prod);
            prop.put(VerifyHelper.class.getName(), veri);

            setSession(sess, true); // 登记会话
        }
        catch (Exception|Error er) {
            CoreLogger.error ( er);
        }
        finally {
            hepr.destroy(); // 销毁环境
        }
    }

    @OnClose
    public void onClose(Session sess) {
        SocketHelper hepr = SocketHelper.getInstance(sess);
        try {
            setSession(sess,false); // 删除会话
        }
        catch (Exception|Error er) {
            CoreLogger.error ( er);
        }
        finally {
            hepr.destroy(); // 销毁环境
        }
    }

    @OnError
    public void onError(Session sess, Throwable ta) {
        try {
            CoreLogger.debug ( ta.getMessage() );
        }
        finally {
            onClose(sess);
        }
    }

    @OnMessage
    public void onMessage(Session sess, String msg, @PathParam("rid") String rid) {
        SocketHelper hepr = SocketHelper.getInstance(sess);
        try {
            Map          prop = sess.getUserProperties();
            Map          data = (Map   ) prop.get("data");
            String       pipe = (String) prop.get("pipe");
            Producer     prod = (Producer) prop.get(Producer.class.getName());
            VerifyHelper veri = (VerifyHelper) prop.get(VerifyHelper.class.getName());

            // 解析数据
            Map dat;
            if (msg.startsWith("{") && msg.endsWith("}")) {
                dat = (  Map  ) Data.toObject(msg);
            } else {
                dat = ActionHelper.parseQuery(msg);
            }

            // 验证数据
            String uid, id, tm;
            try {
                dat.putAll(data);
                dat = veri.verify( dat );
                 id = Core.getUniqueId();
                dat.put("id",id);
                 tm = Synt.declare(dat.get("time"), "");
                uid = Synt.declare(dat.get("uid" ), "");
            } catch (Wrongs wr ) {
                hepr.reply( wr.toReply(( byte ) 9 ));
                return;
            } catch (HongsException ex ) {
                hepr.fault(ex.getLocalizedMessage());
                return;
            }
            msg = rid+","+uid+","+id+","+tm+"|"+Data.toString(dat);

            prod.send(new ProducerRecord< >(pipe , rid , msg));
            if (Core.DEBUG > 0) {
                CoreLogger.trace("Send to {}: {}", pipe, msg );
            }
        }
        catch (Exception|Error er) {
            CoreLogger.error ( er);
        }
        finally {
            hepr.destroy(); // 销毁环境
        }
    }

    //** 静态工具方法 **/

    public  static final Set<Session > conns = new HashSet();
    public  static final Set<Runnable> works = new HashSet();
    public  static final Map<String, Set<Session>> roomConns = new HashMap(); // room_id:[Session]
    public  static final Map<String, Set<Session>> userConns = new HashMap(); // user_id:[Session]
    public  static final Map<String, Set<String >> roomUsers = new HashMap(); // room_id:[user_id]

    synchronized private static void setSession(Session sess, boolean add) {
        SocketHelper hlpr = (SocketHelper) sess.getUserProperties().get(SocketHelper.class.getName());
        String rid = hlpr.getParameter("rid");
        String uid = hlpr.getParameter("uid");
        Set roomConn = roomConns.get(rid);
        Set userConn = userConns.get(uid);
        Set roomUser = roomUsers.get(rid);

        if (add) {
            conns.add(sess);

            if (roomConn == null) {
                roomConn  = new HashSet();
                roomConns.put(rid, roomConn);
            }
            roomConn.add(sess);

            if (userConn == null) {
                userConn  = new HashSet();
                roomConns.put(rid, userConn);
            }
            userConn.add(sess);

            if (roomUser == null) {
                roomUser  = new HashSet();
                roomUsers.put(rid, roomUser);
            }
            roomUser.add(uid );
        } else {
            conns.remove(sess);

            if (roomConn != null) {
                roomConn.remove(sess);
                if (roomConn.isEmpty()) {
                    roomConns.remove(rid);
                }
            }

            if (userConn != null) {
                userConn.remove(sess);
                if (userConn.isEmpty()) {
                    userConns.remove(uid);
                }
            }

            if (roomUser != null) {
                roomUser.remove(uid );
                if (roomUser.isEmpty()) {
                    roomUsers.remove(rid);
                }
            }
        }

        /**
         * 首次调用时启动工作线程
         */
        if (works.isEmpty()) {
            String topic;
            String group;
            int    count;

            // 聊天
            topic = MesageHelper.getProperty("core.mesage.chat.topic");
            group = MesageHelper.getProperty("core.mesage.chat.group");
            count = Synt.asserts(MesageHelper.getProperty("core.mesage.chat.works"), 0);
            for (int i = 0; i < count; i ++) {
              works.add(new MesageChatWorker(group, topic, roomConns));
            }

            // 分发
            group = MesageHelper.getProperty("core.mesage.dist.group");
            count = Synt.asserts(MesageHelper.getProperty("core.mesage.dist.works"), 0);
            for (int i = 0; i < count; i ++) {
              works.add(new MesageChatWorker(group, topic, roomConns));
            }

            // 记录
            group = MesageHelper.getProperty("core.mesage.keep.group");
            count = Synt.asserts(MesageHelper.getProperty("core.mesage.keep.works"), 0);
            for (int i = 0; i < count; i ++) {
              works.add(new MesageChatWorker(group, topic, roomConns));
            }

            // 通知
            topic = MesageHelper.getProperty("core.mesage.note.topic");
            group = MesageHelper.getProperty("core.mesage.note.group");
            count = Synt.asserts(MesageHelper.getProperty("core.mesage.note.works"), 0);
            for (int i = 0; i < count; i ++) {
              works.add(new MesageChatWorker(group, topic, roomConns));
            }

            // 把这些工作线程都启动起来
            for ( Runnable run : works) {
                new Thread(run).start();
            }
        }
    }

}
