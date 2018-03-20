# docker build -t hongs/hongs-web:0.4 .
# docker run -p 80:8080 hongs/hongs-web:0.4

FROM hongs/hongs-env:0.4

EXPOSE 8080

ADD target/HongsWeb /opt/HongsWeb
ADD env/supervisord.conf /etc/supervisord.conf
ADD env/crontabs /var/spool/cron/crontabs/root
RUN chmod 600    /var/spool/cron/crontabs/root &&\
    chmod 755 -R /opt/HongsWeb/bin &&\
    chmod 777 -R /opt/HongsWeb/var &&\
    /opt/HongsWeb/bin/app system.setup --debug 13

CMD ["/usr/bin/supervisord"]
