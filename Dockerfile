FROM openjdk:8-jre-alpine

MAINTAINER dev@hiis.io

RUN mkdir -p /root/play-http-api/

ADD target/universal/stage /root/play-http-api/

WORKDIR /root/play-http-api

EXPOSE 9000

CMD ["/root/play-http-api/bin/play-http-api"]

# sbt clean stage
# docker build -t abanda/play-http-api .
# docker push abanda/play-http-api