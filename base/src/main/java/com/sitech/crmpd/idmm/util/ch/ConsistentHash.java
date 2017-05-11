package com.sitech.crmpd.idmm.util.ch;

import java.util.*;


public class ConsistentHash<T> {

    private final HashFunction hashFunction;
    private final int numberOfReplicas;
    private final SortedMap<Integer, T> circle = new TreeMap<Integer, T>();

    public ConsistentHash(HashFunction hashFunction, int numberOfReplicas,
                          Collection<T> nodes) {
        this.hashFunction = hashFunction;
        this.numberOfReplicas = numberOfReplicas;

        for (T node : nodes) {
            add(node);
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


    public static void main(String[] args) {
        HashFunction hash = new StrHashFunction();
        LinkedList<String> nodes = new LinkedList<>();
        HashMap<String, Integer> m = new HashMap<>();
        for(int i=0; i<5; i++){
            String n = "this is my Node "+i;
            nodes.add(n);
            m.put(n, 0);
        }
        ConsistentHash<String> ch = new ConsistentHash(hash, 1, nodes);

        long p = 15700240000L;
        for(int i=0; i<1000000; i++){
            String n = ch.get(String.valueOf(p+i));
            m.put(n, m.get(n)+1);
        }
        for(String n: m.keySet()){
            System.out.println(n + ":" + m.get(n));
        }
    }

}