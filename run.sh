#! /bin/bash

cd /app/

spark-submit    --class com.gorskimariusz.meetup.streaming.MeetupResponsesStreaming \
                target/scala-2.11/pw-bd-project-meetup-streaming.jar pw-bd-project elastic 60 20 0 /tmp/

