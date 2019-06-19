import pers.joe.SearchResultListener;
import pers.joe.TopicTree;

import java.util.List;

public class TestMain {

    /**
     * 通配符主题search tree
     * example:
     * 假设当前主题树中存在四个主题 如下：
     *  {
     *       fate/stay/night/heaven/feel --->node0
     *       fate/stay/night ---> node1,
     *       fate/+/night ----> node2,node3,
     *       fate/stay/# ----> node4
     *       fate/#      ----> node5
     *  }
     * 用 "fate/stay/#" 主题到主题树中遍历查找，查找结果为 node0,node1,node2,node3,node4,node5
     * 用 "fate/stay/+" 主题到主题树中遍历查找，查找结果为 node1，node2，node3，node4，node5
     * 用 "fate/stay/+/+/feel" 主题到主题树中遍历查找，查找结果 node0,node4,node5
     * 具体的主题匹配规则请查看MQTT 主题标准
     *
     */
    public static void main(String[] args) {
        TopicTree<String> mqttTopicTree = new TopicTree<>(Integer.MAX_VALUE);


//        mqttTopicTree.saveTopic("/topic","/topic");
//        mqttTopicTree.saveTopic("topic/#","topic/#");

//        mqttTopicTree.saveTopic("fate/stay/night/heaven/feel","node0");
//        mqttTopicTree.saveTopic("fate/stay/night","node1");
//        mqttTopicTree.saveTopic("fate/+/night ","node2");
//        mqttTopicTree.saveTopic("fate/+/night ","node3");
//        mqttTopicTree.saveTopic("fate/stay/#","node4");

        mqttTopicTree.saveTopic("fate/#","node5");
        mqttTopicTree.saveTopic("fate/+","node523");
        mqttTopicTree.saveTopic("fate/+/#","node52355");
        mqttTopicTree.saveTopic("fate/stay","node52300");
//        mqttTopicTree.saveTopic("#","node52");
//        mqttTopicTree.saveTopic("#","node52c");

//        mqttTopicTree.saveTopic(  "fate/stay4/night","caster4");
//        mqttTopicTree.saveTopic(  "fate/stay4/night/#","caster5");
//        mqttTopicTree.saveTopic("+/+/night","saber1");
//        mqttTopicTree.saveTopic("+/+/#","saberpp");
//        mqttTopicTree.saveTopic("+/#","saberpp3");
//        mqttTopicTree.saveTopic("fate/#","saberpp333");
//        mqttTopicTree.saveTopic("fate/stay4/#","sad");
//        mqttTopicTree.saveTopic("#","zxcpp");

        long start = System.currentTimeMillis();
        //BUG 待修复
//        for(int i=0;i<10000000;i++){
//            List<String> s = mqttTopicTree.search("fate/stay/night");
//        }


        List<String> s1 = mqttTopicTree.search("fate/+");
        System.out.println(s1.toString());
        mqttTopicTree.remove("fate/stay", new SearchResultListener<String>() {
            @Override
            public void result(List<String> v) {
                for(String s:v){
                    v.remove(s);
                }
            }
        });
        List<String> s = mqttTopicTree.search("fate/+");

        System.out.println(s.toString());

        System.out.println(System.currentTimeMillis() - start);

    }
}
