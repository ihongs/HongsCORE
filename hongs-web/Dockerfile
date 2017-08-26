# docker build -t hongs/hongs-web:4.0 .
# docker run -p 80:8080 hongs/hongs-web:4.0

FROM hongs/hongs-env:4.0

EXPOSE 8080

ADD target/HongsWMS /opt/HongsWMS

RUN chmod +x /opt/HongsWeb/bin/*

RUN /opt/HongsWeb/bin/app system.setup

CMD nohup /opt/HongsWeb/bin/app server.start --debug 1 2>&1 > /opt/HongsWeb/var/log/run.out
