package com.sitech.crmpd.idmm.client.pool;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCache.StartMode;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.RetryNTimes;

import com.google.common.net.HostAndPort;

/**
 * @author heihuwudi@gmail.com</br> Created By: 2015年7月8日 下午3:06:38
 */
public class Caches {
	private static CuratorFramework client;
	private static PathChildrenCache pathChildrenCache;
	private static final List<InetSocketAddress> CACHE = Collections
			.synchronizedList(new ArrayList<InetSocketAddress>());
	private static final Random RANDOM = new Random();
	private static final int sessionTimeoutMs = Integer.getInteger(
			"idmm2.client.zookeeper.sessionTimeoutMs", 5000);
	private static final int connectionTimeoutMs = Integer.getInteger(
			"idmm2.client.zookeeper.connectionTimeoutMs", 5000);

	/**
	 * @param connectString
	 * @return 可用地址
	 * @throws Exception
	 */
	public static synchronized InetSocketAddress getAddress(String connectString, String path, final String tag) throws Exception {
		if (client == null) {
			client = CuratorFrameworkFactory.newClient(connectString, sessionTimeoutMs,
					connectionTimeoutMs, new RetryNTimes(Integer.MAX_VALUE, 5000));
			client.start();
			client.blockUntilConnected();
//			System.out.println("====path:"+path);
			pathChildrenCache = new PathChildrenCache(client, path, false);
			pathChildrenCache.getListenable().addListener(new PathChildrenCacheListener() {

				@Override
				public void childEvent(CuratorFramework client, PathChildrenCacheEvent event)
						throws Exception {
					final Type type = event.getType();
					if (type == Type.CHILD_ADDED || type == Type.CHILD_REMOVED) {
						// 缓存数据发生变化
						update(tag);
					}
				}
			});
			pathChildrenCache.start(StartMode.BUILD_INITIAL_CACHE);
		}
		while (CACHE.isEmpty()) {
			update(tag);
			TimeUnit.SECONDS.sleep(1);
		}
		final InetSocketAddress address = CACHE.remove(0);
		CACHE.add(address);
		return address;
	}

	private static void update(String tag) {
		CACHE.clear();
		final Iterator<ChildData> iterator = pathChildrenCache.getCurrentData().iterator();
		while (iterator.hasNext()) {
			final ChildData childData = iterator.next();
			final String absolute = childData.getPath();
			String addrstr = absolute.substring(absolute.lastIndexOf('/') + 1);
			int p = addrstr.indexOf('-');
			String addr_tag = null;
			if(p > 0){
				addr_tag = addrstr.substring(p+1);
				addrstr = addrstr.substring(0, p);
			}
			if(tag != null && !tag.equals(addr_tag))
				continue; //如果提供了标签 且 与地址中的标签不一致， 则忽略。 双机房部署
			final HostAndPort hostAndPort = HostAndPort.fromString(addrstr);
			CACHE.add(new InetSocketAddress(hostAndPort.getHost(), hostAndPort.getPort()));
		}

		if(CACHE.size() == 0 && tag != null) {
			// 如果按标签取本地的broker地址完全取不到, 则重新获取全部的地址 2017-4-20
			final Iterator<ChildData> iter = pathChildrenCache.getCurrentData().iterator();
			while (iter.hasNext()) {
				final ChildData childData = iter.next();
				final String absolute = childData.getPath();
				String addrstr = absolute.substring(absolute.lastIndexOf('/') + 1);
				int p = addrstr.indexOf('-');
				if (p > 0)
					addrstr = addrstr.substring(0, p);
				final HostAndPort hostAndPort = HostAndPort.fromString(addrstr);
				CACHE.add(new InetSocketAddress(hostAndPort.getHost(), hostAndPort.getPort()));

			}
		}
		Collections.shuffle(CACHE, RANDOM);
	}
}
