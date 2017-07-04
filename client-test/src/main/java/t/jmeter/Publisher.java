package t.jmeter;

import com.sitech.crmpd.idmm.client.MessageContext;
import com.sitech.crmpd.idmm.client.api.Message;
import com.sitech.crmpd.idmm.client.api.PropertyOption;
import com.sitech.crmpd.idmm.client.pool.PooledMessageContextFactory;
import java.util.Random;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;
import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;

public class Publisher extends AbstractJavaSamplerClient
{
//    private SampleResult results;
    private String brokeAddr;
    private String timeOut;
    private String clientID;
    private String topic;
    private String password;
    private String message;
    private KeyedObjectPool<String, MessageContext> pool;
    private Random random = new Random(100000000L);
    private MessageContext context = null;
    private int poolsize = 50;

    public void setupTest(JavaSamplerContext jsc)
    {
//        results = new SampleResult();

        brokeAddr = jsc.getParameter("brokeAddr", "");
        timeOut = jsc.getParameter("timeOut", "60000");
        clientID = jsc.getParameter("clientID", "");
        topic = jsc.getParameter("topic", "");
        password = jsc.getParameter("password", "");
        message = jsc.getParameter("message", "");
        poolsize = jsc.getIntParameter("poolsize", 50);

        GenericKeyedObjectPoolConfig config = new GenericKeyedObjectPoolConfig();
        config.setMaxTotal(poolsize);
        config.setMaxTotalPerKey(poolsize);
        config.setMaxIdlePerKey(poolsize);
        pool = new GenericKeyedObjectPool(
                new PooledMessageContextFactory(this.brokeAddr, Integer.parseInt(this.timeOut)));
    }


    public SampleResult runTest(JavaSamplerContext arg0)
    {
        boolean flag = true;
        String err_msg = "";

        SampleResult results = new SampleResult();
        results.sampleStart();

        MessageContext context = null;
        try
        {
            context = (MessageContext)pool.borrowObject(clientID);
            Message message = Message.create(this.message);

            message.setProperty(PropertyOption.GROUP, String.valueOf(Math.abs(this.random.nextInt())));
//            message.setProperty(PropertyOption.VISIT_PASSWORD, this.password);

            String id = context.send(this.topic, message);

            context.commit(new String[] { id });
        }
        catch (Exception e) {
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

