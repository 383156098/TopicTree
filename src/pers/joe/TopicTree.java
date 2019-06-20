package pers.joe;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @auther Joe
 * @data 2018/11/21 12:04
 * @description <P>
 * 该MqttTopicTree 是一个线程安全高速缓存
 * 支持 普通订阅,通配符订阅
 * 不提供共享订阅 超出树的设计范围..
 * </P>
 * @since JDK 1.8
 */
public class TopicTree<V>{


    /**
     * Hash 树根节点
     */
    private Node<V> rootNode = new Node<>();

    /**
     * 数的节点限制 模式不限制
     */
    private int limitNode = -1;

    /**
     * 记录当前节点数
     */
    private AtomicInteger nodeSum = new AtomicInteger(0);


    /**
     * 树的节点总数
     *
     * @return int
     */
    public int size() {
        return nodeSum.get();
    }


    public TopicTree(int limitNode) {
        this.limitNode = limitNode;
    }

    public TopicTree(){
        this(-1);
    }

    /**
     * 分层保存主题
     * <p>
     * 该保存方法禁止一些没有分层的主题
     * </P>
     *
     * @param topicName topicName
     * @param v         v
     */
    public void saveTopic(String topicName, V v) {
        List<String> levelTopic = solveTopic(topicName);
        if (limitNode == -1 || (limitNode - nodeSum.get()) >= levelTopic.size()) {
            topicFilter(levelTopic);
            addTree(levelTopic, rootNode, v, 0);
        } else {
            throw new TopicException("tree node is reach the ceiling >" + limitNode);
        }
    }


    /**
     * 递归保存主题
     * @param strings  strings
     * @param rootNode rootNode
     * @param v        v
     * @param c        c
     */
    private void addTree(List<String> strings, Node<V> rootNode, V v, int c) {
        ConcurrentHashMap<String, Node<V>> concurrentHashMap = rootNode.getNodes();
        String key = strings.get(c);
        Node<V> node = saveAndGet(key, concurrentHashMap);
        if (c != strings.size() - 1) {
            addTree(strings, node, v, ++c);
        } else {
            CopyOnWriteArrayList<V> list = node.getVs();
            if (list == null) {
                list = new CopyOnWriteArrayList<>();
                list.add(v);
                node.setVs(list);
            } else {
                list.add(v);
            }
        }
    }




    /**
     * 自动区分 通配符主题和非通配符主题
     */
    public List<V> search(String topic) {
        List<String> levelTopic = solveTopic(topic);
        return searchTree(levelTopic, rootNode, topicFilter(levelTopic));
    }


    /**
     * 通过普通主题 查找通配符主题和当前主题
     * @param topic topic
     * @return List<V>
     */
    public List<V> notWildCardSearchTree(String topic) {
        List<String> levelTopic = solveTopic(topic);
        if (!topicFilter(levelTopic)) {
            CopyOnWriteArrayList<V> copyOnWriteArrayList = new CopyOnWriteArrayList<>();
            searchTree(levelTopic, rootNode, 0, copyOnWriteArrayList);
            return copyOnWriteArrayList;
        }
        throw new IllegalArgumentException("notWildCardSearchTree() function not support include wildcard topic.");
    }


    /**
     * 通过WildCard主题查找 普通主题
     * @param topic topic
     * @return List<V>
     */
    public List<V> wildCardSearchTree(String topic) {
        List<String> levelTopic = solveTopic(topic);
        if (topicFilter(levelTopic)) {
            CopyOnWriteArrayList<V> copyOnWriteArrayList = new CopyOnWriteArrayList<>();
            wildCardSearch(levelTopic, rootNode, 0, copyOnWriteArrayList);
            return copyOnWriteArrayList;
        }

        throw new IllegalArgumentException(topic + " topic is not carry wildcard.");
    }


    /**
     * 递归搜寻主题找到主题的根源
     * 算法通过递归的来实现树的倒退遍历，利用了方法的堆栈特性
     *
     * @param strings  strings
     * @param rootNode rootNode
     */
    private CopyOnWriteArrayList<V> searchTree(List<String> strings, Node<V> rootNode, boolean isWillCard) {
        CopyOnWriteArrayList<V> copyOnWriteArrayList = new CopyOnWriteArrayList<>();
        if (isWillCard) {
            wildCardSearch(strings, rootNode, 0, copyOnWriteArrayList);
        } else {
            searchTree(strings, rootNode, 0, copyOnWriteArrayList);
        }
        return copyOnWriteArrayList;
    }



    private void addContainer(List<String> strings, Node<V> node, int c, CopyOnWriteArrayList<V> container) {
        if (c != strings.size() - 1) {
            if (node != null) {
                willCardSearchTree(strings, node, c + 1, container);
            }
        } else {
            updateContainer(container, node);
        }

    }

    private void updateContainer(CopyOnWriteArrayList<V> container, Node<V> node) {
        if (node != null) {
            CopyOnWriteArrayList<V> list = node.getVs();
            if (list != null) {
                container.addAll(list);
            }
        }
    }

    /**
     * 全树遍历, 包含wildCard topic
     */
    private void traversal(ConcurrentHashMap<String, Node<V>> concurrentHashMap, List<V> container) {
        Set<Map.Entry<String, Node<V>>> entries = concurrentHashMap.entrySet();
        Iterator<Map.Entry<String, Node<V>>> iterator = entries.iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Node<V>> map = iterator.next();
            Node<V> node = map.getValue();
            if (node != null) {
                CopyOnWriteArrayList<V> list = node.getVs();
                if (list != null) {
                    container.addAll(list);
                }
                ConcurrentHashMap<String, Node<V>> concurrentHashMap1 = node.getNodes();
                if (concurrentHashMap1 != null) {
                    traversal(concurrentHashMap1, container);
                }
            }

        }
    }

    /**
     * 全树遍历，不包含 wildCard topic
     */
    private void traverOrdinaryTopic(ConcurrentHashMap<String, Node<V>> concurrentHashMap, List<V> container) {
        Set<Map.Entry<String, Node<V>>> entries = concurrentHashMap.entrySet();
        for (Map.Entry<String, Node<V>> map : entries) {
            String key = map.getKey();
            if (!argumentKey(key)) {
                Node<V> node = map.getValue();
                if (node != null) {
                    CopyOnWriteArrayList<V> list = node.getVs();
                    if (list != null) {
                        container.addAll(list);
                    }
                    ConcurrentHashMap<String, Node<V>> concurrentHashMap1 = node.getNodes();
                    if (concurrentHashMap1 != null) {
                        traverOrdinaryTopic(concurrentHashMap1, container);
                    }
                }
            }
        }
    }

    private void addContainer(Node<V> node, List<String> levelTopic, int c, CopyOnWriteArrayList<V> container) {
        if (node != null) {
            if (c != levelTopic.size() - 1) {
                searchTree(levelTopic, node, c + 1, container);
            } else {
                updateContainer(container, node);
            }
        }
    }


    /**
     * 通过普通主题 找出当前主题和通配符主题
     * example:
     * 假设当前主题树中存在三个主题 如下：
     * 我们用 "fate/stay/night" 主题到主题树中遍历查找，查找结果为 node1,node2,node3,node4
     * 如果我们使用 "fate/+/night" 主题到主题树中遍历查找，查找结果为 node2,node3. (因为该方法不支持通配符匹配如果想支持通配符可以调用
     * willCardSearchTree() 方法进行查找.)
     * {
     * fate/stay/night ---> node1,
     * fate/+/night ----> node2,node3,
     * fate/stay/# ----> node4
     * }
     *
     * @param levelElement 主题分解之后的节点
     * @param rootNode     根节点
     * @param c            层
     * @param container    接收数据容器
     */
    private void searchTree(List<String> levelElement, Node<V> rootNode, int c, CopyOnWriteArrayList<V> container) {
        ConcurrentHashMap<String, Node<V>> concurrentHashMap = rootNode.getNodes();
        String key = levelElement.get(c);
        if (concurrentHashMap != null) {
            Node<V> commNode = concurrentHashMap.get(key);
            addContainer(commNode, levelElement, c, container);

            // fate/stay/night 主题正常遍历树是无法查找到 fate/stay/night/# 所以需要最后补充遍历
            if (commNode != null) {
                if (c == levelElement.size() - 1) {
                    ConcurrentHashMap<String, Node<V>> nextHexMap = commNode.getNodes();
                    if (nextHexMap != null) {
                        Node<V> nextHexNode = nextHexMap.get("#");
                        if (nextHexNode != null) {
                            CopyOnWriteArrayList<V> list = nextHexNode.getVs();
                            if (list != null) {
                                container.addAll(list);
                            }
                        }
                    }
                }
            }
            Node<V> addNode = concurrentHashMap.get("+");
            addContainer(addNode, levelElement, c, container);
            Node<V> hexNode = concurrentHashMap.get("#");
            if (hexNode != null) {
                CopyOnWriteArrayList<V> copyOnWriteArrayList = hexNode.getVs();
                if (copyOnWriteArrayList != null) {
                    container.addAll(copyOnWriteArrayList);
                }
            }
        }
    }



    public void wildCardSearch(List<String> levelTopic,Node<V> rootNode,int c,CopyOnWriteArrayList<V> container){
        ConcurrentHashMap<String,Node<V>> concurrentHashMap = rootNode.getNodes();
        String key = levelTopic.get(c);
        if(concurrentHashMap!=null){
            if("#".equals(key)){
                traverOrdinaryTopic(concurrentHashMap,container);
            }else if("+".equals(key)){
                if(checkSize(c,levelTopic)){
                    for(Map.Entry<String,Node<V>> entry:concurrentHashMap.entrySet()){
                        if(!argumentKey(entry.getKey())){
                            Node<V> node = entry.getValue();
                            if(node!=null){
                                wildCardSearch(levelTopic,node,c+1,container);
                            }
                        }
                    }
                }else{
                    Set<Map.Entry<String, Node<V>>> entries = concurrentHashMap.entrySet();
                    for (Map.Entry<String, Node<V>> entry : entries) {
                        if(!argumentKey(entry.getKey())){
                            Node<V> node = entry.getValue();
                            updateContainer(container, node);
                        }
                    }
                }
            }else{
                Node<V> node = concurrentHashMap.get(key);
                if(checkSize(c,levelTopic)){
                    wildCardSearch(levelTopic,rootNode,c+1,container);
                }else{
                    updateContainer(container,node);
                }
            }
        }
    }

    /**
     * 通配符主题search tree
     * example:
     * 假设当前主题树中存在四个主题 如下：
     * {
     * fate/stay/night/heaven/feel --->node0
     * fate/stay/night ---> node1,
     * fate/+/night ----> node2,node3,
     * fate/stay/# ----> node4
     * fate/#      ----> node5
     * }
     * 用 "fate/stay/#" 主题到主题树中遍历查找，查找结果为 node0,node1,node2,node3,node4,node5
     * 用 "fate/stay/+" 主题到主题树中遍历查找，查找结果为 node1，node2，node3，node4，node5
     * 用 "fate/stay/+/+/feel" 主题到主题树中遍历查找，查找结果 node0,node4,node5
     * 具体的主题匹配规则请查看MQTT 主题标准
     */
    private void willCardSearchTree(List<String> levelElement, Node<V> rootNode, int c, CopyOnWriteArrayList<V> container) {
        String key = levelElement.get(c);
        ConcurrentHashMap<String, Node<V>> concurrentHashMap = rootNode.getNodes();
        if (concurrentHashMap != null) {
            if ("#".equals(key)) {
                CopyOnWriteArrayList<V> copyOnWriteArrayList = rootNode.getVs();
                if (copyOnWriteArrayList != null) {
                    container.addAll(copyOnWriteArrayList);
                }
                traversal(concurrentHashMap, container);
            } else if ("+".equals(key)) {
                if (c != levelElement.size() - 1) {
                    Set<Map.Entry<String, Node<V>>> entries = concurrentHashMap.entrySet();
                    for (Map.Entry<String, Node<V>> map : entries) {
                        //如果key = # 该主题匹配当前主题
                        if ("#".equals(map.getKey())) {
                            updateContainer(container, map.getValue());
                        } else {
                            //如果不是# 忽略当前层 递归下一层继续匹配
                            Node<V> node = map.getValue();
                            if (node != null) {
                                willCardSearchTree(levelElement, node, c + 1, container);
                            }
                        }
                    }
                } else {
                    Set<Map.Entry<String, Node<V>>> entries = concurrentHashMap.entrySet();
                    for (Map.Entry<String, Node<V>> entry : entries) {
                        Node<V> node = entry.getValue();
                        updateContainer(container, node);
                    }
                }
            } else {
                Node<V> node = concurrentHashMap.get(key);
                addContainer(levelElement, node, c, container);
                Node<V> plusNode = concurrentHashMap.get("+");
                addContainer(levelElement, plusNode, c, container);
                Node<V> hexNode = concurrentHashMap.get("#");
                if (hexNode != null) {
                    CopyOnWriteArrayList<V> copyOnWriteArrayList = hexNode.getVs();
                    if (copyOnWriteArrayList != null) {
                        container.addAll(copyOnWriteArrayList);
                    }
                }

            }
        }
    }


    private CopyOnWriteArrayList<V> singleSearchTree(List<String> strings, Node<V> rootNode, int c) {
        ConcurrentHashMap<String, Node<V>> concurrentHashMap = rootNode.getNodes();
        String key = strings.get(c);
        if (concurrentHashMap != null) {
            Node<V> node = concurrentHashMap.get(key);
            if (node != null) {
                if (c != strings.size() - 1) {
                    return singleSearchTree(strings, node, c + 1);
                } else {
                    CopyOnWriteArrayList<V> list = node.getVs();
                    if (list != null) {
                        return list;
                    }
                }
            }
        }
        return new CopyOnWriteArrayList<>();
    }


    private void removeTree(List<String> strings, Node<V> rootNode, int c) {
        ConcurrentHashMap<String, Node<V>> concurrentHashMap = rootNode.getNodes();
        String key = strings.get(c);
        if (concurrentHashMap != null) {
            Node<V> node = concurrentHashMap.get(key);
            if (node != null) {
                if (c != strings.size() - 1) {
                    removeTree(strings, node, c + 1);
                    ConcurrentHashMap<String, Node<V>> map = node.getNodes();
                    CopyOnWriteArrayList<V> list = node.getVs();
                    if ((list == null && map.size() == 0) || (map.size() == 0 && list.size() == 0)) {
                        concurrentHashMap.remove(key);
                        nodeSum.decrementAndGet();
                    }
                } else {
                    ConcurrentHashMap<String, Node<V>> nextLevel = node.getNodes();
                    if (nextLevel == null || nextLevel.size() == 0) {
                        concurrentHashMap.remove(key);
                        nodeSum.decrementAndGet();
                    }
                }
            }
        }
    }

    private boolean checkSize(int c,List<String> levelTopic){
        return c!=levelTopic.size()-1;
    }


    private boolean argumentKey(String key){
        return "+".equals(key) || "#".equals(key);
    }

    /**
     * 删除指定主题下的 某个数据
     * @param topic          topic
     * @param resultListener resultListener
     */
    public void remove(String topic, SearchResultListener<V> resultListener) {
        List<String> levelTopic = solveTopic(topic);
        topicFilter(levelTopic);
        if (resultListener != null) {
            resultListener.result(singleSearchTree(levelTopic, rootNode, 0));
        }
    }

    /**
     * 移除当前主题中的V
     * @param topicName topicName
     */
    public void remove(String topicName) {
        List<String> levelTopic = solveTopic(topicName);
        topicFilter(levelTopic);
        removeTree(levelTopic, rootNode, 0);
    }


    /**
     * 保存并返回Node<V>
     *
     * @param key               key
     * @param concurrentHashMap concurrentHashMap
     * @return pers.joe.Node<V>
     */
    private Node<V> saveAndGet(String key, ConcurrentHashMap<String, Node<V>> concurrentHashMap) {
        if (concurrentHashMap == null) {
            concurrentHashMap = new ConcurrentHashMap<>();
        }
        Node<V> node = concurrentHashMap.get(key);
        if (node == null) {
            node = new Node<>();
            concurrentHashMap.put(key, node);
            nodeSum.incrementAndGet();
            return node;
        }
        return node;
    }


    /**
     * topic filter
     *
     * @param levelTopic topicName
     */
    private boolean topicFilter(List<String> levelTopic) throws IllegalArgumentException {

        boolean isWillCard = false;
        int levelTopicSize = levelTopic.size() - 1;
        for (int i = 0; i < levelTopic.size(); i++) {
            String level = levelTopic.get(i);
            if ("#".equals(level)) {
                if ((i == 0 && levelTopicSize != 0) || (i != 0 && levelTopicSize != i)) {
                    throw new IllegalArgumentException("# multi level illegal...");
                }
                isWillCard = true;
            }
            if ("+".equals(level)) {
                isWillCard = true;
            }
        }
        return isWillCard;
    }


    /**
     * 分解主题
     * @param str str 原字符串
     * @return List<String>
     */
    private List<String> solveTopic(String str) {
        if (str != null && str.length() > 0 && str.length() < 65535) {
            List<String> list = new ArrayList<>();
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                if (c == '/') {
                    list.add(stringBuilder.toString());
                    stringBuilder = new StringBuilder();
                } else {
                    stringBuilder.append(c);
                    if (i == str.length() - 1) {
                        list.add(stringBuilder.toString());
                    }
                }
            }
            return list;
        }
        throw new IllegalArgumentException("illegal topic:" + str);
    }
}
