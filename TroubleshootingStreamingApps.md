Error :
```java 
ERROR DirectKafkaInputDStream: ArrayBuffer(org.apache.spark.SparkException: Couldn't find leaders for Set([sometopic,8]))
```
It is recommended to have 3 replicas for each partition just in case one of the hosts becomes unresponsive or goes down. This is easiest done at the time of topic creation.
If topics are already created with a replication factor of 1, use the following example to increase the replicationfactor as described in the document https://kafka.apache.org/081/ops.html.

In the following example, there is a topic that has been created called topicFirst with 2 partitions with replication factor of 1 which will be increased to 3 just for partition 0. The broker ids are 60,61 and 62

Zookeeper server is on the localhost where the command was executed, and the kafka chroot is /kafka.
```sh
kafka-topics --zookeeper localhost:2181/kafka --describe
Topic:topicFirst PartitionCount:2 ReplicationFactor:1 Configs:
Topic: topicFirst Partition: 0 Leader: 62 Replicas: 62 Isr: 62
Topic: topicFirst Partition: 1 Leader: 62 Replicas: 62 Isr: 62
```
1. Create a json file using a text editor that will specify the partitions that will be replicated
Ex. vi increase-replication-factor.json

2. Add the following lines like the following example to increase the replication factor to 3:
```json
{"version":1,
"partitions":[{"topic":"topicFirst","partition":0,"replicas":[60,61,62]}]}
```
3. Run the following command:
```sh
kafka-reassign-partitions --zookeeper localhost:2181/kafka --reassignment-json-file increase-replication-factor.json --execute
```
If the command was successful, an output like this will be seen on the console:

-----

Current partition replica assignment
```json
{"version":1,"partitions":[{"topic":"topicFirst","partition":1,"replicas":[62]},{"topic":"topicFirst","partition":0,"replicas":[62]}]}
```
Save this to use as the --reassignment-json-file option during rollback
Successfully started reassignment of partitions
```json
{"version":1,"partitions":[{"topic":"topicFirst","partition":0,"replicas":[60,61,62]}]}
```

-------

4. Verify the creation of additional replicas:
```sh
kafka-topics --zookeeper localhost:2181/kafka --describe
Topic:topicFirst PartitionCount:2 ReplicationFactor:1 Configs:
Topic: topicFirst Partition: 0 Leader: 62 Replicas: 60,61,62 Isr: 62,60,61
Topic: topicFirst Partition: 1 Leader: 62 Replicas: 62 Isr: 62
```
