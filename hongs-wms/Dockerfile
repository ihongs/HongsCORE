# docker build -t hongs/hongs-wms:4.0 .
# docker run hongs/hongs-wms:4.0

FROM hongs/hongs-jdk:8

EXPOSE 8080

ADD target/HongsWMS /opt/HongsWMS

RUN chmod +x /opt/HongsWMS/bin/*

RUN /opt/HongsWMS/bin/app system.setup

CMD /opt/HongsWMS/bin/app server.start --debug 1
