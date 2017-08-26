# docker build -t hongs/hongs-wms:4.0 .
# docker run -p 80:8080 hongs/hongs-wms:4.0

FROM hongs/hongs-env:4.0

EXPOSE 8080

ADD target/HongsWMS /opt/HongsWMS

RUN chmod +x /opt/HongsWMS/bin/*

RUN /opt/HongsWMS/bin/app system.setup

CMD /opt/HongsWMS/bin/app server.start --debug 1
