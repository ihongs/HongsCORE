# docker build -t hongs/hongs-wms:0.4 .
# docker run -p 80:8080 hongs/hongs-wms:0.4

FROM hongs/hongs-env:0.4

EXPOSE 8080

ADD target/HongsWMS /opt/HongsWMS
ADD env/supervisord.conf /etc/supervisord.conf
ADD env/crontabs /var/spool/cron/crontabs/root
RUN chmod 600    /var/spool/cron/crontabs/root &&\
    chmod 755 -R /opt/HongsWMS/bin &&\
    chmod 777 -R /opt/HongsWMS/var &&\
    /opt/HongsWMS/bin/app system.setup --debug 13

CMD ["/usr/bin/supervisord"]
