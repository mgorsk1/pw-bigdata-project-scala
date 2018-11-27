#! /bin/bash

cd /jupyter

# Start the jupyter notebook
nohup jupyter notebook --ip=0.0.0.0 --allow-root &

ls -lt
cd /app/

sbt clean assembly

spark-submit    --class com.gorskimariusz.meetup.streaming.MeetupResponsesStreaming \
                target/scala-2.11/meetupstreaming-assembly-0.2.jar pw-bd-project 60 20 20 /tmp/

