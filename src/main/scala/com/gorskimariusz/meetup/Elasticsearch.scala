package com.gorskimariusz.meetup
import org.apache.spark.sql.DataFrame
import org.elasticsearch.spark.sql._

object Elasticsearch {
  def index(data: DataFrame, index: String, prefix: Option[String]): Unit = {
    var fullIndexName = ""

    if (prefix.isDefined) {
      val fullPrefix = prefix.get
      fullIndexName = s"$index-{$fullPrefix}/default"
    }
    else {
      fullIndexName = s"$index/default"
    }

    data.saveToEs(fullIndexName)
  }
}
