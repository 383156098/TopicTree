import pers.joe.SearchResultListener;
import pers.joe.TopicTree;

import java.util.List;

public class TestMain {


    public static void main(String[] args) {
        TopicTree<String> topicTree = new TopicTree<>(Integer.MAX_VALUE);


        topicTree.saveTopic("fate/#","node5");
        topicTree.saveTopic("fate/+","node523");
        topicTree.saveTopic("fate/+/#","node52355");
        topicTree.saveTopic("fate/stay/+","node530");
        topicTree.saveTopic("fate/stay","node52300");
        topicTree.saveTopic("fate/stay/night","node5230022");

        //results --> node5230022, node530, node52355, node5
        List<String> commSearch = topicTree.search("fate/stay/night");
        System.out.println(commSearch.toString());

        //results --> node5230022
        List<String> wildCardSearch = topicTree.search("fate/+/+");
        System.out.println(wildCardSearch.toString());


    }
}
