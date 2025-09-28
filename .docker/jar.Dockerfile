FROM eclipse-temurin:21-jre-alpine

# Use application.jar as the name to comply with legacy Micronaut Dockerfile
COPY build/libs/kafkactl-*.jar /home/app/application.jar

RUN apk update \
    && echo -e "#!/bin/sh\n\njava -jar /home/app/application.jar \"\$@\"" > /usr/bin/kafkactl \
    && chmod +x /usr/bin/kafkactl \
    && apk upgrade \
    && rm -rf /var/cache/apk/*

ENTRYPOINT ["kafkactl"]