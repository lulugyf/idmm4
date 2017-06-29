package com.sitech.crmpd.idmm.broker.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import com.sitech.crmpd.idmm.broker.config.PartsConsumer;
import com.sitech.crmpd.idmm.client.api.*;
import com.sitech.crmpd.idmm.netapi.*;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by gyf on 5/1/2017.
 */
public class BLEActor extends AbstractActor {
//    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    private static final Logger log = LoggerFactory.getLogger(BLEActor.class);
    private ActorRef reply;
    private ActorRef store;
    private ActorRef brk;


    private int seq_seed = 1;
    private Map<Channel, Integer> jobs = new HashMap<>(); // 用于保存需要多请求然后合并应答的情况
    private Map<Integer, Msg> seqs = new HashMap<>(); // 报文序列号 对应  客户端channel

    private Map<String, ActorRef> bles = new HashMap<>(); // bleid 对应 actor
    private PartsConsumer cp;

    private int max_pull_time = 5; // 客户端一次pull请求, 最多遍历分区的次数

    // 向BLE发送的请求包
    public static class Msg {
        Channel channel;
        FramePacket fm;
        long create_time;

        //TODO 对于SEND_COMMIT多包请求的异常情况, 还未完全处理
        public String bleid;
        public int req_total; //总包数
        public int req_seq;   //本包序号
        public boolean wantmsg; //是否需要拉取消息
        protected int partnum; //用于记录当前访问的分区序号

        public Msg(Channel c, FramePacket f) {
            channel = c;fm = f; create_time=System.currentTimeMillis();
        }
    }

    // 从BLE应答的消息
    public static class RMsg {
        Channel channel;
        FramePacket fm;
        long create_time;
        public RMsg(Channel c, FramePacket f) {
            channel = c;fm = f; create_time=System.currentTimeMillis();
        }
    }


    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Msg.class, s -> {
                    try {
                        onReceive(s);
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                })
                .match(RefMsg.class, s -> {
                    onReceive(s);
                })
                .match(RMsg.class, s -> {
                    try {
                        onReceive(s);
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }
                })
                .match(PartsConsumer.class, s -> {
                    onReceive(s);
                })
                .matchAny(o -> log.info("received unknown message:{}", o))
                .build();
    }

    private void onReceive(PartsConsumer cp) {
        if(this.cp != null)
            cp.copyStatus(this.cp);
        log.info("reset consume partitions");
        this.cp = cp;
    }

    private void onReceive(RefMsg s){
        if("brk".equals(s.name))
            brk = s.ref;
        else if("persist".equals(s.name)){
            store = s.ref;
        }else if("reply".equals(s.name)){
            reply = s.ref;
        }else if("ble".equals(s.name)) {
            bles.put(s.str, s.ref);
        }
    }

    /**
     * 处理从客户端送来的请求
     * @param s
     */
    private void onReceive(Msg s) {
        int seq = seq_seed ++;
        switch(s.fm.getType()){
            case BRK_PULL:
            {
                // BRK_PULL的时候不预先给bleid
                BMessage bm = s.fm.getMessage();
                String topic = bm.p(BProps.TARGET_TOPIC);
                String client = bm.p(BProps.CLIENT_ID);
                PartsConsumer.SPart p = cp.nextPart(topic, client);
                if(p == null){
                    log.error("can not found consume partition {} {}", topic, client);
                    Message answer = Message.create();
                    answer.setProperty(PropertyOption.RESULT_CODE, ResultCode.SERVICE_ADDRESS_NOT_FOUND);
                    answer.setProperty(PropertyOption.CODE_DESCRIPTION, "no matched ble found");
                    FrameMessage fr = new FrameMessage(MessageType.ANSWER, answer);
                    reply.tell(new ReplyActor.Msg(s.channel, fr), ActorRef.noSender());
                    return;
                }
                bm.p(BProps.PART_ID, p.id);
                s.bleid = p.bleid;
                s.partnum = p.num;
            } // 这里没有break, 发送请求到ble 是走的下面的代码

            case BRK_SEND_COMMIT:
            case BRK_ROLLBACK:
            case BRK_SKIP:
            case BRK_RETRY:
            case BRK_COMMIT:
            {
                if(!bles.containsKey(s.bleid)) {
                    log.error("ble {}  not found", s.bleid);
                    Message answer = Message.create();
                    answer.setProperty(PropertyOption.RESULT_CODE, ResultCode.SERVICE_ADDRESS_NOT_FOUND);
                    answer.setProperty(PropertyOption.CODE_DESCRIPTION, "no matched ble found");
                    FrameMessage fr = new FrameMessage(MessageType.ANSWER, answer);
                    reply.tell(new ReplyActor.Msg(s.channel, fr), ActorRef.noSender());
                    // TODO 如果是多包, 可能有部分成功, 以及多个失败应答的可能, 需要额外处理
                    return;
                }

                s.fm.setSeq(seq);
                seqs.put(seq, s);
                log.debug("send packet seq {}", seq);

                if(s.req_total > 1 && s.req_seq == 0) {
                    // 多包请求
                    jobs.put(s.channel, s.req_total);
                }

                bles.get(s.bleid).tell(new BLEWriterActor.Msg(s.channel, s.fm), getSelf());
            }
                break;
            default:
                log.error("invalid request type {}", s.fm.getType());
                break;
        }
    }

    /**
     * 处理从BLE应答的消息
     * @param rMsg
     */
    private void onReceive(RMsg rMsg) {
        int seq = rMsg.fm.getSeq();
        log.debug("receive packet seq {}", seq);
        Msg s = seqs.remove(seq);
        if(s == null) {
            log.error("seq not found {}", seq);
            return;
        }
        Channel ch = s.channel;

        FramePacket fm = rMsg.fm;
        BMessage bm = fm.getMessage();
        RetCode rcode = bm.getEnumProperty(BProps.RESULT_CODE, RetCode.class);
        if( rcode != RetCode.OK){
            String desc = "[null]";
            if(bm.existProperty(BProps.RESULT_DESC))
                desc = bm.p(BProps.RESULT_DESC);
            log.error("ble reply failed, {} {}", rcode, desc);
            // TODO 多包请求, 部分成功怎么处理?  原来的broker就没有处理， 呵呵
        }
        int i = jobs.getOrDefault(ch, -1);
        if(i >= 2) {
            jobs.put(ch, i-1);
            return;
        }
        if(i == 1) jobs.remove(ch);

        //准备应答, 根据报文类型处理
        switch(rMsg.fm.getType()){
            case BRK_SEND_COMMIT_ACK:
            {
                Message answer = Message.create();
                if(rcode == RetCode.OK){
                    answer.setProperty(PropertyOption.RESULT_CODE, ResultCode.OK);
                }else{
                    answer.setProperty(PropertyOption.RESULT_CODE, ResultCode.INTERNAL_SERVER_ERROR);
                    answer.setProperty(PropertyOption.CODE_DESCRIPTION,
                            rcode + "~" + bm.p(BProps.RESULT_DESC));
                }
                FrameMessage fr = new FrameMessage(MessageType.ANSWER, answer);
                reply.tell(new ReplyActor.Msg(ch, fr), ActorRef.noSender());
            }
                break;
            case BRK_COMMIT_ACK:
            case BRK_ROLLBACK_ACK:
            case BRK_SKIP_ACK:
            case BRK_RETRY_ACK: {
                if(rcode != RetCode.OK){
                    Message answer = Message.create();
                    answer.setProperty(PropertyOption.RESULT_CODE, ResultCode.INTERNAL_SERVER_ERROR);
                    answer.setProperty(PropertyOption.CODE_DESCRIPTION,
                            rcode + "~" + bm.p(BProps.RESULT_DESC));
                    FrameMessage fr = new FrameMessage(MessageType.ANSWER, answer);
                    reply.tell(new ReplyActor.Msg(ch, fr), ActorRef.noSender());
                }else {
                    if (s.wantmsg) {
                        // 需要进一步PULL消息, 另外发起PULL请求
                        BMessage bml = s.fm.getMessage(); // 上次请求的报文
                        String target_topicid = bml.p(BProps.TARGET_TOPIC);
                        String clientid = bml.p(BProps.CLIENT_ID);
                        BMessage mr = BMessage.c()
                                .p(BProps.TARGET_TOPIC, target_topicid)
                                .p(BProps.CLIENT_ID, clientid)
                                .p(BProps.PROCESSING_TIME, bml.p(BProps.PROCESSING_TIME));
                        FramePacket f = new FramePacket(FrameType.BRK_PULL, mr);
                        BLEActor.Msg bmsg = new BLEActor.Msg(s.channel, f);
                        bmsg.bleid = null; //需要在BLEActor中选择分区后再确定BLE
                        bmsg.wantmsg = true;
                        bmsg.req_total = 1;
                        bmsg.req_seq = 0; // 第一次遍历, 分区数据需要提前推送给BLEActor
                        getSelf().tell(bmsg, ActorRef.noSender());
                    } else {
                        // 成功应答
                        Message answer = Message.create();
                        answer.setProperty(PropertyOption.RESULT_CODE, ResultCode.OK);
                        FrameMessage fr = new FrameMessage(MessageType.ANSWER, answer);
                        reply.tell(new ReplyActor.Msg(ch, fr), ActorRef.noSender());
                    }
                }
            }
                break;
            case BRK_PULL_ACK:{
                if(rcode == RetCode.OK){
                    //成功pull到消息, 返回客户端, 需要在message_id后拼接 part_num
                    String msgid = bm.p(BProps.MESSAGE_ID);
                    msgid = msgid + "::" + s.partnum;
                    Message answer = Message.create();
                    answer.setProperty(PropertyOption.RESULT_CODE, ResultCode.OK);
                    answer.setId(msgid);
                    answer.setProperty(PropertyOption.CONSUMER_RETRY, bm.p(BProps.CONSUMER_RETRY));
                    FrameMessage fr = new FrameMessage(MessageType.ANSWER, answer);
                    // DONE redirect pull_reply to persistent, and find message body, then reply
                    // DONE get status of queue and save to PartsConsumer
                    BMessage bm1 = s.fm.getMessage();
                    int[] state = bm.p(BProps.QSTATE);
                    cp.setStatus(bm1.p(BProps.TARGET_TOPIC), bm1.p(BProps.CLIENT_ID), s.partnum,
                            state[0], state[1], state[2]);
                    store.tell(new PersistActor.Msg(ch, fr), ActorRef.noSender());
                }else if(rcode == RetCode.NO_MORE_MESSAGE) {
                    BMessage bm1 = s.fm.getMessage();
                    cp.setStatus(bm1.p(BProps.TARGET_TOPIC), bm1.p(BProps.CLIENT_ID), s.partnum,
                            0, 0, 0);
                    // 无消息, 判断遍历次数, 最多遍历5次
                    if(s.req_seq >= max_pull_time){
                        // 应答到客户端无消息
                        Message answer = Message.create();
                        answer.setProperty(PropertyOption.RESULT_CODE, ResultCode.NO_MORE_MESSAGE);
                        FrameMessage fr = new FrameMessage(MessageType.ANSWER, answer);
                        reply.tell(new ReplyActor.Msg(ch, fr), ActorRef.noSender());
                    }else{
                        //累加计数器, 继续遍历分区
                        s.req_seq += 1;
                        getSelf().tell(s, ActorRef.noSender());
                    }
                }else{
                    // 其它异常, 向客户端返回异常
                    Message answer = Message.create();
                    answer.setProperty(PropertyOption.RESULT_CODE, ResultCode.INTERNAL_SERVER_ERROR);
                    answer.setProperty(PropertyOption.CODE_DESCRIPTION,
                            rcode + "~" + bm.p(BProps.RESULT_DESC));
                    FrameMessage fr = new FrameMessage(MessageType.ANSWER, answer);
                    reply.tell(new ReplyActor.Msg(ch, fr), ActorRef.noSender());
                }
            }
                break;
            default:
                log.error("unknown packet type {}", rMsg.fm.getType());
                break;
        }
    }


}
