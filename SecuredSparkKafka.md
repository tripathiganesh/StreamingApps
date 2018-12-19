Kafka does not have an implementation in place to support delegation tokens and as a result, passing the keytab and principal in your Spark application is not enough to ensure a working connection between Spark and Kafka in the scenario where both of YARN and Kafka services are secured using Kerberos. 

Before following the instructions in this article, you need to be sure that you have met the software requirements in order to read data securely from a Kafka cluster using Spark as described in the link below:

See [Spark 2 Kafka Integration - Requirements](https://www.cloudera.com/documentation/spark2/latest/topics/spark2_kafka.html#requirements )

These instruction are neccessary till below are implemented :

See [KIP-48 Delegation token support for Kafka](https://cwiki.apache.org/confluence/display/KAFKA/KIP-48+Delegation+token+support+for+Kafka)

See [KAFKA-1696](https://issues.apache.org/jira/browse/KAFKA-1696)



The following link has a sample application that can be built and used to run and test a secure Kafka cluster. It has clear instructions on how to submit a Spark application in the case where only the Kafka service is secured:

See [spark-secure-kafka-app-code](https://github.com/markgrover/spark-secure-kafka-app)

If both your YARN and Kafka services are secured with Kerberos then you need to follow the instructions below.
Assuming you have USER who's going to run the Spark streaming application and will also authenticate as the Kafka client then you will need to run the following command:
 
```sh
SPARK_KAFKA_VERSION=0.10 spark2-submit --master yarn --deploy-mode cluster 
--keytab /PATHTO/USER.keytab
--principal USER@YOUR.COMPANY.COM
--files /PATHTO/spark_jaas.conf#spark_jaas.conf,/PATHTO/USER_COPY2.keytab#USER_COPY2.keytab 
--driver-java-options "-Djava.security.auth.login.config=./spark_jaas.conf" 
--conf "spark.executor.extraJavaOptions=-Djava.security.auth.login.config=./spark_jaas.conf" 
--class COM.COMPANY.PACKAGE.MAINCLASS /PATHTO/APP.jar [APPLICATION_ARGS]
```
In the command mentioned, you will notice that we're passing two keytab files, USER.keytab with the --keytab argument and USER_COPY2.keytab using the --files argument. The first argument is used to authenticate with YARN and is required for long running Spark streaming applications, while the second keytab file being passed using the --files  is needed in order to authenticate with Kafka as a client. In case you're going to use the same user, then both the keytab files need to have unique names or else the command will fail with the following exception:
 
Exception in thread "main" java.lang.IllegalArgumentException: Attempt to add (file:/PATHTO/USER.keytab) multiple times to the distributed cache.
 
The workaround here is to simply make a copy of your keytab file and rename it. In this example, both these files are located under the directory /PATHTO/ on the host machine where the spark2-submit command is being executed from.

The spark_jaas.conf file will contain the following:
```java
KafkaClient {
    com.sun.security.auth.module.Krb5LoginModule required
    useKeyTab=true
    storeKey=true
    keyTab="./USER_COPY2.keytab"
    useTicketCache=false
    serviceName="kafka"
    principal="USER@YOUR.COMPANY.COM";
};
```
Notice we added ```./``` in front of the Kafka client user's keytab filename. This should not be changed since we have distributed this file to the executors using the --files argument and YARN will localize the file which will reside in the container's working directory. 
