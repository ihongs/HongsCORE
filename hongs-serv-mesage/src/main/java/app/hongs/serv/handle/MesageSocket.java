package app.hongs.serv.handle;

import app.hongs.Cnst;
import app.hongs.Core;
import app.hongs.CoreLogger;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.SocketHelper;
import app.hongs.action.VerifyHelper;
import app.hongs.serv.mesage.Mesage;
import app.hongs.serv.mesage.MesageHelper;
import app.hongs.serv.mesage.MesageWorker;
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
            Map           prop = sess.getUserProperties();
            Map           data = new HashMap();
            VerifyHelper  veri = new VerifyHelper();
            MesageWorker  queu = MesageHelper.getWorker();

            veri.isPrompt(true);

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
            } catch (HongsException ex ) {
                hepr.fault(ex.getLocalizedMessage());
                CoreLogger.error  ( ex );
                return;
            }

            // 注入环境备后用
            prop.put("data", data);
            prop.put(VerifyHelper.class.getName(), veri);
            prop.put(MesageWorker.class.getName(), queu);

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
        SocketHelper hepr = SocketHelper.getInstance(sess);
        try {
            setSession(sess,false); // 删除会话
            CoreLogger.error ( ta); // 记录异常
        }
        catch (Exception|Error er) {
            CoreLogger.error ( er);
        }
        finally {
            hepr.destroy(); // 销毁环境
        }
    }

    @OnMessage
    public void onMessage(Session sess, String msg, @PathParam("rid") String rid) {
        SocketHelper hepr = SocketHelper.getInstance(sess);
        try {
            Map          prop = sess.getUserProperties();
            Map          data = (Map) prop.get("data");
            VerifyHelper veri = (VerifyHelper) prop.get(VerifyHelper.class.getName());
            MesageWorker queu = (MesageWorker) prop.get(MesageWorker.class.getName());

            // 解析数据
            Map dat;
            if (msg.startsWith("{") && msg.endsWith("}")) {
                dat = (  Map  ) Data.toObject(msg);
            } else {
                dat = ActionHelper.parseQuery(msg);
            }

            // 验证数据
            String uid;
            String id;
            String kd;
            long   st;
            try {
                dat.putAll(data);
                dat = veri.verify( dat );
                id  = Core.newIdentity();
                uid = Synt.declare(dat.get("uid" ), "");
                kd  = Synt.declare(dat.get("kind"), "");
                st  = Synt.declare(dat.get("time"), Long.class);
                dat.put("id", id);
            } catch (Wrongs wr) {
                hepr.reply( wr.toReply(( byte ) 9 ));
                return;
            } catch (HongsException ex ) {
                hepr.fault(ex.getLocalizedMessage());
                CoreLogger.error  ( ex );
                return;
            }
            msg = Data.toString(dat);

            queu.add(new Mesage(id, uid, rid, kd, msg, st));
            
            if (Core.DEBUG > 0) {
                CoreLogger.trace("From {} to {}: {}", uid, rid, msg);
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

    public static final Set<Session> CONNS = new HashSet();
    public static final Map<String, Set<Session>> USER_CONNS = new HashMap(); // user_id:[Session]
    public static final Map<String, Set<Session>> ROOM_CONNS = new HashMap(); // room_id:[Session]
    public static final Map<String, Set<String >> ROOM_USERS = new HashMap(); // room_id:[user_id]

    synchronized private static void setSession(Session sess, boolean add) {
        SocketHelper hlpr = (SocketHelper) sess.getUserProperties().get(SocketHelper.class.getName());
        String uid = (String) hlpr.getSessibute(Cnst.UID_SES);
        String rid = hlpr.getParameter("rid");
        Set userConn = USER_CONNS.get(uid);
        Set roomConn = ROOM_CONNS.get(rid);
        Set roomUser = ROOM_USERS.get(rid);

        if (add) {
            CONNS.add(sess);

            if (userConn == null) {
                userConn  = new HashSet();
                USER_CONNS.put(uid, userConn);
            }
            userConn.add(sess);

            if (roomConn == null) {
                roomConn  = new HashSet();
                ROOM_CONNS.put(rid, roomConn);
            }
            roomConn.add(sess);

            if (roomUser == null) {
                roomUser  = new HashSet();
                ROOM_USERS.put(rid, roomUser);
            }
            roomUser.add(uid );
        } else {
            CONNS.remove(sess);

            if (userConn != null) {
                userConn.remove(sess);
                if (userConn.isEmpty()) {
                    USER_CONNS.remove(uid);
                }
            }

            if (roomConn != null) {
                roomConn.remove(sess);
                if (roomConn.isEmpty()) {
                    ROOM_CONNS.remove(rid);
                }
            }

            if (roomUser != null) {
                roomUser.remove(uid );
                if (roomUser.isEmpty()) {
                    ROOM_USERS.remove(rid);
                }
            }
        }
    }

}
