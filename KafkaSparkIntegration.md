### Spark Streaming + Kafka Integration (Kafka broker version >= 0.10.0)

maven artifact `artifactId = spark-streaming-kafka-0-10_2.11`
See [Integration guide](https://spark.apache.org/docs/2.2.0/streaming-kafka-0-10-integration.html)


```scala
import org.apache.spark._
import org.apache.spark.streaming._
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe


val ssc = new StreamingContext(sc, Seconds(10))

val kafkaParams = Map[String, Object]( "bootstrap.servers" -> "nightly513-1.vpc.cloudera.com:9093", "key.deserializer" -> classOf[StringDeserializer], "value.deserializer" -> classOf[StringDeserializer], "group.id" -> "testgroup", "auto.offset.reset" -> "latest", "enable.auto.commit" -> (false: java.lang.Boolean) ,"sasl.kerberos.service.name" -> "kafka", "security.protocol" -> "SASL_SSL", "ssl.truststore.location" -> "/etc/cdep-ssl-conf/CA_STANDARD/truststore.jks", "ssl.truststore.password" -> "cloudera", "ssl.key.password" -> "cloudera")
`)

val topics = Array("test1")
val stream = KafkaUtils.createDirectStream[String, String]( ssc, PreferConsistent, Subscribe[String, String](topics, kafkaParams))

stream.foreachRDD { rdd =>
  val offsetRanges = rdd.asInstanceOf[HasOffsetRanges].offsetRanges
  rdd.foreachPartition { iter =>
    val o: OffsetRange = offsetRanges(TaskContext.get.partitionId)
    println(s"${o.topic} ${o.partition} ${o.fromOffset} ${o.untilOffset}")
  }
}
stream.foreachRDD { rdd => rdd.foreach { input => println(input) } }
ssc.start()
ssc.awaitTermination()
```
```sh
$ cat jaas.conf
KafkaClient{
    com.sun.security.auth.module.Krb5LoginModule required
    useKeyTab=true
    keyTab="./test.keytab"
    principal="x@X.Y.COM";
};
```
```sh
# Also systest.keytab file should be present in the working directory

cp /keytabs/test.keytab ./
export SPARK_KAFKA_VERSION=0.10
export KAFKA_OPTS="-Djava.security.auth.login.config=/temp/jaas.conf"
spark2-shell  --driver-java-options '-Djava.security.auth.login.config=./jaas.conf' \
              --conf 'spark.executor.extraJavaOptions=-Djava.security.auth.login.config=./jaas.conf' \
              --files "./jaas.conf,systest.keytab"
               
 ```
