package t.metrics;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.sitech.crmpd.idmm.client.MessageContext;
import com.sitech.crmpd.idmm.client.api.Message;
import com.sitech.crmpd.idmm.client.api.PropertyOption;
import com.sitech.crmpd.idmm.client.api.PullCode;
import com.sitech.crmpd.idmm.client.api.ResultCode;
import com.sitech.crmpd.idmm.client.pool.PooledMessageContextFactory;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;

import java.io.FileInputStream;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Subscriber implements Runnable
{
    final MetricRegistry metrics = new MetricRegistry();
    final ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build();
    final Timer timer = metrics.timer("subscriber");

    public static void main(String[] args) throws Exception {
        new Subscriber().start(args);
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

    private String zookeeperAddr;
    private int timeOut;
    private String clientID;
    private String topic;
    private KeyedObjectPool<String, MessageContext> pool;

    private String description = "";
    private long noMessageSleepMS = 0;
    private int threadnum = 50;

    public void setupTest(Properties p)
    {
        this.zookeeperAddr = p.getProperty("zkAddr", "");
        timeOut = Integer.parseInt(p.getProperty("timeOut", "60") );
        this.clientID = p.getProperty("clientID", "");
        this.topic = p.getProperty("topic", "");
        noMessageSleepMS = Long.parseLong(
                p.getProperty("noMessageSleepMS", "0"));
        threadnum = Integer.parseInt(p.getProperty("threadnum", "50"));

        GenericKeyedObjectPoolConfig config = new GenericKeyedObjectPoolConfig();
        config.setMaxTotal(threadnum);
        config.setMaxTotalPerKey(threadnum);
        config.setMaxIdlePerKey(threadnum);
        pool = new GenericKeyedObjectPool(
                new PooledMessageContextFactory(this.zookeeperAddr, timeOut*1000), config);
    }

    public void run()
    {
        long processingTime = timeOut;

        String msgid = null;
        PullCode code = null;
        MessageContext context = null;
        try
        {
            context = (MessageContext)pool.borrowObject(clientID);
            long t1=System.currentTimeMillis(), t2;
            while(true) {
                code = msgid == null ? null : PullCode.COMMIT_AND_NEXT;
                Message message = context.fetch(this.topic, processingTime,
                        msgid, code, this.description, false);
                ResultCode resultCode = message.getEnumProperty(PropertyOption.RESULT_CODE, ResultCode.class);
                msgid = null;
                if (resultCode == ResultCode.OK) //ResultCode.NO_MORE_MESSAGE)
                {
                    msgid = message.getId();
                } else if (resultCode == ResultCode.NO_MORE_MESSAGE) {
                    if (noMessageSleepMS > 0) {
                        try {
                            Thread.sleep(noMessageSleepMS);
                        } catch (Exception e) {
                        }
                    }
                }
                t2 = System.currentTimeMillis();
                timer.update(t2-t1, TimeUnit.MILLISECONDS);
                t1 = t2;
            }
        }
        catch (Exception e)
        {
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