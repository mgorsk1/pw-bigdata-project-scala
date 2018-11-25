FROM centos/python-35-centos7
MAINTAINER gorskimariusz13@gmail.com

LABEL "com.gorskimariusz.project"="pw-bd-project-spark"

USER root

RUN mkdir -p /jupyter/

WORKDIR /tmp/

COPY jupyter_notebook_config.py /opt/app-root/src/.jupyter/

RUN wget http://www-eu.apache.org/dist/spark/spark-2.3.1/spark-2.3.1-bin-hadoop2.7.tgz
RUN tar -xzf spark-2.3.1-bin-hadoop2.7.tgz
RUN mv spark-2.3.1-bin-hadoop2.7 /opt/spark-2.3.1
RUN ln -s /opt/spark-2.3.1 /opt/spark

RUN yum -y install java-1.8.0-openjdk-devel vim

ENV LD_LIBRARY_PATH /opt/rh/rh-python35/root/usr/lib64:/opt/rh/rh-nodejs6/root/usr/lib64:/opt/rh/httpd24/root/usr/lib64
ENV SPARK_HOME /opt/spark
ENV JAVA_HOME /usr/
ENV SPARK_SBIN_HOME /opt/spark/sbin
ENV SPARK_BIN_HOME /opt/spark/bin
ENV PYTHONPATH $SPARK_HOME/python:$SPARK_HOME/python/lib/py4j-0.10.6-src.zip
ENV PATH $SPARK_BIN_HOME:$SPARK_SBIN_HOME:$JAVA_HOME:$SPARK_HOME:$LD_LIBRARY_PATH:$PATH

RUN pip install --upgrade pip
RUN pip install pip-tools
RUN pip install jupyter
RUN pip install findspark
RUN pip install pyspark
RUN pip install kafka-python
RUN pip install faker
