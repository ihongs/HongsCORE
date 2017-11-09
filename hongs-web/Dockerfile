# docker build -t hongs/hongs-web:0.4 .
# docker run -p 80:8080 hongs/hongs-web:0.4

FROM hongs/hongs-env:0.4

EXPOSE 8080

# Schedule tasks
ADD crontab    /var/spool/cron/crontabs/root
RUN chown root /var/spool/cron/crontabs/root
RUN chmod 600  /var/spool/cron/crontabs/root

ADD target/HongsWeb /opt/HongsWeb
RUN chmod +x /opt/HongsWeb/bin/*
RUN /opt/HongsWeb/bin/app system.setup

CMD ["/opt/HongsWeb/bin/start.sh"]
