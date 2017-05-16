package com.sitech.crmpd.idmm.broker.actor;

import akka.actor.AbstractActor;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import com.sitech.crmpd.idmm.cfg.PartConfig;
import com.sitech.crmpd.idmm.cfg.PartitionStatus;
import com.sitech.crmpd.idmm.client.api.*;
import com.sitech.crmpd.idmm.netapi.*;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
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

    private Map<String, ActorRef> bles = new HashMap<>();

    // 向BLE发送的请求包
    public static class Msg {
        Channel channel;
        FramePacket fm;
        long create_time;

        //TODO 对于多包请求的情况, 还未处理
        public String bleid;
        public int req_total; //总包数
        public int req_seq;   //本包序号
        public boolean wantmsg; //是否需要拉取消息

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
                .matchAny(o -> log.info("received unknown message:{}", o))
                .build();
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

    private void onReceive(Msg s) {
        int seq = seq_seed ++;
        if(!bles.containsKey(s.bleid)) {
            log.error("ble {}  not found");
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

    private void onReceive(RMsg s) {
        int seq = s.fm.getSeq();
        log.debug("receive packet seq {}", seq);
        Msg m = seqs.remove(seq);
        if(m == null) {
            log.error("seq not found {}", seq);
            return;
        }
        Channel ch = m.channel;

        FramePacket fm = s.fm;
        BMessage bm = fm.getMessage();
        RetCode rcode = bm.getEnumProperty(BProps.RESULT_CODE, RetCode.class);
        if( rcode != RetCode.OK){
            log.error("ble reply failed, {} {}", rcode, bm.p(BProps.RESULT_DESC));
            // TODO 多包请求, 部分成功怎么处理?
        }
        int i = jobs.getOrDefault(ch, -1);
        if(i >= 2) {
            jobs.put(ch, i-1);
            return;
        }
        if(i == 1) jobs.remove(ch);

        //准备应答, 根据报文类型处理
        switch(s.fm.getType()){
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
            default:
                log.error("unknown packet type {}", s.fm.getType());
                break;
        }
    }


}
