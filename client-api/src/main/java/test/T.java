package test;

import com.sitech.crmpd.idmm.client.DefaultMessageContext;
import com.sitech.crmpd.idmm.client.MessageContext;
import com.sitech.crmpd.idmm.client.api.Message;
import com.sitech.crmpd.idmm.client.api.PropertyOption;
import com.sitech.crmpd.idmm.client.api.PullCode;
import com.sitech.crmpd.idmm.client.api.ResultCode;
import com.sitech.crmpd.idmm.client.pool.PooledMessageContextFactory;
import org.apache.commons.cli.*;
import org.apache.commons.pool2.KeyedObjectPool;
import org.apache.commons.pool2.impl.GenericKeyedObjectPool;

import java.util.Random;

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
    public static void main(String[] args) throws Exception{
        Options options = new Options();
        options.addOption("s", "source", true, "source topic id");
        options.addOption("p", "publisher", true, "publisher id");
        options.addOption("t", "targer", true, "target topic id");
        options.addOption("c", "consumer", true, "consumer id");
        options.addOption("n", "count", true, "send message count");
        options.addOption("z", "zkaddr", true, "zookeeper address");

        options.addOption("h", "help", false, "print this message");

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = parser.parse( options, args);
        if(cmd.hasOption('h')) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("idmm test client", options);
            return;
        }

        String zk_addr = cmd.getOptionValue('z', "127.0.0.1:2181/idmm4/broker");
        String pub_client_id  = cmd.getOptionValue('p', "Pub101");
        String src_topic = cmd.getOptionValue('s',"TRecOprCntt");

        String taret_topic = cmd.getOptionValue('t',"TRecOprCnttDest");
        String sub_client_id = cmd.getOptionValue('s',"Sub119Opr");
        int count = Integer.parseInt(cmd.getOptionValue('n', "10"));

        System.out.println("-----------------------begin....");
        final KeyedObjectPool<String, MessageContext> pool = new GenericKeyedObjectPool<String, MessageContext>(
                new PooledMessageContextFactory(zk_addr, //"127.0.0.1:2181/idmm2/broker",
                        60000));
        System.out.println("-----------------------start");

        long t1, t2;
        try {
//            final PropertyOption<String> MESSAGE_PART = PropertyOption.valueOf("msg_part");
            MessageContext context = pool.borrowObject(pub_client_id);
            produce(context, src_topic, count);
            pool.returnObject(pub_client_id, context);

            context = pool.borrowObject(sub_client_id);
            consumAll(context, taret_topic);
            pool.returnObject(sub_client_id, context);

            // context.close();
        } catch (final Exception e) {
            e.printStackTrace();
        }
        pool.close();
        System.out.printf("closed, produce=%d\n", count);
    }

    private static void produce(MessageContext context, String src_topic, int count)
        throws Exception
    {
//        long t1, t2;
        Random r = new Random(System.currentTimeMillis());
        for (int i = 0; i < count; i++) {
            final Message message = Message.create("I am here waiting for you: "+i);
            message.setProperty(PropertyOption.valueOf("msg_part"),  "12");
            message.setProperty(PropertyOption.GROUP,  "123" + r.nextInt());
            //message.setProperty(PropertyOption.EXPIRE_TIME, System.currentTimeMillis()+60*1000);
            //message.setProperty(PropertyOption.EFFECTIVE_TIME, System.currentTimeMillis()+60*1000);
//				message.setProperty(PropertyOption.REPLY_TO, "notice_1");

//            t1 = System.currentTimeMillis();
            final String id = context.send(src_topic, message);
            context.commit(id);
//            t2 = System.currentTimeMillis();
            System.out.println("P: id=" + id);
            //TimeUnit.SECONDS.sleep(1);
            //context.commit(id);
        }
    }

    private static void consumAll(MessageContext context, String target_topic)
            throws Exception{
        PullCode code = null;
        String lastMsgId = null;
        String description = "success";
        int c = 0;
        int no_c = 0;
        while (true) {
            Message msg3 = context.fetch(target_topic, 60, lastMsgId, code,
                    description, false);
            final ResultCode resultCode = msg3.getEnumProperty(PropertyOption.RESULT_CODE, ResultCode.class);
            if (resultCode == ResultCode.NO_MORE_MESSAGE) {
                System.out.println("no more message break. count:"+c);
                Thread.sleep(1000L);
                no_c += 1;
                if(no_c > 5)
                    break; //无消息5次则退出循环

                lastMsgId = null;
                code = null;
                continue;
            }
            lastMsgId = msg3.getId();
            if(lastMsgId == null){
                System.out.println("messageid is null, break");
                break;
            }
            System.out.println("messageid: "+lastMsgId);
            no_c = 0;
            c++;

            code = PullCode.COMMIT_AND_NEXT;

        }
        System.out.printf("consume total: %d\n", c);
    }
}
