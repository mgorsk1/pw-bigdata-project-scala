package com.gorskimariusz.meetup.streaming

import com.gorskimariusz.meetup.Elasticsearch
import org.apache.spark.sql.{DataFrame}
import org.apache.spark.sql.functions._
import org.apache.spark.sql.expressions.Window

object MeetupTopicsAnalysis {
  def saveMostPopular(input: DataFrame): Unit = {
    val rankWindow = Window.orderBy(desc("count"))

    val topics = input
      .where("response = 'yes'")
      .select(col("group.group_topics.topic_name"))
      .withColumn("topic", explode(col("topic_name")))
      .groupBy(col("topic"))
      .count()
      .orderBy(desc("count"))
      .limit(10)
      .withColumn("dateForIndex", date_format(unix_timestamp(), "y.MM.dd"))
      .withColumn("position", dense_rank().over(rankWindow))

    Elasticsearch.index(topics, "meetup-agg-topics", Option("dateForIndex"))
  }
}
