package com.sitech.crmpd.idmm.ble.util;

import java.io.FileWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
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
	 
	 public static void main(String[] args){
		 System.out.println(genMsgid());
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
