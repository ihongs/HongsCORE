package app.hongs.serv.handle;

import app.hongs.Core;
import app.hongs.CoreConfig;
import app.hongs.CoreLogger;
import app.hongs.HongsException;
import app.hongs.action.ActionHelper;
import app.hongs.action.SocketHelper;
import app.hongs.action.VerifyHelper;
import app.hongs.util.Data;
import app.hongs.util.verify.Wrongs;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Properties;
import javax.websocket.OnOpen;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

/**
 * 消息连通器
 * @author Hongs
 */
@ServerEndpoint(
        value = "/handle/mesage/socket/{tid}",
        configurator = SocketHelper.Config.class)
public class MesageSocket {

    @OnOpen
    public void onOpen(Session sess) {
        try {
            SocketHelper.getInstance(sess); // 初始化环境

            Map           prop = sess.getUserProperties();
            Map           data;
            VerifyHelper  veri;
            Worker        worker;
            Producer      producer;
            Consumer      consumer;
            String        uid;
            String        gid;

            data = new HashMap();
            veri = new VerifyHelper();
            veri.isPrompt(true );

            /**
             * 这里相较 Action 的校验不同
             * 不能使用 ActionHelper 获取请求数据
             * 只能通过 VerifyHelper 的值传递会话
             * 校验过程中以此提取请求、会话数据等
             */
            try {
                try {
                    data.put(Session.class.getName(), sess ); // 会话

                    veri.addRulesByForm("mesage", "connect");
                    data = veri.verify(data);
                    veri.getRules( ).clear();
                    veri.addRulesByForm("mesage", "message");
                } catch (Wrongs wr ) {
                    Map sd = toReply(wr.toReply ( (byte) 0 ) );
                    sess.getBasicRemote().sendText(Data.toString(sd));
                    return;
                } catch (HongsException ex) {
                    Map sd = inReply(ex.getLocalizedMessage());
                    sess.getBasicRemote().sendText(Data.toString(sd));
                    return;
                }
            } catch (IOException ex) {
                CoreLogger.error(ex);
                return;
            }

            uid = (String) data.get("uid");
            gid = (String) data.get("gid");

            // 生产者、消费者
            producer = newProducer();
            consumer = newConsumer(gid , uid , 0 );

            // 当前会话处理器
            worker   = new Worker (consumer, sess);
                       new Thread (worker).start();

            // 注入环境备后用
            prop.put( "data", data );
            prop.put(Worker.class.getName(), worker);
            prop.put(Producer.class.getName(), producer);
            prop.put(Consumer.class.getName(), consumer);
            prop.put(VerifyHelper.class.getName(), veri);

            setSession(sess , true );
        } catch (Exception|Error er) {
            CoreLogger.error(  er  );
        } finally {
            Core.getInstance().destroy(); // 销毁环境
        }
    }

    @OnClose
    public void onClose(Session sess) {
        try {
            /**
             * 终止通道监听线程
             */
            Map prop = sess.getUserProperties();
            String n =  Worker.class.getName( );
            Worker w = (Worker) prop.get(  n  );
            w.die( );

            setSession(sess , false);
        } catch (Exception|Error er) {
            CoreLogger.error(  er  );
        }
    }

    @OnMessage
    public void onMessage(Session sess, String msg, @PathParam("tid") String tid) {
        try {
            SocketHelper.getInstance(sess); // 初始化环境

            Map          prop = sess.getUserProperties();
            Map          data = (Map) prop.get( "data" );
            VerifyHelper veri = (VerifyHelper) prop.get(VerifyHelper.class.getName());
            Producer     producer = (Producer) prop.get(/**/Producer.class.getName());

            // 解析数据
            Map dat;
            if (msg.startsWith("{") && msg.endsWith("}")) {
                dat = (  Map  ) Data.toObject(msg);
            } else {
                dat = ActionHelper.parseQuery(msg);
            }

            // 验证数据
            try {
                try {
                    dat.putAll(data);
                    dat = veri.verify(dat);
                } catch (Wrongs wr ) {
                    Map sd = toReply(wr.toReply( ( byte ) 0 ));
                    sess.getBasicRemote().sendText(Data.toString(sd));
                    return;
                } catch (HongsException ex) {
                    Map sd = inReply(ex.getLocalizedMessage());
                    sess.getBasicRemote().sendText(Data.toString(sd));
                    return;
                }
            } catch (IOException ex) {
                CoreLogger.error(ex);
                return;
            }

            if (Core.DEBUG > 0) {
                CoreLogger.trace("Send to "+topicPrefix+".m:"+tid+" "+dat);
            }

            producer.send(new ProducerRecord<>(topicPrefix+".m:"+tid, tid, dat)); // 消息
            producer.send(new ProducerRecord<>(topicPrefix+".n:"/**/, tid, dat)); // 通知
        } catch (Exception|Error er) {
            CoreLogger.error(  er  );
        } finally {
            Core.getInstance().destroy(); // 销毁环境
        }
    }

    public static class Worker implements Runnable {

     volatile  private  boolean exit = false;
        final  private  Session sess ;
        final  private Consumer cons ;

        public Worker(Consumer cons, Session sess) {
            this.cons = cons;
            this.sess = sess;
        }

        public void die() {
            exit  = true;
        }

        @Override
        public void run() {
            Map     sd;
            String  ss;

            while (!exit) {
                try {

                    ConsumerRecords<String, Map> rs = cons.poll(pollTimeout);
                for(ConsumerRecord <String, Map> rd : rs) {
                    sd = rd.value(  );
                    sd.put("id" , rd.offset());
                    sd = inReply (sd);
                    ss = Data.toString(  sd  );

                    try {
                        sess.getBasicRemote().sendText(ss);
                    } catch (IOException ex) {
                        CoreLogger.error(ex);
                    }
                }

                } catch (Exception|Error er) {
                        CoreLogger.error(er);
                } finally {
                        cons.close();
                }
            }
        }

    }

    //** 静态工具方法 **/

    public  static final Set<Session> conns = new HashSet();
    public  static final Map<String, Set<Session>> userConns = new HashMap();
    public  static final Map<String, Set<Session>> roomConns = new HashMap();

    private static final CoreConfig producerConfig;
    private static final CoreConfig consumerConfig;
    private static final String     topicPrefix;
    private static final long       pollTimeout;

    static {
        producerConfig = new CoreConfig("mesage_producer").padDefaults();
        consumerConfig = new CoreConfig("mesage_consumer").padDefaults();

        CoreConfig cnf = new CoreConfig("mesage");
        topicPrefix = cnf.getProperty("core.mesage.topic.prefix","core");
        pollTimeout = cnf.getProperty("core.mesage.poll.timeout", 100L );
    }

    private static KafkaProducer newProducer() {
        return new KafkaProducer(producerConfig);
    }

    private static KafkaConsumer newConsumer(String gid, String cid, int max) {
        Properties cnf = (Properties) consumerConfig.clone();
        if (gid != null) {
            cnf.setProperty( "group.id", gid);
        }
        if (cid != null) {
            cnf.setProperty("client.id", cid);
        }
        if (max != 0) {
            cnf.setProperty("max.poll.records", String.valueOf(max));
        }
        return new KafkaConsumer(cnf);
    }

    synchronized private static void setSession(Session sess, boolean add) {
        SocketHelper hlpr = (SocketHelper) sess.getUserProperties().get(SocketHelper.class.getName());
        String tid = hlpr.getParameter("tid");
        String uid = hlpr.getParameter("uid");
        Set roomConn = roomConns.get(tid);
        Set userConn = userConns.get(uid);

        if (add) {
            conns.add(sess);

            if (roomConn == null) {
                roomConn  = new HashSet();
                roomConns.put(tid, roomConn);
            }
            roomConn.add(sess);

            if (userConn == null) {
                userConn  = new HashSet();
                roomConns.put(tid, userConn);
            }
            userConn.add(sess);
        } else {
            conns.remove(sess);

            if (roomConn != null) {
                roomConn.remove(tid);
                if (roomConn.isEmpty()) {
                    roomConns.remove(tid);
                }
            }

            if (userConn != null) {
                userConn.remove(uid );
                if (userConn.isEmpty()) {
                    userConns.remove(uid);
                }
            }
        }
    }

    private static Map toReply(Map rd) {
        if (! rd.containsKey( "ok" )) {
            rd.put("ok", true);
        }
        if (! rd.containsKey("ern" )) {
            rd.put("ern" , "");
        }
        if (! rd.containsKey("err" )) {
            rd.put("err" , "");
        }
        if (! rd.containsKey("ern" )) {
            rd.put("msg" , "");
        }
        if (! rd.containsKey("info")) {
            rd.put("info", new HashMap());
        }
        return rd;
    }

    private static Map inReply(Map rd) {
        Map xd = new HashMap();
        xd.put("info", rd);
        return toReply(xd);
    }

    private static Map inReply(String msg) {
        Map xd = new HashMap();
        xd.put("msg", msg);
        return toReply(xd);
    }

}
