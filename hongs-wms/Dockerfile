# docker build -t hongs/hongs-wms:0.4 .
# docker run -p 80:8080 hongs/hongs-wms:0.4

FROM ubuntu:16.04

MAINTAINER Kevin_Hongs kevin.hongs@gmail.com

EXPOSE 8080

ENV LANG en_US.UTF-8
ENV JAVA_HOME /opt/jdk
ENV PATH $PATH:$JAVA_HOME/bin

ADD evn/supervisord.conf /etc/supervisord.conf
ADD env/apt-sources.list /etc/apt/sources.list
ADD env/spo-crontab.list /var/spool/cron/crontabs/root
ADD env/jdk*        /opt/
ADD target/HongsWMS /opt/

RUN apt-get -y update &&\
    apt-get -y install language-pack-en* &&\
    apt-get -y install language-pack-zh* &&\
    apt-get -y install supervisor  &&\
    apt-get -y install cron &&\
    touch /var/log/cron.log &&\
    mv    /opt/jd* /opt/jdk &&\
    chmod 600    /var/spool/cron/crontabs/root &&\
    chmod 755 -R /opt/HongsWMS/bin &&\
    chmod 777 -R /opt/HongsWMS/var &&\
    /opt/HongsWMS/bin/hdo system.setup --DEBUG 13

CMD ["/usr/bin/supervisord"]
