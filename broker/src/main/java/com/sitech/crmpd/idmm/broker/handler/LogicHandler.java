package com.sitech.crmpd.idmm.broker.handler;

import akka.actor.ActorRef;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import com.sitech.crmpd.idmm.broker.actor.BLEActor;
import com.sitech.crmpd.idmm.broker.actor.PersistActor;
import com.sitech.crmpd.idmm.broker.actor.ReplyActor;
import com.sitech.crmpd.idmm.broker.config.PartsProducer;
import com.sitech.crmpd.idmm.broker.config.TopicConf;
import com.sitech.crmpd.idmm.broker.config.TopicMapping;
import com.sitech.crmpd.idmm.cfg.PartConfig;
import com.sitech.crmpd.idmm.client.api.*;
import com.sitech.crmpd.idmm.netapi.BMessage;
import com.sitech.crmpd.idmm.netapi.BProps;
import com.sitech.crmpd.idmm.netapi.FramePacket;
import com.sitech.crmpd.idmm.netapi.FrameType;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.ehcache.Cache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;


@Sharable
@Configuration
public class LogicHandler extends SimpleChannelInboundHandler<FrameMessage> implements
		ApplicationContextAware {

	/** name="{@link LogicHandler}" */
	private static final Logger log = LoggerFactory.getLogger(LogicHandler.class);
	/**
	 * uuid在mdc中的key
	 */
	public static final String MDC_UUID_KEY = "uuid";
	private Set<SocketAddress> remoteAddresses = Sets.newConcurrentHashSet();
	private ApplicationContext applicationContext;

	@Value("${maxconn.per.broker:10000}")
	private int maxconn_per_broker; //单个broker允许的最大连接数

	@Resource
	private MessageIdGenerator messageIdGenerator;
	private AtomicLong messageIdSequence = new AtomicLong();

	private TopicConf tconf;

    @Resource
    private Cache<String, Message> messageCache;

    private Map<String, List<TopicMapping>> topicMapping; //主题映射配置数据
    private Map<String, List<String>> subscribes;        //目标主题订阅关系配置数据
    private PartsProducer parts;
//	public void setTopicMapping(Map<String, List<TopicMapping>> m) { this.topicMapping = m;}
//	public void setSubscribes(Map<String, List<String>> s) { this.subscribes = s; }
	public void setParts(PartsProducer p) {this.parts = p;}

	public void setTopicConf(TopicConf tc) { this.tconf = tc; }
	
	/**
	 * @see ApplicationContextAware#setApplicationContext(ApplicationContext)
	 */
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	private ActorRef persist;
	private ActorRef ble;
	private ActorRef reply;
	public void setRef(String name, ActorRef ref) {
        if("persist".equals(name))
            persist = ref;
        else if("ble".equals(name))
            ble = ref;
        else if("creply".equals(name))
            reply = ref;
    }

	/**
	 * @see io.netty.channel.ChannelInboundHandlerAdapter#channelActive(io.netty.channel.ChannelHandlerContext)
	 */
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		final SocketAddress s = ctx.channel().remoteAddress();
		if (s != null) {
			remoteAddresses.add(s);
		}

		if(remoteAddresses.size()>maxconn_per_broker){
			log.error("current connects is " + remoteAddresses.size() + ",maxconn_per_broker is [" + maxconn_per_broker +"]", "");
			ctx.close();
		}
	}

	/**
	 * @see io.netty.channel.ChannelInboundHandlerAdapter#channelInactive(io.netty.channel.ChannelHandlerContext)
	 */
	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		final SocketAddress s = ctx.channel().remoteAddress();
		if (s != null) {
			remoteAddresses.remove(s);
		}
	}
	private Random rand = new Random(System.currentTimeMillis());

	/**
	 * @see io.netty.channel.SimpleChannelInboundHandler#channelRead0(io.netty.channel.ChannelHandlerContext,
	 *      Object)
	 */
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FrameMessage msg) throws Exception {

		final MessageType type = msg.getType();
		switch(type) {
			case SEND:
            {
                Message message = msg.getMessage();
                final String src_topic_id = message.getStringProperty(PropertyOption.TOPIC);
                final String client_id = message.getStringProperty(PropertyOption.CLIENT_ID);
                if(!tconf.checkPub(client_id, src_topic_id)){
					Message answer = Message.create();
					answer.setProperty(PropertyOption.RESULT_CODE, ResultCode.BAD_REQUEST);
					answer.setProperty(PropertyOption.CODE_DESCRIPTION, "publish to topic not allowed");
					FrameMessage fr = new FrameMessage(MessageType.ANSWER, answer);
					reply.tell(new ReplyActor.Msg(ctx.channel(), fr), ActorRef.noSender());
					break;
				}

                final SocketAddress address = ctx.channel().remoteAddress();
                final MessageId id = messageIdGenerator.generate((InetSocketAddress) address, message,
                        messageIdSequence.incrementAndGet());
                message.setId(id.getValue());
                messageCache.put(id.getValue(), message);
                persist.tell(new PersistActor.Msg(ctx.channel(), msg), ActorRef.noSender());
            }
				break;
			case SEND_COMMIT:
				sendCommit(ctx, msg);
				break;
			case PULL:
				pull(ctx, msg);
				break;
			case SEND_ROLLBACK:
				break;

			default:
				log.error("invalid request type {}", type);
				break;
		}

	}

	/**
	 * 消费者请求处理, 这里有最复杂的处理流程, 大概率与BLE有多次交互
	 * @param ctx
	 * @param msg
	 */
	private void pull(ChannelHandlerContext ctx, FrameMessage msg) {
		Message m = msg.getMessage();
		PullCode pcode =  m.existProperty(PropertyOption.PULL_CODE) ?
			m.getEnumProperty(PropertyOption.PULL_CODE, PullCode.class) : null;
		String target_topicid = m.getStringProperty(PropertyOption.TARGET_TOPIC);
		String clientid = m.getStringProperty(PropertyOption.CLIENT_ID);

		if(pcode != null) {
			String msgid = m.getId();
			// 从 msgid 中解出 part_num, 暂定在最后一段
			int part_num = -1;
			int p = msgid.lastIndexOf("::");
			if(p > 0){
				part_num = Integer.parseInt(msgid.substring(p+2));
				msgid = msgid.substring(0, p);
			}
			PartConfig part = parts.findPart(target_topicid, clientid, part_num);
			boolean getNext = false;
			BMessage mr = BMessage.c().p(BProps.MESSAGE_ID, msgid)
					.p(BProps.TARGET_TOPIC, target_topicid)
					.p(BProps.CLIENT_ID, clientid)
					.p(BProps.PART_ID, part.getPartId());
			FrameType t = null;
			switch (pcode) {
				case COMMIT_AND_NEXT:
					t = FrameType.BRK_COMMIT;
					getNext = true;
					break;
				case COMMIT:
					t = FrameType.BRK_COMMIT;
					break;

				case ROLLBACK:
					t = FrameType.BRK_ROLLBACK;
					break; //这个暂时忽略, 可以不处理, 等待消息的自动解锁
				case ROLLBACK_AND_NEXT:
					getNext = true;
					t = FrameType.BRK_SKIP;
					break; //调用skip消息 BRK_SKIP
				case ROLLBACK_BUT_RETRY:
					t = FrameType.BRK_RETRY;
					break; // BRK_RETRY
				default:
					log.error("invalid pullcode {}", pcode);
					break;
			}
			FramePacket f = new FramePacket(t, mr);
			BLEActor.Msg bmsg = new BLEActor.Msg(ctx.channel(), f);
			bmsg.bleid = part.getBleid();
			bmsg.wantmsg = getNext;
			bmsg.req_total = 1;
			bmsg.partnum = part_num;
			bmsg.partid = part.getPartId();
			if(getNext && m.existProperty(PropertyOption.PROCESSING_TIME))
				mr.p(BProps.PROCESSING_TIME, m.getLongProperty(PropertyOption.PROCESSING_TIME).intValue());
			ble.tell(bmsg, ActorRef.noSender());
		}else{
			// 直接开始拉取消息, 怎么遍历分区呢? 遍历的控制需要在BLEActor中实现
			BMessage mr = BMessage.c()
					.p(BProps.TARGET_TOPIC, target_topicid)
					.p(BProps.CLIENT_ID, clientid);
			if(m.existProperty(PropertyOption.PROCESSING_TIME))
				mr.p(BProps.PROCESSING_TIME, m.getLongProperty(PropertyOption.PROCESSING_TIME).intValue());
			FramePacket f = new FramePacket(FrameType.BRK_PULL, mr);
			BLEActor.Msg bmsg = new BLEActor.Msg(ctx.channel(), f);
			bmsg.bleid = null; //需要在BLEActor中选择分区后再确定BLE
			bmsg.wantmsg = true;
			bmsg.req_total = 1;
			bmsg.req_seq = 0; // 第一次遍历, 分区数据需要提前推送给BLEActor
			ble.tell(bmsg, ActorRef.noSender());
		}
	}

	private void sendCommit(ChannelHandlerContext ctx, FrameMessage msg) {
		Message mq = msg.getMessage();
		final String[] batchIds = mq.existProperty(PropertyOption.BATCH_MESSAGE_ID) ? mq
				.getArray(PropertyOption.BATCH_MESSAGE_ID, new String[0]) : new String[] { mq
				.getStringProperty(PropertyOption.MESSAGE_ID) };

		List<BLEActor.Msg> req = new LinkedList<>();
		int seq = 0;
		boolean refuse = false;
		OUTERLOOP:
		for(String msgid: batchIds) {
			Message message = messageCache.get(msgid);
			messageCache.remove(msgid); //TODO 应该commit 成功后再删除, 以便允许客户端重试
			String src_topic = message.getStringProperty(PropertyOption.TOPIC);
			String producer_client = message.getStringProperty(PropertyOption.CLIENT_ID);

			String group = message.existProperty(PropertyOption.GROUP) ?
					message.getStringProperty(PropertyOption.GROUP) : String.valueOf(rand.nextDouble());
			int priority = message.existProperty(PropertyOption.PRIORITY) ?
					message.getIntegerProperty(PropertyOption.PRIORITY) : -1;
			long expire = message.existProperty(PropertyOption.EXPIRE_TIME) ?
					message.getLongProperty(PropertyOption.EXPIRE_TIME) : -1;

			// 主题映射 & 消费订阅 & 确定分区
			for(String[] vv: tconf.getMapping(src_topic, mq)) {

				String target_topic = vv[0];
				String consume_client = vv[1];

				PartConfig part = parts.findPart(target_topic, consume_client, group);
				switch(part.getStatus()){
					case READY:
					case JOIN:
					{
						BMessage bm = BMessage.c()
								.p(BProps.MESSAGE_ID, msgid)
								.p(BProps.GROUP, group)
								.p(BProps.PART_ID, part.getPartId());
						if(priority > 0)
							bm.p(BProps.PRIORITY, priority);
						if(expire > 0)
							bm.p(BProps.EXPIRE_TIME, expire);
						FramePacket fp = new FramePacket(FrameType.BRK_SEND_COMMIT, bm);
						BLEActor.Msg am = new BLEActor.Msg(ctx.channel(), fp);
						am.bleid = part.getBleid();
						am.req_seq = seq ++;
						req.add(am);
						log.debug("group {} to part_num {} part_id {} mid: {}",
								group, part.getPartNum(), part.getPartId(), msgid);
					}
					break;
					default:
						log.error("part {} status not allow send-commit", part);
						refuse = true;
						break OUTERLOOP;
				}
			}

		}
		if(refuse){
			// 有分区状态不满足要求, 拒绝
			Message answer = Message.create();
			answer.setProperty(PropertyOption.RESULT_CODE, ResultCode.SERVICE_ADDRESS_NOT_FOUND);
			answer.setProperty(PropertyOption.CODE_DESCRIPTION, "part not allow send-commit");
			FrameMessage fr = new FrameMessage(MessageType.ANSWER, answer);
			reply.tell(new ReplyActor.Msg(ctx.channel(), fr), ActorRef.noSender());
		}else{
			if(req.size() == 0) {
				log.warn("no matched topic found");
				Message answer = Message.create();
				answer.setProperty(PropertyOption.RESULT_CODE, ResultCode.SERVICE_ADDRESS_NOT_FOUND);
				answer.setProperty(PropertyOption.CODE_DESCRIPTION, "no match topic found");
				FrameMessage fr = new FrameMessage(MessageType.ANSWER, answer);
				reply.tell(new ReplyActor.Msg(ctx.channel(), fr), ActorRef.noSender());
			}else{
				int total = req.size();
				for(BLEActor.Msg m: req){
					m.req_total = total;
					ble.tell(m, ActorRef.noSender());
				}

			}
		}
	}

	/**
	 * @see io.netty.channel.ChannelInboundHandlerAdapter#exceptionCaught(io.netty.channel.ChannelHandlerContext,
	 *      Throwable)
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		log.error(Strings.nullToEmpty(cause.getMessage()), cause);
		ctx.close();
	}

	public Map<String, MessageHandler> getMessageHandlers() {
		return applicationContext.getBeansOfType(MessageHandler.class);
	}

	/**
	 * 获取{@link #remoteAddresses}属性的值
	 *
	 * @return {@link #remoteAddresses}属性的值
	 */
	public Set<SocketAddress> getRemoteAddresses() {
		return remoteAddresses;
	}

}
