# docker build -t hongs/hongs-web:4.0 .
# docker run hongs/hongs-web:4.0

FROM hongs/hongs-jdk:8

EXPOSE 8080

ADD target/HongsWMS /opt/HongsWMS

RUN chmod +x /opt/HongsWeb/bin/*

RUN /opt/HongsWeb/bin/app system.setup

CMD /opt/HongsWeb/bin/app server.start --debug 1
