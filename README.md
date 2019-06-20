# TopicTree
According MQTT topic standard design topic tree.
topic format is the MQTT topic standard. Support common and wildcard search.

## new TopicTree
```
 TopicTree<String> topicTree = new TopicTree<>(size);
 ```
 
 ## save topic
 ```
 //save "fate/stay/night" topic, data is "saber","caster"
 topic.saveTopic("fate/stay/night","saber");
  topic.saveTopic("fate/stay/night","caster");
 ```
 
 ## search topic
 if your topic is not carrying wildcards it will find the current topic and wildcard topic.if topic carries wildcards only find common topic. 
 ```
 //not wildcard search
 List<String> commSearch = topicTree.search("fate/stay/night");
 //wildcard search
 List<String> wildCardSearch = topicTree.search("fate/#");
 ```
 
 ## remove topic
 ```
 topicTree.remove("fate/stay/night")
 ```
 
 ## remove data from topic
 
 ```
 topicTree.remove("fate/stay/night",v->{
  v.remove("caster")
 })
 ```
 
 
