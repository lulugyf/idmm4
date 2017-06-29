package com.sitech.crmpd.idmm.util;

import java.io.FileWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.*;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicLong;

public final class Util {

	public static String getlocalip()
	{
		String ip = null;
		try {
			ip = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e1) {
			
			e1.printStackTrace();
			return null;
		}// often returns "127.0.0.1"
		if("127.0.0.1".equals(ip) || ip.startsWith("0:0:0:0:0:0:0:")){
//	    System.out.println("Your Host addr: " + addr);  
		    Enumeration<NetworkInterface> n = null;
			try {
				n = NetworkInterface.getNetworkInterfaces();
			} catch (SocketException e1) {
				e1.printStackTrace();
			}
		    for (; n.hasMoreElements();)
		    {
		        NetworkInterface e = n.nextElement();
	
		        Enumeration<InetAddress> a = e.getInetAddresses();
		        for (; a.hasMoreElements();)
		        {
		            InetAddress addr = a.nextElement();
		            //System.out.println("  " + addr.getHostAddress());
		            ip = addr.getHostAddress();
		            if(!"127.0.0.1".equals(ip) && !ip.startsWith("0:0:0:0:0:0:0:")){
		            	break;
		            }
		        }
		    }
		}
		return ip;
	}
	
	private static String localip;
	private static String processid;
	private static AtomicLong seed = new AtomicLong(System.currentTimeMillis());
	static{
		localip = getlocalip();
		processid = String.valueOf(getCurrentProcessID());
	}
	public static String genMsgid()
	{
		return String.format("%s.%s.%s", localip, processid, seed.getAndIncrement());
	}

	public static String genMsgid(String hp)
	{
		return String.format("%s.%s", hp, seed.getAndIncrement());
	}
	
	 public static Integer getCurrentProcessID()
	    {
	        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();
	        String name = runtime.getName();
	        if(name.indexOf("@") > 0)
	        	return Integer.parseInt(name.substring(0, name.indexOf("@")));
	        else
	        	return Integer.parseInt(name);
	    }
	 
	 public static void main(String[] args) throws Exception{
//		 System.out.println(genMsgid());
//		 System.out.println(ManagementFactory.getRuntimeMXBean().getName());

//		 DatagramSocket ds = new DatagramSocket();
//		 DatagramPacket dp = new DatagramPacket("123".getBytes(), 0, 3);
//		 dp.setAddress(InetAddress.getByAddress(new byte[]{10, 1, 1, 106}));
//		 dp.setPort(1056);
//		 ds.send(dp);
//		 System.out.println(ds.getLocalAddress().getHostAddress()); // 0.0.0.0

//		 Socket s = new Socket("172.21.3.187", 22);
//		 System.out.println(s.getLocalAddress().getHostAddress());
//		 s.close();
	 }

	/**
	 * 通过连接一个外部地址， 获取连接上的本地ip地址
	 * @param remoteAddr
	 * @return
	 */
	 public static String getMyAddr(String remoteAddr) {
		String r = remoteAddr;
		if(r.indexOf(',') > 0){
			r = r.substring(0, r.indexOf(','));
		}
		int p = r.indexOf(':');
		String addr = null;
		try{
			Socket s = new Socket(r.substring(0, p), Integer.parseInt(r.substring(p+1)));
			addr = s.getLocalAddress().getHostAddress();
			s.close();
			return addr;
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	 }
	 
	 /**
	  * 保存当前进程id到指定文件
	  * @param fname
	  */
	 public static void savePID(String fname){
		try{
			int pid = getCurrentProcessID();
			FileWriter fw = new FileWriter(fname);
			fw.write(String.format("%d\n", pid));
			fw.close();
		}catch(Exception ex){
			
		}
	}

}
