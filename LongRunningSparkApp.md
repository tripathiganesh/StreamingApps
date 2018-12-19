To run: 
```sh
spark-submit --master yarn-client --class com.cloudera.spark2.LongRunningApp spark-long-running-1.0-SNAPSHOT.jar /path/to/write/data /path/to/input/file /path/to/checkpoint/dir
```

This application generates longs from 0. Each micro-batch is then saved to files starting with "seriesData" in the directory represented by first argument (on HDFS).

To verify the data, the input file (on HDFS) contains all the data that was written to Spark.

The "input file" has all the data in the order it was created. Remember that the output may not be exactly in the same order as the input.

The "checkpoint dir" is the directory on HDFS where the app stores checkpoints and received, but yet to be processed data.


```scala
package com.cloudera.spark2

import java.io._
import java.util.concurrent.Executors

import org.apache.hadoop.fs.{PathFilter, FileStatus, FileSystem, Path}
import org.apache.spark.SparkConf
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.receiver.Receiver
import org.apache.spark.streaming.{Seconds, StreamingContext}

import scala.collection.mutable.HashSet

object LongRunningApp {


  def main(args: Array[String]) {

    require(args.length == 3)
    val dir = args(0)
    val inputFile = args(1)
    val checkpointDir = args(2)
    require(!dir.isEmpty)
    require(!inputFile.isEmpty)
    require(!checkpointDir.isEmpty)

    def createCheckpoint(): StreamingContext = {
      val sparkConf =
        new SparkConf()
          .setAppName("LongRunningApp")
          .set("spark.streaming.receiver.writeAheadLog.enable", "true")
      val ssc = new StreamingContext(sparkConf, Seconds(2))
      ssc.checkpoint(checkpointDir)
      val conf = new SerializableConfiguration(ssc.sparkContext.hadoopConfiguration)
      val dataGeneratorStream = ssc.receiverStream(new TimestampReceiver)
      dataGeneratorStream.repartition(5).saveAsTextFiles(s"$dir/seriesData")
      var lastRDDFirst: Long = -1l
      var lastRDDLast: Long = -1l
      var wasFirst = true

      dataGeneratorStream.foreachRDD { (rdd, time) =>
        // For the very first RDD generated, there is nothing to compare against, so skip the
        // comparisons
        if (!wasFirst) {
          val fs = FileSystem.get(conf.value)
          val rddOutputDir = {
            val files = fs.listStatus(new Path(dir))
            if (files.isEmpty) {
              throw new RuntimeException("No output dirs!")
            }
            files.reduceLeft { (dir1, dir2) =>
              if (dir1.getModificationTime < dir2.getModificationTime) dir1 else dir2
            }
          }.getPath
          val actualContentsFromLastRDD = new HashSet[Long]
          val rddOutputFiles = fs.listStatus(rddOutputDir)
          if (rddOutputFiles.isEmpty) {
            throw new RuntimeException("No output files!")
          }
          rddOutputFiles.foreach { rddOutputFile =>
            val inStream =
              new BufferedReader(new InputStreamReader(fs.open(rddOutputFile.getPath)))
            var nextLine = inStream.readLine()
            while (nextLine != null) {
              actualContentsFromLastRDD += nextLine.toLong
              nextLine = inStream.readLine()
            }
            inStream.close()
          }
          require((lastRDDFirst to lastRDDLast).forall { value =>
            actualContentsFromLastRDD.exists(_.toString == value.toString)
          }, "Content from last RDD missing on HDFS!")

          fs.delete(rddOutputDir, true)
        }
        wasFirst = false
        val collectedFromThisRDD = rdd.sortBy(t => t).collect()

        if (collectedFromThisRDD.isEmpty) {
          throw new RuntimeException("No data generated in this RDD")
        }

        lastRDDFirst = collectedFromThisRDD.head
        lastRDDLast = collectedFromThisRDD.last
      }
      ssc
    }

    val ssc = StreamingContext.getOrCreate(checkpointDir, createCheckpoint)

    sys.addShutdownHook(ssc.stop(stopSparkContext = true, stopGracefully = true))
    ssc.checkpoint(checkpointDir)
    ssc.start()
    ssc.awaitTermination()
  }

  private class TimestampReceiver
    extends Receiver[Long](StorageLevel.MEMORY_AND_DISK) {

    override def onStart(): Unit = {
      Executors.newSingleThreadExecutor().submit(new Runnable {
        var next = 0l
        override def run(): Unit = {
          while (true) {
            val inputs = (1 to 5).map(x => {
              next += 1
              next
            })
            store(inputs.iterator)
            Thread.sleep(10)
          }
        }
      })
    }

    override def onStop(): Unit = {
    }
  }
}
```

`SerializableConfiguration`

```scala
package com.cloudera.spark2

import java.io.{ObjectInputStream, ObjectOutputStream}

import org.apache.hadoop.conf.Configuration

class SerializableConfiguration(@transient var value: Configuration) extends Serializable {
  private def writeObject(out: ObjectOutputStream): Unit = {
    out.defaultWriteObject()
    value.write(out)
  }

  private def readObject(in: ObjectInputStream): Unit = {
    value = new Configuration(false)
    value.readFields(in)
  }
}
```
