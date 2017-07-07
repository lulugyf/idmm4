package t.metrics;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.sitech.crmpd.idmm.client.MessageContext;
import com.sitech.crmpd.idmm.client.api.Message;
import com.sitech.crmpd.idmm.client.api.PropertyOption;
import com.sitech.crmpd.idmm.client.pool.PooledMessageContextFactory;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;

import java.io.FileInputStream;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Publisher implements Runnable
{
    final MetricRegistry metrics = new MetricRegistry();
    final ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build();
    final Timer timer = metrics.timer("publisher");

    public static void main(String[] args) throws Exception {
        new Publisher().start(args);
    }
    public void start(String[] args) throws Exception{
        Properties p = new Properties();
        FileInputStream fin = new FileInputStream(args[0]);
        p.load(fin);
        fin.close();

        setupTest(p);

        reporter.start(20, TimeUnit.SECONDS);

        for(int i=0; i<threadnum; i++){
            new Thread(this).start();
        }
    }

    private String zkAddr;
    private String timeOut;
    private String clientID;
    private String topic;
    private String password;
    private String message;
    private KeyedObjectPool<String, MessageContext> pool;
    private Random random = new Random(System.currentTimeMillis());
    private MessageContext context = null;
    private int threadnum = 50;

    public void setupTest(Properties p)
    {
        zkAddr = p.getProperty("zkAddr", "");
        timeOut = p.getProperty("timeOut", "60000");
        clientID = p.getProperty("clientID", "");
        topic = p.getProperty("topic", "");
//        password = p.getProperty("password", "");
        message = p.getProperty("message", "");
        threadnum = Integer.parseInt(p.getProperty("threadnum", "50"));

        GenericKeyedObjectPoolConfig config = new GenericKeyedObjectPoolConfig();
        config.setMaxTotal(threadnum);
        config.setMaxTotalPerKey(threadnum);
        config.setMaxIdlePerKey(threadnum);
        pool = new GenericKeyedObjectPool(
                new PooledMessageContextFactory(zkAddr, Integer.parseInt(timeOut)*1000));
    }


    public void run()
    {
        MessageContext context = null;
        try
        {
            context = (MessageContext)pool.borrowObject(clientID);
            long t1=System.currentTimeMillis(), t2;
            while(true) {
                Message message = Message.create(this.message);

                message.setProperty(PropertyOption.GROUP, String.valueOf(Math.abs(this.random.nextInt())));
//            message.setProperty(PropertyOption.VISIT_PASSWORD, this.password);

                String id = context.send(this.topic, message);

                context.commit(new String[]{id});
                t2 = System.currentTimeMillis();
                timer.update(t2-t1, TimeUnit.MILLISECONDS);
                t1 = t2;
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }finally{
            if(context != null) {
                try {
                    pool.returnObject(clientID, context);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

}

