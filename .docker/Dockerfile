FROM ubuntu:24.04

ARG TARGETOS
ARG TARGETARCH

# Use application as the name to comply with legacy Micronaut native Dockerfile
COPY build/native/nativeCompile/kafkactl-$TARGETOS-$TARGETARCH/kafkactl-* /home/app/application

RUN apt-get update \
    && echo -e "#!/bin/sh\n\n/home/app/application \"\$@\"" > /usr/bin/kafkactl \
    && chmod +x /usr/bin/kafkactl /home/app/application \
    && apt-get upgrade -y

ENTRYPOINT ["kafkactl"]