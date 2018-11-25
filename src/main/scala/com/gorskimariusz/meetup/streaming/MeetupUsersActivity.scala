package com.gorskimariusz.meetup.streaming

import java.io.FileInputStream

import com.gorskimariusz.meetup.Elasticsearch
import org.apache.spark.sql.{DataFrame, Column}
import org.apache.spark.sql.functions._
import com.google.protobuf.ByteString
import com.google.pubsub.v1.{ProjectTopicName, PubsubMessage}
import java.util.concurrent.TimeUnit

import com.google.api.gax.batching.BatchingSettings
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.pubsub.v1.Publisher
import org.threeten.bp.Duration

object MeetupUsersActivity {
  // Google Pub/Sub Credentials
  val credentials = GoogleCredentials.fromStream(new FileInputStream("./src/resources/gcp/key.json"))

  // Batch settings control how the publisher batches messages// Batch settings control how the publisher batches messages
  val requestBytesThreshold = 5000L // default : 1kb
  val messageCountBatchSize = 10L // default : 100
  val publishDelayThreshold = Duration.ofMillis(100) // default : 1 ms

  // Publish request get triggered based on request size, messages count & time since last publish
  val batchingSettings = BatchingSettings.
    newBuilder
    .setElementCountThreshold(messageCountBatchSize)
    .setRequestByteThreshold(requestBytesThreshold)
    .setDelayThreshold(publishDelayThreshold)
    .build

  def saveMostActive(input: DataFrame): Unit = {
    val users = input.select(col("member.member_name"), col("member.member_id"))
                  //.withColumn("member_name", explode(col("member_name")))
                  .groupBy(col("member_id"), col("member_name"))
                  .count()
                  .where("count > 1")
                  .orderBy(desc("count"))
                  .withColumn("timestamp", lit(from_unixtime(unix_timestamp(current_timestamp(), "yyyy-MM-dd HH:mm:ss.SSS"),"yyyy-MM-dd HH:mm:ss")))
                  .withColumn("dateForIndex", date_format(current_timestamp(), "y.M.d"))
                  .withColumn("position", monotonically_increasing_id() + 1)
                  .where("position <= 5")

    Elasticsearch.index(users, "meetup-rascals", Option("dateForIndex"))
  }

  def notifyAboutIntruder(input: DataFrame): Unit = {
    val colsToKeep = Seq("title", "message")

    val users = input.select(col("member.member_name"), col("event.event_name"), col("response"))
      .filter(col("member_name").contains("Mariusz Górski"))
      .withColumn("title", lit("Intruder detected!"))
      .withColumn("message", concat(
          lit("User > "),
          col("member_name"),
          lit(" < responded > "),
          upper(col("response")),
          lit(" < to event > "),
          col("event_name"),
          lit(" <")))
      .select(input.columns.filter(colsToKeep.contains(_)).map(new Column(_)): _*)
      .toJSON
      .take(999)

    // proceed only if are any alerts to publish
    if (users.length > 0) {
      val topicName = ProjectTopicName.of("pw-bd-project", "meetup-notify")

      val publisher = Publisher
        .newBuilder(topicName)
        .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
        .setBatchingSettings(batchingSettings)
        .build

      try {
        users.foreach(user => {
          println(user)
          val data = ByteString.copyFromUtf8(user)

          val pubsubMessage = PubsubMessage.newBuilder.setData(data).build
          val messageIdFuture = publisher.publish(pubsubMessage)
          println(messageIdFuture.get())
        })
      } finally if (publisher != null) {
        publisher.shutdown()
        publisher.awaitTermination(5, TimeUnit.SECONDS)
      }
    }
  }
}
