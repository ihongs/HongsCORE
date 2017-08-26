# docker build -t hongs/hongs-wms:0.4 .
# docker run -p 80:8080 hongs/hongs-wms:0.4

FROM hongs/hongs-env:0.4

EXPOSE 8080

ADD target/HongsWMS /opt/HongsWMS

RUN chmod +x /opt/HongsWMS/bin/*

RUN /opt/HongsWMS/bin/app system.setup

CMD ["/opt/HongsWMS/bin/start.sh"]
