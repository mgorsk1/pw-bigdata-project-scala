package com.gorskimariusz.meetup
import org.apache.spark.sql.DataFrame
import org.elasticsearch.spark.sql._

import org.apache.spark.sql.functions.unix_timestamp
import org.apache.spark.sql.types.DoubleType

object Elasticsearch {
  def index(data: DataFrame, index: String, prefix: Option[String]): Unit = {
    val dataWithTimestamp = data.withColumn("timestamp", unix_timestamp().cast(DoubleType)*1000)

    var fullIndexName = ""

    if (prefix.isDefined) {
      val fullPrefix = prefix.get
      fullIndexName = s"$index-{$fullPrefix}/default"

      dataWithTimestamp.saveToEs(fullIndexName, Map("es.mapping.exclude" -> fullPrefix))
    }
    else {
      fullIndexName = s"$index/default"

      dataWithTimestamp.saveToEs(fullIndexName)
    }
  }
}
