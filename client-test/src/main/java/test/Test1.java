package test;

import com.sitech.crmpd.idmm.client.MessageContext;
import com.sitech.crmpd.idmm.client.api.Message;
import com.sitech.crmpd.idmm.client.api.PropertyOption;
import com.sitech.crmpd.idmm.client.api.PullCode;
import com.sitech.crmpd.idmm.client.api.ResultCode;
import com.sitech.crmpd.idmm.client.pool.PooledMessageContextFactory;
import org.apache.commons.cli.*;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;

import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by guanyf on 7/4/2017.
 */
public class Test1 {
    private String zookeeperAddr;
    private int timeOut = 60;
    private String clientID;
    private String topic;
    private KeyedObjectPool<String, MessageContext> pool;
    private MessageContext context = null;
    private Random random = new Random(System.currentTimeMillis());

    private PullCode code = null;
    private String description = "";
    private long noMessageSleepMS = 1000;
    private int poolsize = 50;

    private static LinkedBlockingQueue<String> msgids = new LinkedBlockingQueue<String>();

    public void setupTest() {
        topic = "TRecOprCnttDest";
        clientID = "Sub119Opr";
        zookeeperAddr = "127.0.0.1:2181/idmm4/broker";
        GenericKeyedObjectPoolConfig config = new GenericKeyedObjectPoolConfig();
        config.setMaxTotal(poolsize);
        config.setMaxTotalPerKey(poolsize);
        config.setMaxIdlePerKey(poolsize);
        pool = new GenericKeyedObjectPool(
                new PooledMessageContextFactory(zookeeperAddr, timeOut*1000), config);

    }

    public boolean consume() {
        String msgid = null;
        boolean flag = false;
        MessageContext context = null;
        try
        {
            context = (MessageContext)pool.borrowObject(clientID);
            msgid = msgids.poll();
            code = msgid == null ? null : PullCode.COMMIT_AND_NEXT;
            Message message = context.fetch(this.topic, timeOut,
                    msgid, code, this.description, false);
            ResultCode resultCode = message.getEnumProperty(PropertyOption.RESULT_CODE, ResultCode.class);
            if (resultCode == ResultCode.OK ) //ResultCode.NO_MORE_MESSAGE)
            {
                msgid = message.getId();
                msgids.offer(msgid);
                System.out.println(msgid);
//                code = PullCode.COMMIT;
//                Message reply = context.fetch(topic, timeOut,
//                        msgid, code, this.description, false);
//                resultCode = reply.getEnumProperty(PropertyOption.RESULT_CODE, ResultCode.class);
//                if(resultCode != ResultCode.OK) {
//                    System.out.println(resultCode + "::"
//                            +reply.getStringProperty(PropertyOption.CODE_DESCRIPTION) );
//                }else{
//                    flag = true;
//                }
            }else if(resultCode == ResultCode.NO_MORE_MESSAGE){
//                if(noMessageSleepMS > 0) {
//                    try {
//                        Thread.sleep(noMessageSleepMS);
//                    } catch (Exception e) {
//                    }
//                }
                System.out.println("no more message");
            }else{
                System.out.println("fail:" + resultCode);
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
        return flag;
    }

    public static void main(String[] args) throws Exception{
        Test1 t = new Test1();
        t.setupTest();

        int c = 0;
        for(int i=0; i<70; i++){
            if(t.consume())
                c++;
        }
        System.out.println("count="+c);
    }


}
