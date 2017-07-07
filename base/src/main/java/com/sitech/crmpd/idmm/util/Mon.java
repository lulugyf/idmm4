package com.sitech.crmpd.idmm.util;

import com.google.common.base.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Created by guanyf on 7/7/2017.
 */
@Configuration
public class Mon extends Thread{

    @Value("${cpu_out_file:}")
    private String outpath;

    @Value("${cpu_interval_secs:20}")
    private int cpu_interval;

    private boolean running = true;
    private String nodeid;

    public void setNodeid(String id) {
        this.nodeid = id;
    }

    static class CpuTime implements Comparable<CpuTime>{
        final String threadName;
        final long threadId;
        final long cpuTime;

        CpuTime(Thread th, ThreadMXBean tmxb) {
            threadId = th.getId();
            cpuTime = tmxb.getThreadCpuTime(threadId);
            threadName = th.getName();
        }
        @Override
        public int compareTo(CpuTime o) {
            return (int)(cpuTime-o.cpuTime);
        }
    }

    private void cpuMon(PrintWriter pout) throws Exception{
        ThreadMXBean tmxb = ManagementFactory.getThreadMXBean();

        Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        ArrayList<CpuTime> t = new ArrayList<>(threadSet.size());
        for(Thread th: threadSet) {
            t.add(new CpuTime(th, tmxb));
        }
        Collections.sort(t);
        for(CpuTime ct: t){
            pout.println(ct.cpuTime + " " + ct.threadName );
        }
    }

    @Override
    public void run() {
        while(running){
            try {
                TimeUnit.SECONDS.sleep(cpu_interval);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try{
                PrintWriter pout = new PrintWriter(new FileWriter(outpath+"-"+nodeid, true));
                pout.println("\n\n===================");
                pout.println(new Date());
                pout.println("===================");
                cpuMon(pout);
                pout.close();
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    @PostConstruct
    private void init() {
        if(!Strings.isNullOrEmpty(outpath)) {
            this.start();
        }
    }

}
