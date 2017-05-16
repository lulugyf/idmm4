package com.sitech.crmpd.idmm.client;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.bind.DatatypeConverter;

import com.sitech.crmpd.idmm.client.api.Message;
import com.sitech.crmpd.idmm.client.api.MessageType;
import com.sitech.crmpd.idmm.client.api.PropertyOption;
import com.sitech.crmpd.idmm.client.api.PullCode;
import com.sitech.crmpd.idmm.client.exception.OperationException;
import com.sitech.crmpd.idmm.client.pool.Caches;
import com.sitech.crmpd.idmm.client.utils.Encryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.sitech.crmpd.idmm.client.api.ResultCode;

/**
 * @author heihuwudi@gmail.com</br> Created By: 2015年4月3日 上午11:04:55
 */
public class DefaultMessageContext extends MessageContext {
	/** name="{@link DefaultMessageContext}" */
	private static final Logger LOGGER = LoggerFactory
			.getLogger(DefaultMessageContext.class);
	private static final int AUTO_COMPRESS_SIZE = Integer.getInteger(
			"idmm2.auto.compress.size", 4096);
	private static final int NO_COMPRESS_SIZE = Integer.getInteger(
			"idmm2.no.compress.size", 1024);
	private static final int CONTENT_MAX_SIZE = Integer.getInteger(
			"idmm2.content.max.size", 1024 * 1024);
	private Map<String, String> cache = Maps.newHashMap();
	private BasicMessageContext context;
	private final int timeout;
	private AtomicBoolean running = new AtomicBoolean(false);
	private String connectString;
	private String path;
	private String tag;

	/**
	 * @param connectString
	 * @param path
	 * @param timeout
	 * @param clientId
	 * @throws OperationException
	 */
	public DefaultMessageContext(String connectString, String path,
			int timeout, String clientId, String tag) throws OperationException {
		super(clientId);
		this.connectString = connectString;
		this.timeout = timeout;
		this.path = path;
		this.tag = tag;
		context = create(timeout);
	}

	private BasicMessageContext create(int timeout) throws OperationException {
		try {
			final BasicMessageContext context = new BasicMessageContext(
					Caches.getAddress(connectString, path, tag), timeout);
			running.set(true);
			return context;
		} catch (final OperationException e) {
			throw e;
		} catch (final Exception e) {
			throw new OperationException(ResultCode.SERVER_NOT_AVAILABLE, e);
		}
	}

	private String send(boolean isRetry, String topic, Message message)
			throws OperationException {
		Preconditions.checkArgument(topic != null);
		Preconditions.checkArgument(message != null);
		try {
			byte[] content = message.getContent();

			if (!isRetry && content != null) {
				if (content.length > DefaultMessageContext.CONTENT_MAX_SIZE) {
					throw new OperationException(
							ResultCode.MESSAGE_CONTENT_LENGTH_IS_TOO_LANG);
				}
				if (content.length > DefaultMessageContext.AUTO_COMPRESS_SIZE) { // 强制压缩
					DefaultMessageContext.LOGGER.trace("长度大于强制压缩长度[{}]，开启压缩标识",
							DefaultMessageContext.AUTO_COMPRESS_SIZE);
					message.setProperty(PropertyOption.COMPRESS, true);
				}

				if (content.length < DefaultMessageContext.NO_COMPRESS_SIZE) { // 强制不压缩
					DefaultMessageContext.LOGGER.trace("长度小于最小压缩长度[{}]，关闭压缩标识",
							DefaultMessageContext.NO_COMPRESS_SIZE);
					message.setProperty(PropertyOption.COMPRESS, false);
				}

				boolean content_modified = false;
				if (message.existProperty(PropertyOption.COMPRESS)
						&& message.getBooleanPropertyValue(PropertyOption.COMPRESS)) {
					// 先处理压缩
					content = Compress.gzipToBytes(content);
					content_modified = true;
				}
				if(message.existProperty(PropertyOption.ENCRYPT)
						&& message.getBooleanPropertyValue(PropertyOption.ENCRYPT)){
					//然后处理加密
					content = Encryptor.encrypt(content);
					content_modified = true;
				}
				if(content_modified) {
					final Message compressMessage = Message.create(content);
					compressMessage.copyProperties(message);
					message = compressMessage;
				}
			}
			message.setProperty(PropertyOption.TOPIC, topic);
			if (isRetry) {
				final int retryNum = message
						.existProperty(PropertyOption.PRODUCER_RETRY) ? message
						.getIntPropertyValue(PropertyOption.PRODUCER_RETRY) + 1
						: 1;
				message.setProperty(PropertyOption.PRODUCER_RETRY, retryNum);
			}

			final Message answerMessage = context.trade(getClientId(),
					MessageType.SEND, message);
			final String id = answerMessage.getId();
			cache.put(id, message.getPropertiesAsString());
			return id;
		} catch (final OperationException e) {
			throw e;
		} catch (final IOException e) {
			if (isRetry) {
				throw new OperationException(ResultCode.SERVER_NOT_AVAILABLE, e);
			}
			DefaultMessageContext.LOGGER.warn("", e);
			running.compareAndSet(true, false);
			failover();
			return send(true, topic, message);
		}
	}

	/**
	 * @see MessageContext#send(String,
	 *      Message)
	 */
	@Override
	public String send(String topic, Message message) throws OperationException {
		if(tag != null)
			topic = topic + "-" + tag;
		return send(false, topic, message);
	}

	/**
	 * @see MessageContext#commit(String[])
	 */
	@Override
	public void commit(String... id) throws OperationException {
		commit(false, MessageType.SEND_COMMIT, id);
	}

	/**
	 * @see MessageContext#rollback(String[])
	 */
	@Override
	public void rollback(String... id) throws OperationException {
		Message r = commit(false, MessageType.SEND_ROLLBACK, id);
		System.out.println(r.toString());
	}

	private Message commit(boolean isRetry, MessageType type, String... id)
			throws OperationException {
		Preconditions.checkArgument(id != null);
		Preconditions.checkArgument(id.length > 0);
		try {
			final Message message = Message.create();
			if (isRetry) {
				final int retryNum = message
						.existProperty(PropertyOption.PRODUCER_RETRY) ? message
						.getIntPropertyValue(PropertyOption.PRODUCER_RETRY) + 1
						: 1;
				message.setProperty(PropertyOption.PRODUCER_RETRY, retryNum);
			}
			message.setProperty(PropertyOption.BATCH_MESSAGE_ID, id);
			return context.trade(getClientId(), type, message);
		} catch (final OperationException e) {
			throw e;
		} catch (final IOException e) {
			if (isRetry) {
				throw new OperationException(ResultCode.SERVER_NOT_AVAILABLE, e);
			}
			DefaultMessageContext.LOGGER.warn("", e);
			running.compareAndSet(true, false);
			failover();
			return commit(true, type, id);
		}
	}

	/**
	 * @see MessageContext#fetch(String,
	 *      long, String, PullCode,
	 *      String, boolean)
	 */
	@Override
	public Message fetch(String topic, long processingTime,
			String lastMessageId, PullCode lastMessageStatus,
			String description, boolean noMoreMessageBlocking)
			throws OperationException {
		Preconditions.checkArgument(topic != null);
		if(tag != null)
			topic = topic + "-" + tag;
		try {
			final Message message = Message.create();
			message.setProperty(PropertyOption.TARGET_TOPIC, topic);
			message.setProperty(PropertyOption.PROCESSING_TIME, processingTime);
			if (!Strings.isNullOrEmpty(lastMessageId)) {
				if (lastMessageStatus == null) {
					throw new OperationException(
							ResultCode.REQUIRED_PARAMETER_MISSING,
							PropertyOption.RESULT_CODE);
				}
				message.setId(lastMessageId);
				message.setProperty(PropertyOption.PULL_CODE, lastMessageStatus);
				if (!Strings.isNullOrEmpty(description)) {
					message.setProperty(PropertyOption.CODE_DESCRIPTION,
							description);
				}
			}
			Message answerMessage = context.trade(getClientId(),
					MessageType.PULL, message);
			final ResultCode resultCode = ResultCode.valueOf(answerMessage
					.getProperty(PropertyOption.RESULT_CODE));
			if (resultCode == ResultCode.NO_MORE_MESSAGE
					&& noMoreMessageBlocking) {
				try {
					final long noMoreMessageSleep = getNoMoreMessageSleep();
					TimeUnit.MILLISECONDS
							.sleep(noMoreMessageSleep > 1000 ? noMoreMessageSleep
									: 1000);
				} catch (final InterruptedException e) {
				}
				return fetch(topic, processingTime, null, null, null);
			}

			if (answerMessage.existProperty(PropertyOption.REST_COMPRESS)
					&& answerMessage
							.getBooleanPropertyValue(PropertyOption.REST_COMPRESS)) {
				final Message uncompressMessage = Message.create(Compress
						.gunzipFromBytes(DatatypeConverter
								.parseBase64Binary(answerMessage
										.getContentAsString())));
				uncompressMessage.copyProperties(answerMessage);
				answerMessage = uncompressMessage;
			}

			byte[] content = answerMessage.getContent();
			boolean content_modified = false;
			//先处理解密
			if(answerMessage.existProperty(PropertyOption.ENCRYPT)
					&& answerMessage
					.getBooleanPropertyValue(PropertyOption.ENCRYPT)){
				content = Encryptor.decrypt(content);
				content_modified = true;
			}

			// 然后处理解压
			if (answerMessage.existProperty(PropertyOption.COMPRESS)
					&& answerMessage
							.getBooleanPropertyValue(PropertyOption.COMPRESS)) {
				content = Compress.gunzipFromBytes(content);
				content_modified = true;
			}
			if(content_modified){
				final Message uncompressMessage = Message.create(content);
				uncompressMessage.copyProperties(answerMessage);
				answerMessage = uncompressMessage;
			}
			return answerMessage;
		} catch (final OperationException e) {
			throw e;
		} catch (final IOException e) {
			DefaultMessageContext.LOGGER.warn("", e);
			running.compareAndSet(true, false);
			failover();
			return fetch(topic, processingTime, lastMessageId,
					lastMessageStatus, description, noMoreMessageBlocking);
		}
	}

	/**
	 * @see java.io.Closeable#close()
	 */
	@Override
	public void close() throws IOException {
		if (cache != null) {
			cache.clear();
		}
		if (context != null) {
			context.close();
		}
	}

	private void failover() throws OperationException {
		if (!running.get()) {
			if (context != null) {
				context.close();
			}
			context = null;
			while (context == null) {
				try {
					context = create(timeout);
				} catch (final Exception e) {}

				try {
					TimeUnit.SECONDS.sleep(1);
				} catch (final InterruptedException e) {
				}
			}
		}
	}

	/**
	 * @see MessageContext#delete(String[])
	 */
	@Override
	public void delete(String... id) throws OperationException {
		commit(false, MessageType.DELETE, id);
	}

	/**
	 * @see MessageContext#isClosed()
	 */
	@Override
	public boolean isClosed() {
		return !context.isOK();
	}
}
