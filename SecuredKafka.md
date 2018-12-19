
1) If you are building a custom Java client consumer/producer program, then:

     1.1) Set client config parameters (security.protocol and sasl.kerberos.service.name) before init Kafka client:
```java
//append sasl property before init Kafka client
props.put("security.protocol", "SASL_PLAINTEXT");
props.put("sasl.kerberos.service.name", "kafka");

//init Kafka client
Producer<String, String> producer = new KafkaProducer<>(props);
```
    1.2) Create a jaas config file like below if you would like to kinit before invoke your Java program:

```sh
KafkaClient {
com.sun.security.auth.module.Krb5LoginModule required
useTicketCache=true;
};
```
    Create a jaas config file like below if you just want to pass through a keytab file instead of kinit first before run your Java program:
```sh
KafkaClient {
com.sun.security.auth.module.Krb5LoginModule required
useKeyTab=true
keyTab="/path/to/kafka.keytab"
principal="kafka/kafka1.hostname.com@EXAMPLE.COM";
};
```
    1.3) when run your Kafka Java program, give the jaas config file through java -D option:
```sh
-Djava.security.auth.login.config=/path/to/jaas.conf
```

Reference :
```sh
How SASL client authentication work in Kafka Java Client:
When a KafkaClient instance is run with the SASL_PLAINTEXT or SASL_SSL mode, it invokes the SaslChannelBuilder:
            https://github.com/cloudera/kafka/blob/cdh5-0.9.0_2.0.1/clients/src/main/java/org/apache/kafka/common/network/ChannelBuilders.java#L45-L50
Then SaslChannelBuilder builds a LoginManager object:
            https://github.com/cloudera/kafka/blob/cdh5-0.9.0_2.0.1/clients/src/main/java/org/apache/kafka/common/network/ChannelBuilders.java#L45-L50
Then LoginManager instance spawns a Login object and its thread:
           https://github.com/cloudera/kafka/blob/cdh5-0.9.0_2.0.1/clients/src/main/java/org/apache/kafka/common/security/kerberos/LoginManager.java#L66-L90
           and
           https://github.com/cloudera/kafka/blob/cdh5-0.9.0_2.0.1/clients/src/main/java/org/apache/kafka/common/security/kerberos/LoginManager.java#L44-L46
Now, Login's inner thread runs a periodic ticket re-login (keytab) or renewal (renwable ticket cache) logic continually or at least until the point of a failure:   https://github.com/cloudera/kafka/blob/cdh5-0.9.0_2.0.1/clients/src/main/java/org/apache/kafka/common/security/kerberos/Login.java#L134-L139
```
