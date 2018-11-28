#! /bin/bash

cd /jupyter

# Start the jupyter notebook
nohup jupyter notebook --ip=0.0.0.0 --allow-root &

ls -lt
cd /app/

spark-submit    --class com.gorskimariusz.meetup.streaming.MeetupResponsesStreaming \
                --conf "es.nodes=elastic" \
                target/scala-2.11/pw-bd-project-meetup-streaming.jar pw-bd-project 60 20 20 /tmp/

