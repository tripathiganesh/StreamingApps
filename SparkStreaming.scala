import kafka.serializer.StringDecoder
import org.apache.spark.SparkContext
import org.apache.spark.SparkConf
import org.apache.spark.streaming._
import org.apache.spark.streaming.kafka._

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.spark.HBaseContext;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HConnection;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

object SparkStreaming 
{
  
  def main(args:Array[String]): Unit = {
      
    for (arg <- args)
    {
      println(arg)
    }
    
    if (args.length == 0) {
      println("{brokerList} {topic}")
      return
    }

    val brokerList = args(0)
    val topic = args(1)
    
    val conf = new SparkConf().setAppName("Streaming")
    val sc = new SparkContext(conf)
    
    val ssc = new StreamingContext(sc, Seconds(2))
    
    val directKafkaStream = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder ](ssc, Map(("metadata.broker.list", brokerList)), Set(topic))

    
    val hConf = HBaseConfiguration.create();
    hConf.addResource(new Path("/etc/hbase/conf/core-site.xml"));
    hConf.addResource(new Path("/etc/hbase/conf/hbase-site.xml"));

    val hbaseContext = new HBaseContext(ssc, conf);

    directKafkaStream.foreachRDD(rdd => {
      rdd.foreachPartition(partitionOfRecords => {
          // ConnectionPool is a static, lazily initialized pool of connections
 /*         val connection = ConnectionPool.getConnection()
          partitionOfRecords.foreach(record => connection.send(record))
          ConnectionPool.returnConnection(connection)  // return to the pool for future reuse
*/
          print("partitionOfRecords: " + partitionOfRecords)
        })
    })
        
    // Start the computation
    ssc.start()
    ssc.awaitTermination()

  }

}
