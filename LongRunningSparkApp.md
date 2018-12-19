To run: 
```sh
spark-submit --master yarn-client --class com.cloudera.spark2.LongRunningApp spark-long-running-1.0-SNAPSHOT.jar /path/to/write/data /path/to/input/file /path/to/checkpoint/dir
```

This application generates longs from 0. Each micro-batch is then saved to files starting with "seriesData" in the directory represented by first argument (on HDFS).

To verify the data, the input file (on HDFS) contains all the data that was written to Spark.

The "input file" has all the data in the order it was created. Remember that the output may not be exactly in the same order as the input.

The "checkpoint dir" is the directory on HDFS where the app stores checkpoints and received, but yet to be processed data.
