package app.hongs.serv.mesage;

import app.hongs.CoreLogger;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import javax.websocket.Session;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

/**
 * 聊天处理器
 *
 * 消息管道数据结构
 * ROOM_ID,USER_ID,MSG_ID,TIME|{MSG_DATA}
 * 前三项为固定数据, MSG_DATA 由 mesage.form 的 message 表单决定
 *
 * @author Hongs
 */
public class MesageChatWorker implements Runnable {

    volatile private boolean  exit ;
    final    private Consumer cons ;
    final    private Map<String, Set<Session>> cnns;

    public MesageChatWorker (String group, String topic, Map<String, Set<Session>> cnns) {
        this.cons = MesageHelper.newConsumer(group, null, 0);
        this.cons.subscribe ( Arrays.asList (topic) );
        this.cnns = cnns ;
        this.exit = false;
    }

    public void die() {
        exit  = true;
    }

    @Override
    public void run() {
        try {
            while (!exit) {
                try {
                        ConsumerRecords<String, String> rs = cons.poll(100);
                    for(ConsumerRecord <String, String> rd : rs) {
                        String rid = "";
                        String msg = rd.value();
                        int pos = msg.indexOf('|');
                        if (pos != -1) {
                            rid = msg.substring(0 , pos);
                            msg = msg.substring(1 + pos);
                            pos = rid.indexOf(',');
                            rid = rid.substring(0 , pos);
                        }
                        msg = "{\"ok\":true,\"ern\":\"\",\"err\":\"\",\"msg\":\"\",\"info\":" + msg + "}";

                        if (cnns.containsKey(rid)) {
                            for (Session sess : cnns.get(rid)) {
                                try {
                                    sess.getBasicRemote().sendText(msg);
                                } catch (IOException ex) {
                                    CoreLogger.error(ex);
                                }
                            }
                        }
                    }
                } catch (Exception | Error er) {
                    CoreLogger.error(er);
                }
            }
        }  finally {
            cons.close();
        }
    }

}
