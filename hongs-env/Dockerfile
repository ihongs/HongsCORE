# docker build -t hongs/hongs-env:0.4 .
# docker run hongs/hongs-env:0.4

FROM ubuntu:16.04

MAINTAINER Kevin Hongs <kevin.hongs@gmail.com>

ADD sources.list /etc/apt/
RUN apt-get -y update
RUN apt-get -y install language-pack-en*
RUN apt-get -y install language-pack-zh*
ENV LANG en_US.UTF-8

RUN touch /var/log/cron.log

ADD jdk* /opt/
RUN  mv  /opt/jdk* /opt/jdk
ENV      JAVA_HOME /opt/jdk
ENV PATH $PATH:$JAVA_HOME/bin
