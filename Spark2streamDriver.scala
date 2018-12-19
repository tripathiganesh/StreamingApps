package com.cloudera.spark2

import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.streaming.{OutputMode, ProcessingTime}
import scala.concurrent.duration._

object Spark2streamDriver {

  Logger.getRootLogger.setLevel(Level.OFF)
  Logger.getLogger("org").setLevel(Level.OFF)
  Logger.getLogger("akka").setLevel(Level.OFF)

  def main(args: Array[String]): Unit = {

    //Build new SparkSession entry point
    val spark = SparkSession.builder()
      .appName("Testing")
      //Schema inference is disabled by default in structured
      //streaming contexts - see http://bit.ly/2kcDaru
      .config("spark.sql.streaming.schemaInference", value = true)
      .master("local[4]")
      .getOrCreate()

    //Create streaming DF from file source
    //other options include Kafka and socket
    val streamDf = spark
      .readStream
      .option("header", "true")
      .csv("/path/to/resources/stream")

    //Create bog standard static DF
    val staticDf = spark
      .read
      .option("header", "true")
      .csv("/path/to/resources/static")

    //This is where the magic happens
    val joinedDf = streamDf.join(staticDf, "key")

    //Configure the output to console
    val stream = joinedDf
      .writeStream
      .trigger(ProcessingTime(1.seconds))
      .outputMode(OutputMode.Append())
      .format("console")
      .start() //"start" is the only real action on a structured stream
               //This is when the execution plan is locked in
               //Other actions (e.g. collect/show) are prohibited prior to start

    //Block long enough to get one output.
    stream.awaitTermination(5000)

  }

}
