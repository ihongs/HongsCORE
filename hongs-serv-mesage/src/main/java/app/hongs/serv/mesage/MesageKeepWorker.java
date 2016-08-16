package app.hongs.serv.mesage;

import app.hongs.CoreLogger;
import app.hongs.HongsException;
import app.hongs.db.DB;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import javax.websocket.Session;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

/**
 *
 * @author Hongs
 */
public class MesageKeepWorker implements Runnable {

    volatile private boolean  exit ;
    final    private Consumer cons ;
    final    private DB       db   ;
    final    private PreparedStatement ps;
    
    public MesageKeepWorker (String group, String topic) throws HongsException {
        this.cons = MesageHelper.newConsumer(group, null, 0);
        this.cons.subscribe ( Arrays.asList (topic) );
        this.exit = false;
        
        this.db = new DB("mesage");
        String tab = db.getTable("note").tableName;
        String sql = "INSERT INTO `"+tab+"` (`rid`, `uid`, `id`, `stime`, `msg`) VALUES (?, ?, ?, ?, ?)";
        this.ps = this.db.prepareStatement(sql);
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
                        String msg = rd.value();
                        int pos = msg.indexOf('|');
                        if (pos == -1) {
                            continue;
                        }
                        
                        try {
                            String   idc;
                            String[] ids;
                            idc = msg.substring(0 , pos);
                            msg = msg.substring(1 + pos);
                            ids = idc.split(",");
                        
                            db.connect();
                            ps.setString(1, ids[0]);
                            ps.setString(2, ids[1]);
                            ps.setString(3, ids[2]);
                            ps.setString(4, ids[3]);
                            ps.setString(5, msg);
                            ps.setLong(5, System.currentTimeMillis() / 1000);
                            ps.execute();
                        } catch (SQLException e) {
                            CoreLogger.error( e);
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
