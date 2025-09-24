FROM alpine:3

ARG VERSION

# Use application as the name to comply with legacy Micronaut native Dockerfile
COPY build/native/nativeCompile/kafkactl-$VERSION /app/application

RUN apk update \
    && echo -e "#!/bin/sh\n\n/app/application \"\$@\"" > /usr/bin/kafkactl \
    && chmod +x /usr/bin/kafkactl \
    && apk upgrade \
    && rm -rf /var/cache/apk/*

ENTRYPOINT ["/app/application"]