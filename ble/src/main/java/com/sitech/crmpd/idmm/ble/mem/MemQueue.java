package com.sitech.crmpd.idmm.ble.mem;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Created by guanyf on 2016/4/26.
 *
 * 从 PrioQueue.java 中分拆出来， 只处理内存数据结构， 而 PrioQueue 则负责与netty db线程之间的交互

 * 排序规则说明：
 * 1. 消息必须要有优先级, 基本原则是：先进先出； 优先级大的先出； 同一groupid下的消息按最大优先级使用同一优先级
 * 2. 可选的groupid， 如果有groupid， 则按同一groupid下最大优先级处理， 并按进入队列的时间顺序先进先出，同一groupid下的消息，只能有一个在途
 * 3. 队列划分依据为 clientid + topicid, 一个队列只给一个clientid消费
 * 4. clientid对应配置上最大并行数量nmax， 当未确认消息的数量超过nmax时， 则不再提供新的消息
 * 5. 消费确认后才移除消息， 超时未确认则恢复为未发送状态， 重发次数+1

 * *** 线程不安全
 */
final public class MemQueue {
    private static final Logger log = LoggerFactory.getLogger(MemQueue.class);

    protected String dst_cli_id;
    protected String dst_topic_id;
    private int maxretry = 10;
    private int errcount = 0;

    private int min_timeout = 6000;
    private int max_timeout = 60000;
    // 针对 groupid 的锁定时间
    private int lockTimeout = 60 * 1000;
    private int nMaxOnway; //最大并发数


    final private ArrayList<MsgIndex> temp_list = new ArrayList<MsgIndex>();

    //保存在途消息的groupid锁定， 确保同一groupid下消息的消费时间顺序, key为groupid
    final private HashMap<String, Long> grouplocks = new HashMap<String, Long>();

    //按groupid 存放的消息体, 没有groupid的消息， 则使用[nogroup]+prio 作为groupid
    final private HashMap<String, LinkedList<MsgIndex>> messages = new HashMap<String, LinkedList<MsgIndex>>();

    // 按msgid 保存的消息索引， 可以直接通过msgid找到对应的消息， 用于消费确认、取消、删除消息时使用
    final private HashMap<String, MsgIndex> messageids = new HashMap<String, MsgIndex>();

    final private HashMap<String, MsgIndex> lockingMessages = new HashMap<String, MsgIndex>();

    public static class Conf{
        public int maxRetry;    //最大消费重试次数
        public int defLockTimeMs; //默认锁定时间
        public int minTimeoutMs;
        public int maxTimeoutMs;
        public int maxOnway;    //最大在途数
    }
    /////////////////////////////////////////////////

    public MemQueue(String dst_cli_id, String dst_topic_id, int nConcurrents){
        this.dst_cli_id = dst_cli_id;
        this.dst_topic_id = dst_topic_id;

        if(nConcurrents > 0)
            this.nMaxOnway = nConcurrents;
        else
            nMaxOnway = 10;
    }
    protected MemQueue() {}

    public void setConf(Conf c) {
        if(c.maxRetry > 0)
            this.maxretry = c.maxRetry;
        if(c.defLockTimeMs > 0)
            this.lockTimeout = c.defLockTimeMs;
        if(c.maxOnway > 0)
            nMaxOnway = c.maxOnway;
        if(c.minTimeoutMs > 0)
            min_timeout = c.minTimeoutMs;
        if(c.maxTimeoutMs > 0)
            max_timeout = c.maxTimeoutMs;
    }
    public Conf getConf() {
        Conf c = new Conf();
        c.maxTimeoutMs = max_timeout;
        c.minTimeoutMs = min_timeout;
        c.maxRetry = maxretry;
        c.maxOnway = nMaxOnway;
        c.defLockTimeMs = lockTimeout;
        return c;
    }

    //使用treeset来保存优先级
    final private TreeSet<PrioItem> plist = new TreeSet<PrioItem>(new Comparator<PrioItem>(){
        @Override
        public int compare(PrioItem o1, PrioItem o2) {
            if(o1.prio > o2.prio)
                return 1;
            else if(o1.prio < o2.prio)
                return -1;
            return 0;
        }
    });

    // 优先级队列中的元素， 同一优先级下多个groupid
    static final class PrioItem extends LinkedList<String> {
        int prio;
        PrioItem(int p){
            prio = p;
        }
    }

    private String errmsg = null;
    public String err(){
        return errmsg;
    }

    final private boolean __ack(MsgIndex m){
        String groupid = m.getGroupid();

        m.markRemove();

        //然后解除groupid的锁定
        if(!groupid.startsWith("[groupid]")){
            grouplocks.remove(groupid);
        }
        messageids.remove(m.getMsgid());
        _removeLock(m);
//        System.out.println("====why2");
        consume ++;
        return true;
    }


    protected MsgIndex mi(String msgid) {
        return messageids.get(msgid);
    }

    /**
     * 解除消息的锁定状态， 从两个锁定容器(组锁定， 消息锁定）中删除， 但不删除消息本身的
     * @param mi
     */
    final private void _removeLock(MsgIndex mi){
        String groupid = mi.getGroupid();
        String msgid = mi.getMsgid();
        if(lockingMessages.containsKey(msgid)){
            lockingMessages.remove(msgid);
        }
        if(!groupid.startsWith("[group]") && grouplocks.containsKey(groupid)){
            grouplocks.remove(groupid);
        }
    }


    /**
     *  消费者取消息， 根据 gorupid 进行锁定
     * @param brokerid broker的标识id, 可选, 记录以便查找路径
     * @param process_time 锁定时间, 单位秒
     * @return
     */
    public final JournalOP get(String brokerid, long process_time){
        errmsg = null;

        process_time *= 1000;
        if(process_time < min_timeout)
            process_time = min_timeout;
        else if(process_time > max_timeout)
            process_time = max_timeout;

        MsgIndex out_mi = null;
        long out_tm = 0L;

        long time_now = System.currentTimeMillis();

        if(plist.size() == 0){
            errmsg = "zero msg";
            return null;
        }
        if(process_time < 0L)
            process_time = lockTimeout;

        OUTER_LOOP:
        for(Iterator<PrioItem> iter = plist.descendingIterator(); iter.hasNext(); ){
            final PrioItem pi = iter.next();
            if(pi.size() == 0){
                iter.remove();
                continue;
            }

            for(ListIterator<String> iter1=pi.listIterator(); iter1.hasNext();){
                final String groupid = iter1.next();
                final boolean hasGroup = !groupid.startsWith("[groupid]");

                // [groupid] 开头的没带group，则锁定时间不能在group上， 而是在每个消息上
                if(grouplocks.containsKey(groupid) && time_now < grouplocks.get(groupid) && hasGroup){
                    errmsg = "locking groupid:" +groupid;
                    continue; //如果有锁定， 则忽略, 锁定带有超时 lockTimeout, 超出这个时间则锁定失效
                }

                LinkedList<MsgIndex> lm = messages.get(groupid);
                if(lm == null || lm.size() == 0){
                    iter1.remove();
                    messages.remove(groupid);
                    errmsg = "empty group: " +groupid;
                    continue;
                }

                for(ListIterator<MsgIndex> iter2=lm.listIterator(); iter2.hasNext();){
                    final MsgIndex om = iter2.next();
                    if(om.isMarkRemove()){
                        iter2.remove(); //lm.remove(j--); //先处理标记删除的
                        continue;
                    }
                    if(om.getRetry() >= maxretry){ // 删除超过重试次数的
                        //log.error("message {} too ,many retries, move to err, ", om.getMsgid());
                        errmsg = "message "+om.getMsgid()+" too ,many retries, move to err, ";
                        skip(om.getMsgid(), "11", "too many retries"); //这里的问题是进不去存储， 所以实际上放不到err 表中
//                        if(store != null) {
                            final JournalOP op = JournalOP.fail(om, "11", "too many retries");
                            setTblID(op, om);
                            iter2.remove();
                            return op;
//                            store.put(op); //直接放到存储里了， 没法走存储操作线程， 通道一次只能传递一个存储操作
//                        }

                    }
                    if(om.getExpireTime() > 0 && om.getExpireTime() < time_now){ //消息超出有效期的， 则直接删除
                        errmsg = "message "+om.getMsgid() + " expired";
                        log.warn("message {} expired {} ms", om.getMsgid(), time_now-om.getExpireTime());
                        skip(om.getMsgid(), "12", "expired");
//                        if(store != null) {
                            final JournalOP op = JournalOP.fail(om, "12", "expired");
                            setTblID(op, om);
                            iter2.remove();
                            return op;
//                            store.put(op); //直接放到存储里了， 没法走存储操作线程， 通道一次只能传递一个存储操作
//                        }
                    }
                    final long getTime = om.getGetTime();
                    if(getTime == -1 ){ //////// 锁定消息
                        errmsg = "req_time is -1 "+om.getMsgid() + " hasGroup:"+hasGroup;
                        if(hasGroup)
                            break; //在同一组下面如果有锁定的话， 不会在 grouplocks 中出现。 这里用break忽略同组的后续消息
                        else
                            continue;
                    }
                    if(getTime > 0 && time_now < getTime){
                        errmsg = "msg: "+om.getMsgid()+" tblid: "+om.getTblid()+" is locking";
                        continue; // 已经取走并未超时
                    }

                    if(getTime < -1){ //这个消息设定了生效时间， 其值为 0-effective_time
                        if(-getTime > time_now)
                            continue; // 未到生效时间
                    }

                    if(lockingMessages.containsKey(om.getMsgid()))
                        _removeLock(om);
                    if(lockingMessages.size() >= nMaxOnway){
                        for(String mid: lockingMessages.keySet()){ //当前为非锁定的消息， 找一个锁定到期的消息并解开
                            MsgIndex mi1 = lockingMessages.get(mid);
                            if(mi1.getGetTime() <= time_now){
                                _removeLock(mi1);
                                mi1.setGetTime(0);
                                break;
                            }
                        }
                        if(lockingMessages.size() >= nMaxOnway){ //没有可解锁的到期消息， 返回空
                            errmsg = "exceed max on-road message,  sendt:"+lockingMessages.size() + " nMaxOnway:"+ nMaxOnway;
                            return null;
                        }
                    }
                    final long getTime1 = time_now+process_time;
                    om.setGetTime(getTime1); //设定消息取走时间
                    if(!groupid.startsWith("[groupid]"))
                        grouplocks.put(groupid, getTime1); //添加groupid锁定
                    om.setRetry(om.getRetry()+1);
                    out_mi = om;
                    out_tm = getTime1;
                    lockingMessages.put(out_mi.getMsgid(), out_mi);
                    break OUTER_LOOP;

                }
            }
        }

        if(out_mi != null){
            final JournalOP op = JournalOP.get(out_mi.getMsgid(), out_tm, brokerid);
            op.maxwait = out_mi.getGetTime();
            op.retry = out_mi.getRetry();
            op.mi = out_mi;
            setTblID(op, out_mi);
            return op;
        }else {
            return null;
        }
    }

    /**
     * 延迟消费, 在n秒后才允许消费
     * @param msgid
     * @param delay 需要延迟的秒数
     * @return
     */
    public final boolean retry(String msgid, long delay){
        MsgIndex mi = null;

        mi = messageids.get(msgid);
        if(mi == null)
            return false;
        delay = System.currentTimeMillis()+delay*1000;
        mi.setGetTime(delay);

        return true;
    }

    /**
     * 消费确认, 消息归档不可再消费
     * @param messageid
     * @return
     */
    public final boolean ack(String messageid){
        MsgIndex m = messageids.get(messageid);
        if(m == null){
            System.out.printf("=====msg %s not found, size: %d\n", messageid, messageids.size());
            return false;
        }
        return __ack(m);
    }

    /**
     * 忽略消息, 不再可见, 提供原因
     * @param messageid
     * @param rcode
     * @param desc
     * @return
     */
    public final MsgIndex skip(String messageid, String rcode, String desc){
        MsgIndex m = null;

        m = messageids.get(messageid);
        if(m == null)
            return null ;
        errcount ++;

        m.setCommitDesc(desc);

        _removeLock(m);

        m.markRemove();
        messageids.remove(messageid);

        return m;
    }

    /**
     * 数据保存到持久存储中
     * @param op
     */
    public final void setTblID(JournalOP op, MsgIndex mi) {
        if(mi != null){
            op.create_time = mi.getCreateTime();
            op.tableid = mi.getTblid();
        }
    }

    public boolean exists(String msgid) {
        return messageids.containsKey(msgid);
    }

    /**
     *  生产者向队列中加入消息索引
     * @param o 消息索引
     * @return
     */
    public final boolean add(MsgIndex o){
        if(messageids.containsKey(o.getMsgid())){
            System.out.println("__add dulplicate:"+o.getMsgid());
            return false; //剔重
        }
        if(o.getGroupid() == null)
            o.setGroupid("[groupid]"+o.getPriority());

        final long t2 = o.getGetTime();
        final long t1 = System.currentTimeMillis();
        if( t2  > t1){
            lockingMessages.put(o.getMsgid(), o);
        }

        final PrioItem pi1 = new PrioItem(o.getPriority());
        PrioItem pi = plist.ceiling(pi1);
        if(pi == null || pi.prio != pi1.prio){
            plist.add(pi1);
            pi = pi1;
        }
        String groupid = o.getGroupid();
        if( !groupid.startsWith("[groupid]") && o.getGetTime() > 0){
            //恢复消息的锁定时间
            grouplocks.put(groupid, o.getGetTime());
        }

        if( !(pi.size() > 0 && groupid.equals(pi.getLast())))
            pi.add(groupid); //如果最后一个groupid 相同，则不加
        final LinkedList<MsgIndex> lm = messages.get(groupid);
        if(lm == null){
            final LinkedList<MsgIndex> lm1 = new LinkedList<MsgIndex>();
            messages.put(groupid, lm1);
            lm1.add(o);
        }else{
            lm.add(o);
        }
        messageids.put(o.getMsgid(), o);
        total ++;
        return true;
    }

    public boolean rollback(String messageid){
        MsgIndex m = null;

        m = messageids.get(messageid);
        if(m == null)
            return false;
        String groupid = m.getGroupid();
        if(!groupid.startsWith("[groupid]"))
            grouplocks.remove(groupid);

        _removeLock(m);

        m.setGetTime(0);
        return true;
    }


    ///////////////////// 状态采集 /////////////
    long consume = 0L;
    long total = 0L;

    /**
     * 总的进入过的消息数量
     * @return
     */
    public long getTotalCount() { return total; }

    /**
     * 积压消息的最大优先级
     * @return
     */
    public int maxPriority() {
        if(plist.isEmpty())
            return -1;
        return plist.last().prio;
    }

    public int onwayLeft() {
        return nMaxOnway - lockingMessages.size();
    }

    /**
     * 总的消费消息数
     * @return
     */
    public long getConsume() {
        return consume;
    }

    /**
     * 当前积压的最大消息数
     * @return
     */
    public int size(){
        return messageids.size();
    }

    /**
     * 失败数量
     * @return
     */
    public int errCount(){
        return errcount;
    }

    /**
     * 当前在途消息数
     * @return
     */
    public int sending(){
        return lockingMessages.size();
    }

}
