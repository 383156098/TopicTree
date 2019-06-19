package pers.joe;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 树节点
 * @param <V>
 */
public class Node<V> implements Serializable {

    public Node() {

    }

    public CopyOnWriteArrayList<V> getVs() {
        return vs;
    }

    public void setVs(CopyOnWriteArrayList<V> vs) {
        this.vs = vs;
    }

    public ConcurrentHashMap<String, Node<V>> getNodes() {
        return nodes;
    }

    public void setNodes(ConcurrentHashMap<String, Node<V>> nodes) {
        this.nodes = nodes;
    }

    private CopyOnWriteArrayList<V> vs;
    private ConcurrentHashMap<String,Node<V>> nodes = new ConcurrentHashMap<>();

}
