
1. Install CDH, Kafka v2.0.x parcel (or higher) and Cloudera Manager 5.5.3 (or higher. Secure features are introduced at these version levels)
 
2. From Cloudera Manager, navigate to Kafka > Configurations. Set SSL client authentication to none. Set Inter Broker Protocol to SASL_PLAINTEXT.
 
3. Make sure that listeners = SASL_PLAINTEXT://<broker_host>:<port> is present in the Kafka broker logs  /var/log/kafka/server.log or in /etc/kafka/conf/server.properties. Note: The "listeners" setting is configured by visiting: CM > Kafka > Instances > Click on "Kafka" broker > Configuration > Kafka Broker Advanced Configuration Snippet (Safety Valve) for kafka.properties. The safety-valve needs to be updated for each of the brokers so that the individual brokers have the appropriate ports available for listening.
 
4. Create a jaas.conf file for your client with the following contents to use with cached Kerberos credentials (you can modify this to use the keytab files instead of cached credentials):
```sh 
KafkaClient {
com.sun.security.auth.module.Krb5LoginModule required
useTicketCache=true;
};
Note: If you kinit first then use the above configuration. If you use the key tab, add following lines to:
KafkaClient {
com.sun.security.auth.module.Krb5LoginModule required
useKeyTab=true
keyTab="/etc/security/keytabs/kafka_server.keytab"
principal="kafka/kafka1.hostname.com@EXAMPLE.COM";
};
```
Note: Cloudera Manager will create the jaas.conf file for brokers.
 
5. Create the client.properties file for convenience using the following properties set inside:
```sh
security.protocol=SASL_PLAINTEXT
sasl.kerberos.service.name=kafka
```
            Note, above is for SASL_PLAINTEXT brokers only. For those who decide to use SASL_SSL broker, use the following:

```sh
security.protocol=SASL_SSL 
ssl.truststore.location=/path/to/truststore/file 
ssl.truststore.password={password}
sasl.kerberos.service.name=kafka
```

6. Test with the Kafka console producer and consumer. To obtain a Kerberos ticket-granting ticket (TGT):
```sh
$ kinit <user>
 ```
7. Verify that the the topic you created exists:
```sh
$ kafka-topics --list --zookeeper <zkhost>:2181
``` 
8. Verify that  the jaas.conf file is used by setting the environment:
```sh
$ export KAFKA_OPTS="-Djava.security.auth.login.config=/home/user/jaas.conf"
```
Note: Every time you have a new shell session and want to use jaas.conf , you need to set that environment variable. This is active as long as the shell session is active.
 
9. Run a Kafka console producer:
```sh
$ kafka-console-producer --broker-list <anybroker>:9092 --topic test1 
--producer.config client.properties
 ```
10. Run a Kafka console consumer:
```sh
$ kafka-console-consumer --new-consumer --topic test1 --from-beginning 
--bootstrap-server <anybroker>:9092 --consumer.config client.properties
```
11. Run a Kafka tool class:
```sh
$ kafka-run-class kafka.admin.ConsumerGroupCommand --bootstrap-server <anyborker>:9092 --list 
--new-consumer --command-config client.properties
```

See : http://docs.oracle.com/javase/7/docs/jre/api/security/jaas/spec/com/sun/security/auth/module/Krb5LoginModule.html
https://issues.apache.org/jira/browse/KAFKA-1686
