package com.gorskimariusz.meetup.streaming

import com.gorskimariusz.meetup.Elasticsearch
import org.apache.spark.sql.{DataFrame}
import org.apache.spark.sql.functions._

object MeetupTopicsAnalysis {
  def saveMostPopular(input: DataFrame): Unit = {
    val topics = input.select(col("group.group_topics.topic_name"))
      .withColumn("topic", explode(col("topic_name")))
      .groupBy(col("topic"))
      .count()
      .orderBy(desc("count"))
      .withColumn("timestamp", lit(from_unixtime(unix_timestamp(current_timestamp(), "yyyy-MM-dd HH:mm:ss.SSS"),"yyyy-MM-dd HH:mm:ss")))
      .withColumn("dateForIndex", date_format(current_timestamp(), "y.M.d"))
      .withColumn("position", monotonically_increasing_id() + 1)
      .where("position <= 20")

    topics.show()

    Elasticsearch.index(topics, "meetup-topics", Option("dateForIndex"))
  }
}
