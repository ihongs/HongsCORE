# docker build -t hongs/hongs-env:0.4 .
# docker run hongs/hongs-env:0.4

FROM ubuntu:16.04

MAINTAINER Kevin Hongs <kevin.hongs@gmail.com>

ADD apt-sources.list /etc/apt/sources.list
ADD supervisord.conf /etc/supervisord.conf
ADD jdk* /opt/
RUN apt-get -y update  &&\
    apt-get -y install language-pack-en* &&\
    apt-get -y install language-pack-zh* &&\
    apt-get -y install supervisor &&\
    apt-get -y install cron &&\
    touch /var/log/cron.log &&\
    mv /opt/jdk* /opt/jdk
ENV JAVA_HOME    /opt/jdk
ENV LANG      en_US.UTF-8
ENV PATH $PATH:$JAVA_HOME/bin
