package com.sitech.crmpd.idmm.util.ch;

import java.util.*;


public class ConsistentHash<T> {

    private final HashFunction hashFunction;
    private final int numberOfReplicas;
    private final SortedMap<Integer, T> circle = new TreeMap();

    public ConsistentHash(HashFunction hashFunction, int numberOfReplicas,
                          Collection<T> nodes) {
        this.hashFunction = hashFunction;
        this.numberOfReplicas = numberOfReplicas;

        if(nodes != null) {
            for (T node : nodes) {
                add(node);
            }
        }
    }

    public void add(T node) {
        for (int i = 0; i <numberOfReplicas; i++) {
            circle.put(hashFunction.hash(node.toString() + i), node);
        }
    }

    public void remove(T node) {
        for (int i = 0; i <numberOfReplicas; i++) {
            circle.remove(hashFunction.hash(node.toString() + i));
        }
    }

    public T get(Object key) {
        if (circle.isEmpty()) {
            return null;
        }
        int hash = hashFunction.hash(key);
        if (!circle.containsKey(hash)) {
            SortedMap<Integer, T> tailMap = circle.tailMap(hash);
            hash = tailMap.isEmpty() ? circle.firstKey() : tailMap.firstKey();
        }
        return circle.get(hash);
    }


    /**
     * 预判新加入节点 会影响到的节点, 暂时不考虑使用vnode(numberOfReplicas>1)的情况
     * @param node
     * @return
     */
    public T affected(T node) {
        return get(node.toString()+0);
    }

    /**
     * 获得按顺序的列表， 以便打印验证
     * @return
     */
    public List<T> getCircle() {
        List<T> r = new LinkedList<>();

        SortedMap<Integer, T> c = circle;
        while(!c.isEmpty()){
            int hash = c.firstKey();
            r.add(c.get(hash));
            c = c.tailMap(hash+1);
        }
        return r;
    }

    public static void main(String[] args) {

        // 测试一致性hash的分布均匀性
        HashFunction hash = new StrHashFunction();
        LinkedList<String> nodes = new LinkedList<>();
        HashMap<String, Integer> m = new HashMap<>();

        // 添加5个单节点
        for(int i=0; i<5; i++){
            String n = "this is my Node "+i;
            nodes.add(n);
            m.put(n, 0);
        }
        ConsistentHash<String> ch = new ConsistentHash(hash, 1, nodes);

        long p = 15700240000L;
        for(int i=0; i<1000000; i++){
            String n = ch.get(String.valueOf(p+i));
            m.put(n, m.get(n)+1); //命中数累加
        }

        // 打印每个节点的命中数
        for(String n: m.keySet()){
            System.out.println(n + ":" + m.get(n));
        }

        ch.remove("hello");
    }

}