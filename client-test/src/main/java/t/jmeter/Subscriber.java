package t.jmeter;

import com.sitech.crmpd.idmm.client.MessageContext;
import com.sitech.crmpd.idmm.client.api.Message;
import com.sitech.crmpd.idmm.client.api.PropertyOption;
import com.sitech.crmpd.idmm.client.api.PullCode;
import com.sitech.crmpd.idmm.client.api.ResultCode;
import com.sitech.crmpd.idmm.client.pool.PooledMessageContextFactory;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

public class Subscriber extends AbstractJavaSamplerClient
{
//    private SampleResult results;
    private String zookeeperAddr;
    private int timeOut;
    private String clientID;
    private String topic;
    private KeyedObjectPool<String, MessageContext> pool;
    private MessageContext context = null;
    private Random random = new Random(System.currentTimeMillis());

    private PullCode code = null;
    private String description = "";
    private long noMessageSleepMS = 0;
    private int poolsize = 50;

    private static LinkedBlockingQueue<String> msgids = new LinkedBlockingQueue<String>();

    public void init()
    {
        GenericKeyedObjectPoolConfig config = new GenericKeyedObjectPoolConfig();
        config.setMaxTotal(poolsize);
        config.setMaxTotalPerKey(poolsize);
        config.setMaxIdlePerKey(poolsize);
        pool = new GenericKeyedObjectPool(
                new PooledMessageContextFactory(this.zookeeperAddr, timeOut*1000), config);
    }

    public void setupTest(JavaSamplerContext jsc)
    {
        this.zookeeperAddr = jsc.getParameter("zookeeperAddr", "");
        timeOut = Integer.parseInt(jsc.getParameter("timeOut", "60") );
        this.clientID = jsc.getParameter("clientID", "");
        this.topic = jsc.getParameter("topic", "");
        noMessageSleepMS = Long.parseLong(
                jsc.getParameter("noMessageSleepMS", "0"));
        poolsize = jsc.getIntParameter("poolsize", 50);

        init();
    }

    public SampleResult runTest(JavaSamplerContext ctx)
    {
        boolean flag = true;
        String err_msg = "";

        long processingTime = timeOut;

        SampleResult results = new SampleResult();
        results.sampleStart();
        String msgid = null;
        MessageContext context = null;
        try
        {
            context = (MessageContext)pool.borrowObject(clientID);
//            msgid = null;
//            code = null;
            msgid = msgids.poll();
            code = msgid == null ? null : PullCode.COMMIT_AND_NEXT;
            Message message = context.fetch(this.topic, processingTime,
                    msgid, code, this.description, false);
            ResultCode resultCode = message.getEnumProperty(PropertyOption.RESULT_CODE, ResultCode.class);
            if (resultCode == ResultCode.OK ) //ResultCode.NO_MORE_MESSAGE)
            {
                msgid = message.getId();
                msgids.offer(msgid);
//                code = PullCode.COMMIT;
//                Message reply = context.fetch(topic, processingTime,
//                        msgid, code, this.description, false);
//                resultCode = reply.getEnumProperty(PropertyOption.RESULT_CODE, ResultCode.class);
//                if(resultCode != ResultCode.OK) {
//                    err_msg = reply.getStringProperty(PropertyOption.CODE_DESCRIPTION);
//                    flag = false;
//                }
            }else if(resultCode == ResultCode.NO_MORE_MESSAGE){
                if(noMessageSleepMS > 0) {
                    try {
                        Thread.sleep(noMessageSleepMS);
                    } catch (Exception e) {
                    }
                }
            }else{
                flag = false;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
            err_msg = e.getMessage();
            flag = false;
        }finally{
            if(context != null) {
                try {
                    pool.returnObject(clientID, context);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        results.sampleEnd();
        results.setDataEncoding("UTF-8");
        results.setSuccessful(flag);
        results.setResponseMessage(err_msg);

        return results;
    }

    public void teardownTest(JavaSamplerContext arg0)
    {
    }
}