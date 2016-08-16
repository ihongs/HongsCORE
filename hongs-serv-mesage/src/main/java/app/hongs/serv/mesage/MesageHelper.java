/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package app.hongs.serv.mesage;

import app.hongs.CoreConfig;
import java.util.Properties;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;

/**
 *
 * @author Hongs
 */
public class MesageHelper {

    private static final CoreConfig propertyConfig;
    private static final CoreConfig producerConfig;
    private static final CoreConfig consumerConfig;

    static {
        propertyConfig = new CoreConfig("mesage").padDefaults();
        producerConfig = new CoreConfig("mesage_producer").padDefaults();
        consumerConfig = new CoreConfig("mesage_consumer").padDefaults();
    }

    public static String getProperty(String key) {
        return propertyConfig.getProperty ( key);
    }

    public static KafkaProducer newProducer() {
        return new KafkaProducer(producerConfig);
    }

    public static KafkaConsumer newConsumer(String gid, String cid, int max) {
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

}
