package app.hongs.serv.mesage;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import javax.websocket.Session;
import org.apache.kafka.clients.consumer.Consumer;

/**
 *
 * @author Hongs
 */
public class MesageNoteWorker implements Runnable {

    volatile private boolean  exit ;
    final    private Consumer cons ;
    final    private Map<String, Set<Session>> cnns;
    
    public MesageNoteWorker(String group, String topic, Map<String, Set<Session>> cnns) {
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
        
    }

}
