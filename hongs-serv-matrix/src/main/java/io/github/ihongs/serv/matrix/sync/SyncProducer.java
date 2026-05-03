package io.github.ihongs.serv.matrix.sync;

import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CruxExemption;
import io.github.ihongs.serv.matrix.util.DataDiffuser;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.DeliveryMode;
import jakarta.jms.JMSException;
import jakarta.jms.MapMessage;
import jakarta.jms.MessageProducer;
import jakarta.jms.Session;
import jakarta.jms.Topic;

/**
 * 数据变更广播 (Java 11+)
 * 使用标准 Jakarta JMS MapMessage 格式
 * @author Hongs
 */
@Core.Singleton
public class SyncProducer implements DataDiffuser {

    private final Session session;
    private final MessageProducer producer;

    private SyncProducer () {
        ConnectionFactory factory;
        Connection        connection;
        Topic             topic;

        CoreConfig cc = CoreConfig.getInstance("matrix");
        String className = cc.getProperty("matrix.sync.connection");
        String brokerUrl = cc.getProperty("matrix.sync.broker.url");
        String topicName = cc.getProperty("matrix.sync.topic.name" , "matrix.sync");

        try {
            factory = JmsFactory.createConnectionFactory( className, brokerUrl );
            connection = factory.createConnection(  );
            connection.start();
            session  = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            topic    = session.createTopic(topicName);
            producer = session.createProducer (topic);
            producer.setDeliveryMode (DeliveryMode.PERSISTENT);
        }
        catch (JMSException ex) {
            throw new CruxExemption(ex, "SyncProducer failed");
        }
    }

    public static SyncProducer getInstance() {
        return Core.getInterior().got(SyncProducer.class.getName(), () -> new SyncProducer());
    }

    @Override
    public void update(String conf, String form, String id) {
        try {
            MapMessage msg = session.createMapMessage();
            msg.setString("sid" , Core.SERVER_ID);
            msg.setString("conf", conf);
            msg.setString("form", form);
            msg.setString( "id" ,  id );
            msg.setInt   ("act" ,  1  );
            producer.send(msg);
        } catch (JMSException ex) {
            throw new CruxExemption("Failed to send update message", ex);
        }
    }

    @Override
    public void delete(String conf, String form, String id) {
        try {
            MapMessage msg = session.createMapMessage();
            msg.setString("sid" , Core.SERVER_ID);
            msg.setString("conf", conf);
            msg.setString("form", form);
            msg.setString( "id" ,  id );
            msg.setInt   ("act" ,  0  );
            producer.send(msg);
        } catch (JMSException ex) {
            throw new CruxExemption("Failed to send delete message", ex);
        }
    }

}
