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

object MeetupUsersAnalysis {
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
                  .groupBy(col("member_id"), col("member_name"))
                  .count()
                  .where("count > 3")
                  .orderBy(desc("count"))
                  .withColumn("dateForIndex", date_format(current_timestamp(), "y.MM.dd"))
                  .withColumn("position", monotonically_increasing_id() + 1)
                  .where("position <= 5")

    users.show()

    Elasticsearch.index(users, "meetup-agg-users", Option("dateForIndex"))
  }

  def notifyAboutSuspiciousUserActivity(input: DataFrame): Unit = {
    val colsToKeep = Seq("title", "message")

    val users = input.select(col("member.member_name"), col("member.member_id"))
      .withColumn("dateForIndex", date_format(current_timestamp(), "y.MM.dd"))
      .groupBy(col("member_name"), col("member_id"), col("dateForIndex"))
      .count()
      .where("count > 5")

    Elasticsearch.index(users, "meetup-agg-rascals", Option("dateForIndex"))

    val usersMessage = users
      .withColumn("title", lit("User in frenzy detected!"))
      .withColumn("message", concat(
          lit("User > "),
          col("member_id"),
          lit(" ("),
          col("member_name"),
          lit(") < sent > "),
          upper(col("count")),
          lit(" < rsvps in 20 seconds ! This is shady - please verify !")))
          .toJSON
          .take(999)

    // proceed only if are any alerts to publish
    if (usersMessage.length > 0) {
      val topicName = ProjectTopicName.of("pw-bd-project", "meetup-notify")

      val publisher = Publisher
        .newBuilder(topicName)
        .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
        .setBatchingSettings(batchingSettings)
        .build

      try {
        usersMessage.foreach(user => {
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
