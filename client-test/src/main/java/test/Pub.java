package test;

import com.sitech.crmpd.idmm.client.MessageContext;
import com.sitech.crmpd.idmm.client.api.Message;
import com.sitech.crmpd.idmm.client.api.PropertyOption;
import com.sitech.crmpd.idmm.client.api.PullCode;
import com.sitech.crmpd.idmm.client.api.ResultCode;
import com.sitech.crmpd.idmm.client.pool.PooledMessageContextFactory;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPoolConfig;

import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

public class Pub {
    public static void main(String[] args) throws Exception {
        sub(args);
    }

    public static void sub(String[] args) throws Exception {
        int threadnum = 2;
        String zkAddr = "127.0.0.1:2181/idmm4/broker";
        int timeOut = 10;
        String clientID = "Sub119Opr";
        String topic = "TRecOprCnttDest";
        String description = "ok";

        int poolsize = 2;
        LinkedBlockingQueue<String> msgids = new LinkedBlockingQueue<String>();

        GenericKeyedObjectPoolConfig config = new GenericKeyedObjectPoolConfig();
        config.setMaxTotal(poolsize);
        config.setMaxTotalPerKey(poolsize);
        config.setMaxIdlePerKey(poolsize);
        GenericKeyedObjectPool pool = new GenericKeyedObjectPool(
                new PooledMessageContextFactory(zkAddr, timeOut*1000), config);

        MessageContext context = (MessageContext)pool.borrowObject(clientID);
        PullCode code;
        while(true) {
            String msgid = msgids.poll();
            code = msgid == null ? null : PullCode.COMMIT_AND_NEXT;
            Message message = context.fetch(topic, timeOut,
                    msgid, code, description, false);
            ResultCode resultCode = message.getEnumProperty(PropertyOption.RESULT_CODE, ResultCode.class);
            if (resultCode == ResultCode.OK) //ResultCode.NO_MORE_MESSAGE)
            {
                msgid = message.getId();
                msgids.offer(msgid);
                System.out.println(msgid);
            } else if (resultCode == ResultCode.NO_MORE_MESSAGE) {
                System.out.println("no more message");
//                break;
            } else {
                System.out.println("fail:" + resultCode);
            }
        }

    }

    public static void pub(String[] args) throws Exception {
        int threadnum = 2;
        String zkAddr = "127.0.0.1:2181/idmm4/broker";
        int timeOut = 10;
        String clientID = "Pub101";
        String topic = "TRecOprCntt";
        String messagebody = "messagebodymessagebodymessagebodymessagebodymessagebodymessagebodymessagebodymessagebody";

        Random random = new Random(System.currentTimeMillis());

        GenericKeyedObjectPoolConfig config = new GenericKeyedObjectPoolConfig();
        config.setMaxTotal(threadnum);
        config.setMaxTotalPerKey(threadnum);
        config.setMaxIdlePerKey(threadnum);
        GenericKeyedObjectPool pool = new GenericKeyedObjectPool(
                new PooledMessageContextFactory(zkAddr, timeOut * 1000));

        MessageContext context = (MessageContext)pool.borrowObject(clientID);
        for(int i=0; i<100; i++) {
            Message message = Message.create(messagebody);

            message.setProperty(PropertyOption.GROUP, String.valueOf(Math.abs(random.nextInt())));
//            message.setProperty(PropertyOption.VISIT_PASSWORD, this.password);

            String id = context.send(topic, message);
            System.out.println("messageid: " + id);

            context.commit(new String[]{id});
        }

        context.close();
    }
}
