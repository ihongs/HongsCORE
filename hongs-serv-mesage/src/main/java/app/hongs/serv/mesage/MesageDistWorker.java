package app.hongs.serv.mesage;

import java.util.Arrays;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.Producer;

/**
 *
 * @author Hongs
 */
public class MesageDistWorker implements Runnable {

    volatile private boolean  exit ;
    final    private Consumer cons ;
    final    private Producer prod ;
    
    public MesageDistWorker (String group, String topic, String outer) {
        this.cons = MesageHelper.newConsumer(group, null, 0);
        this.cons.subscribe ( Arrays.asList (topic) );
        this.prod = MesageHelper.newProducer();
        this.exit = false;
    }

    public void die() {
        exit  = true;
    }

    @Override
    public void run() {
        
    }

}
