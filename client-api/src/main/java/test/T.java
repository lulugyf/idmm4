package test;

import com.sitech.crmpd.idmm.client.DefaultMessageContext;
import com.sitech.crmpd.idmm.client.MessageContext;
import com.sitech.crmpd.idmm.client.api.Message;
import com.sitech.crmpd.idmm.client.api.PropertyOption;
import com.sitech.crmpd.idmm.client.pool.PooledMessageContextFactory;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;

/**
 * Created by guanyf on 5/16/2017.
 */
public class T {
//    public static void main(String[] args) throws Exception {
    private static void usage() {
        System.out.println("<zk_addr> <pub_client_id> <src_topic_id> [count(def 100)]");
        System.exit(0);
    }
        /**
         *
         * @param args
         *           测试生产者
         */
    public static void main(String[] args) {
//        if(args.length < 3)
//            usage();
        String zk_addr = "127.0.0.1:2181/idmm4/broker"; //args[0];
        String pub_client_id  = "client"; //args[1];
        String src_topic = "topic"; //args[2];
        int count = 3;
        if(args.length > 3)
            count = Integer.parseInt(args[3]);
        System.out.println("-----------------------begin....");
        final KeyedObjectPool<String, MessageContext> pool = new GenericKeyedObjectPool<String, MessageContext>(
                new PooledMessageContextFactory(zk_addr, //"127.0.0.1:2181/idmm2/broker",
                        60000));
        System.out.println("-----------------------start");
        long t1, t2;
        try {
            final PropertyOption<String> MESSAGE_PART = PropertyOption.valueOf("msg_part");
            final MessageContext context = pool.borrowObject(pub_client_id);
            for (int i = 0; i < count; i++) {
                final Message message = Message.create("I am here waiting for you: "+i);
                message.setProperty(PropertyOption.valueOf("msg_part"),  "12");
                message.setProperty(PropertyOption.GROUP,  "123");
                //message.setProperty(PropertyOption.EXPIRE_TIME, System.currentTimeMillis()+60*1000);
                //message.setProperty(PropertyOption.EFFECTIVE_TIME, System.currentTimeMillis()+60*1000);
//				message.setProperty(PropertyOption.REPLY_TO, "notice_1");

                t1 = System.currentTimeMillis();
                final String id = context.send(src_topic, message);
                context.commit(id);
                t2 = System.currentTimeMillis();
                System.out.println("id=" + id + " -- "+(t2-t1));
                //TimeUnit.SECONDS.sleep(1);
                //context.commit(id);
            }
            pool.returnObject(pub_client_id, context);
            // context.close();
        } catch (final Exception e) {
            e.printStackTrace();
        }
        pool.close();
        System.out.println("closed");
    }
}
