package io.github.ihongs.serv.matrix.sync;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreConfig;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.CruxException;
import io.github.ihongs.CruxExemption;
import io.github.ihongs.action.CommitRunner;
import io.github.ihongs.db.DB;
import io.github.ihongs.db.Table;
import io.github.ihongs.db.link.Loop;
import io.github.ihongs.serv.matrix.Data;
import io.github.ihongs.util.Synt;
import io.github.ihongs.util.daemon.Chore;
import java.util.Map;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSException;
import jakarta.jms.MapMessage;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Session;
import jakarta.jms.Topic;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.lucene.document.Document;

/**
 * 数据变更同步 (Java 11+)
 * 使用标准 Jakarta JMS MapMessage 格式
 * @author Hongs
 */
public class SyncConsumer implements Runnable {

    @Override
    public void run() {
        // 同步线程
        Chore.getInstance().run(() -> {
            Core core = Core.getInstance();
            Core.ACTION_NAME.set("matrix.sync.consumer");
            core.set(Cnst.REFLUX_MODE, true); // 开启事务, 分批提交

            ConnectionFactory factory   ;
            Connection        connection;
            Session           session   ;
            Topic             topic     ;
            MessageConsumer   consumer  ;

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
                consumer = session.createConsumer (topic);

                CoreLogger.info("SyncConsumer started, listening on topic: {}", topicName);

                DB       db  = DB.getInstance("matrix");
                Table    tb  = db.getTable   ( "data" );
                String   sql = "SELECT * FROM "+tb.tableName+" WHERE id = ? AND etime = ?";
                Object[] pms = new Object [] { "", 00 };

                /**
                 * 间隔一段时间或累计一定数量提交一次
                 */
                final Set <Data>   mods = new HashSet();
                final AtomicInteger cnt = new AtomicInteger(0);
                final int max = 1000; // 1000 条
                final int sec = 60  ; // 60 秒
                Chore.getInstance().ran(() -> {
                    if (mods.isEmpty()) {
                        return;
                    }
                    synchronized (mods) {
                        for (Data mod : mods) {
                            mod.commit();
                            mod.close ();
                        }
                        mods.clear();
                        cnt .set(00);
                    }
                }, sec, sec);

                while (true) {
                    Message msg = consumer.receive();
                    if (msg instanceof MapMessage) {
                        MapMessage mm = (MapMessage) msg;
                        String sid = mm.getString("sid");
                        if (Core.SERVER_ID.equals( sid )) {
                            continue;
                        }

                        String  conf  = mm.getString("conf");
                        String  form  = mm.getString("form");
                        String  id    = mm.getString( "id" );

                        CoreLogger.trace("SyncConsumer sync: {}", mm);

                        try {
                            Data     mod;
                            Map      one;
                            Map      map;
                            Document doc;

                            pms[0] = id ;
                            mod = Data.getInstance(conf, form);
                            one = db  .  fetchOne (sql , pms );
                            if (one == null || one.isEmpty( )) {
                                continue;
                            }

                            // 状态以数据库记录为准
                            if ( 0 != Synt.declare(one.get("state") , 1) ) {
                                map = mod.getData((String) one.get("data"));
                                doc = mod.padDoc(map);
                                mod . setDoc(id, doc);
                            } else {
                                mod . delDoc(id);
                            }
                            mods.add(mod);

                            // 达量提交
                            int num  = cnt.incrementAndGet();
                            if (num >= max) {
                                synchronized (mods) {
                                    for (Data mob : mods) {
                                        mob.commit();
                                        mob.close ();
                                    }
                                    mods.clear();
                                    cnt .set(00);
                                }
                            }
                        }
                        catch (CruxException ex) {
                            CoreLogger.error(ex);
                        }
                    }

                    if (Thread.interrupted()) {
                        break ;
                    }
                }
            }
            catch ( JMSException ex) {
                throw new CruxExemption(ex, "SyncConsumer failed");
            }
            catch (CruxException ex) {
                throw ex.toExemption ();
            }
        });

        // 计划任务, 为免遗漏, 每天同步前两天的
        Chore.getInstance().runDaily(() -> {
            Core core = Core.getInstance();
            Core.ACTION_NAME.set("matrix.sync.consumer");
            core.set(Cnst.REFLUX_MODE, true); // 开启事务, 分批提交

            try {
                DB       db  = DB.getInstance("matrix");
                Table    tb  = db.getTable   ( "data" );
                String   sql = "SELECT * FROM "+tb.tableName+" WHERE etime = ? AND ctime > ?";
                Object[] pms = new Object[]{0, System.currentTimeMillis() / 1000 - 86400 * 2};

                int l = 1000;
                int i = 0;
                Loop loop;
                while (true) {
                    loop = db.query(sql, i, l, pms);
                    i = i + l;

                    Set <Data> mods = new HashSet();

                    int j = 0;
                    for (Map one : loop) {
                        try {
                            String  id  = Synt.asString(one.get( "id" ));
                            String conf = Synt.asString(one.get("conf"));
                            String form = Synt.asString(one.get("form"));
                            Data   mod  = Data.getInstance( conf, form );
                            mods.add(mod);

                            if ( 0 != Synt.declare(one.get("state") , 1) ) {
                                Map      map;
                                Document doc;
                                map = mod.getData((String) one.get("data"));
                                doc = mod.padDoc(map);
                                mod . setDoc(id, doc);
                            } else {
                                mod . delDoc(id);
                            }
                        }
                        catch (CruxException ex) {
                            CoreLogger.error(ex);
                        }
                        j ++ ;
                    }
                    i = i + j;

                    // 提交此批次
                    if (j > 0) {
                        for (Data mod : mods) {
                            mod.commit();
                        }
                    }

                    // 不足即完成
                    if (j < l) {
                        break;
                    }

                    if (Thread.interrupted()) {
                        break;
                    }
                }

                CoreLogger.info("SyncConsumer sync {} items today", i);
            }
            catch (CruxException ex) {
                CoreLogger.error(ex);
            }
            finally {
                core.close();
            }
        });
    }

}
