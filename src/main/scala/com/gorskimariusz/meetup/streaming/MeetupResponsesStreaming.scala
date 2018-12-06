package com.gorskimariusz.meetup.streaming

import com.gorskimariusz.meetup.protobuf.meetup_rawdata.MSG

import org.apache.log4j.{Level, Logger}

import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.pubsub.{PubsubUtils, SparkGCPCredentials}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.sql.SparkSession

import scalapb.spark._

object MeetupResponsesStreaming {
  def createContext(projectID: String, elasticHost: String, windowLength: String, batchInterval: String, checkpointDirectory: String)
  : StreamingContext = {

    // setup streaming
    val spark = SparkSession
      .builder
      .appName("pw-bd-project-meetup")
      .master("local[2]")
      .config("es.index.auto.create", "true")
      .config("es.nodes", elasticHost)
      .config("es.port", "9200")
      .config("es.nodes.wan.only", "true")
      .getOrCreate()

    val sc = spark.sparkContext
    val ssc = new StreamingContext(sc, Seconds(batchInterval.toInt))

    ssc.checkpoint(checkpointDirectory)

    // Create stream
    val userResponses: DStream[MSG] = PubsubUtils
      .createStream(
        ssc,
        projectID,
        None,
        "meetup-rawdata-subscription-streaming",  // Cloud Pub/Sub subscription for incoming tweets
        SparkGCPCredentials.builder.jsonServiceAccount("./src/resources/gcp/key.json").build(), StorageLevel.MEMORY_AND_DISK_SER_2)
      .map(response => MSG.parseFrom(response.getData()))

    // elasticsearch aggregates
    val mostActiveUsers = userResponses.window(Seconds(60), Seconds(20))
    mostActiveUsers.foreachRDD(rdd => {
      val df = rdd.toDataFrame(spark)
      MeetupUsersAnalysis.saveMostActive(df)
    })

    val trendingTopics = userResponses.window(Seconds(60), Seconds(20))
    trendingTopics.foreachRDD(rdd => {
      val df = rdd.toDataFrame(spark)
      MeetupTopicsAnalysis.saveMostPopular(df)
    })

    // pushover notification
    val suspiciousUserActivity = userResponses.window(Seconds(20))
    suspiciousUserActivity.foreachRDD(rdd => {
      val df = rdd.toDataFrame(spark)
      MeetupUsersAnalysis.notifyAboutSuspiciousUserActivity(df)
    })

    ssc
  }

  def main(args: Array[String]): Unit = {
    if (args.length != 6) {
      System.err.println(
        """
          | Usage: MeetupResponsesStreaming <projectID> <elasticHost> <windowLength> <slidingInterval> <totalRunningTime>
          |
          |     <projectID>: ID of Google Cloud project
          |     <elasticHost>: URL of Elasticsearch host
          |     <windowLength>: The duration of the window, in seconds
          |     <batchInterval>: The interval at which the window calculation is performed, in seconds
          |     <totalRunningTime>: Total running time for the application, in minutes. If 0, runs indefinitely until termination.
          |     <checkpointDirectory>: Directory used to store RDD checkpoint data
          |
        """.stripMargin)
      System.exit(1)
    }

    val Seq(projectID, elasticHost, windowLength, slidingInterval, totalRunningTime, checkpointDirectory) = args.toSeq

    // Create Spark context
    val ssc = StreamingContext.getOrCreate(checkpointDirectory,
      () => createContext(projectID, elasticHost, windowLength, slidingInterval, checkpointDirectory))

    // Start streaming until we receive an explicit termination
    ssc.start()

    if (totalRunningTime.toInt == 0) {
      ssc.awaitTermination()
    }
    else {
      ssc.awaitTerminationOrTimeout(1000 * 60 * totalRunningTime.toInt)
    }
  }

  Logger.getLogger("org").setLevel(Level.OFF)
}
