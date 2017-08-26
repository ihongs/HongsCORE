# docker build -t hongs/hongs-wms:4.0 .
# docker run -p 80:8080 hongs/hongs-wms:4.0

FROM hongs/hongs-env:4.0

EXPOSE 8080

ADD target/HongsWMS /opt/HongsWMS

RUN chmod +x /opt/HongsWMS/bin/*

RUN /opt/HongsWMS/bin/app system.setup

CMD rm -f /opt/HongsWMS/var/server/8080.pid && nohup /opt/HongsWMS/bin/app server.start --debug 1 2>&1 > /opt/HongsWMS/var/log/run.out &
