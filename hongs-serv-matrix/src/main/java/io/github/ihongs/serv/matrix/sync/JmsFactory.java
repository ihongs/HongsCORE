package io.github.ihongs.serv.matrix.sync;

import io.github.ihongs.CruxExemption;
import java.lang.reflect.Constructor;
import jakarta.jms.ConnectionFactory;

/**
 * JMS 连接工厂创建器 (Java 11+)
 * @author Hongs
 */
public class JmsFactory {

    private JmsFactory() {
    }

    /**
     * 根据配置创建 ConnectionFactory
     * @param className 连接工厂类
     * @param brokerUrl 连接 URL
     * @return ConnectionFactory 实例
     */
    public static ConnectionFactory createConnectionFactory(String className, String brokerUrl) {
        try {
            Class<?> factoryClass = Class.forName(className);

            try {
                Constructor<?> constructor = factoryClass.getConstructor(String.class);
                return (ConnectionFactory) constructor.newInstance(brokerUrl);
            } catch (NoSuchMethodException e1) {
                Constructor<?> constructor = factoryClass.getConstructor();
                return (ConnectionFactory) constructor.newInstance();
            }
        } catch (ClassNotFoundException e) {
            throw new CruxExemption(e, "JMS ConnectionFactory class not found: " + className);
        } catch (Exception e) {
            throw new CruxExemption(e, "Failed to create JMS ConnectionFactory: "+ className);
        }
    }

}