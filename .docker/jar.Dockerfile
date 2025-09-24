FROM eclipse-temurin:21-jre-alpine

ARG VERSION

COPY build/libs/kafkactl-$VERSION.jar /home/app/application.jar

RUN apk update \
    && echo -e "#!/bin/sh\n\njava -jar /home/app/application.jar \"\$@\"" > /usr/bin/kafkactl \
    && chmod +x /usr/bin/kafkactl \
    && apk upgrade \
    && rm -rf /var/cache/apk/*

ENTRYPOINT ["java", "-jar", "/home/app/application.jar"]