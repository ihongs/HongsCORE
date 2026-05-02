package io.github.ihongs.serv.matrix.sync;

import java.util.Properties;
import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.serv.matrix.util.DataDiffuser;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

/**
 * 数据变更广播
 * @author Hongs
 */
@Core.Singleton
public class SyncProducer implements DataDiffuser {

    private final String topic;
    private final KafkaProducer<String, SyncRecord> producer;

    private SyncProducer () {
        Properties props;
        props    = CoreConfig.getInstance("matrix-kafka");
        topic    = props.getProperty("matrix.sync.topic" , "matrix.sync");
        producer = new KafkaProducer(props);
    }

    public static SyncProducer getInstance() {
        return Core.getInterior().got(SyncProducer.class.getName(), ()-> new SyncProducer());
    }

    @Override
    public void update(String conf, String form, String id) {
        this.producer.send(new ProducerRecord<String, SyncRecord>(this.topic+"."+Core.SERVER_ID, new SyncRecord (true , conf, form, id, Core.SERVER_ID)));
    }

    @Override
    public void delete(String conf, String form, String id) {
        this.producer.send(new ProducerRecord<String, SyncRecord>(this.topic+"."+Core.SERVER_ID, new SyncRecord (false, conf, form, id, Core.SERVER_ID)));
    }

}
