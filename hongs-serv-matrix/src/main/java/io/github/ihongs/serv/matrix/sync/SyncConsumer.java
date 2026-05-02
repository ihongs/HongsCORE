package io.github.ihongs.serv.matrix.sync;

import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.CruxException;
import io.github.ihongs.serv.matrix.Data;
import io.github.ihongs.util.Synt;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

/**
 * 数据变更同步
 * @author Hongs
 */
public class SyncConsumer implements Runnable {

    @Override
    public void run() {
        Properties props;
        String     topic;
        KafkaConsumer  <String, SyncRecord> consumer;
        ConsumerRecords<String, SyncRecord> records ;

        props    = CoreConfig.getInstance("matrix-kafka");
        System.out.println(props.getProperty("bootstrap.servers"));
        topic    = props.getProperty("matrix.sync.topic", "masync.sync");
        topic    = "^"+Pattern.quote(topic)+"(?!"+Pattern.quote(Core.SERVER_ID)+"$)"; // topic 开头, 非 SERVER_ID 结尾
        consumer = new KafkaConsumer(props);        
        consumer.subscribe(Pattern.compile(topic));

        System.out.println("1111111111111111");
        
        records  = consumer.poll(1000);
        for(ConsumerRecord<String, SyncRecord> reco : records) {
            SyncRecord rec = reco.value();
            System.out.println(rec.toString());
            try {
                Data.getInstance(rec.conf, rec.form).rev(rec.id, Synt.mapOf(), 0);
            }
            catch (CruxException ex) {
                CoreLogger.error(ex);
            }
        }
    }

}
