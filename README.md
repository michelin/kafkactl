# Kafkactl 

<!-- [![GitHub release](https://img.shields.io/github/v/release/michelin/kafkactl?logo=github&style=for-the-badge)](https://github.com/michelin/kafkactl/releases)
[![GitHub commits since latest release (by SemVer)](https://img.shields.io/github/commits-since/michelin/kafkactl/latest?logo=github&style=for-the-badge)](https://github.com/michelin/kafkactl/commits/main)-->

[![GitHub Build](https://img.shields.io/github/actions/workflow/status/michelin/kafkactl/on_push_main.yml?branch=main&logo=github&style=for-the-badge)](https://img.shields.io/github/actions/workflow/status/michelin/kafkactl/on_push_main.yml)
[![GitHub Stars](https://img.shields.io/github/stars/michelin/kafkactl?logo=github&style=for-the-badge)](https://github.com/michelin/kafkactl)
[![GitHub Watch](https://img.shields.io/github/watchers/michelin/kafkactl?logo=github&style=for-the-badge)](https://github.com/michelin/kafkactl)
[![Docker Pulls](https://img.shields.io/docker/pulls/michelin/kafkactl?label=Pulls&logo=docker&style=for-the-badge)](https://hub.docker.com/r/michelin/kafkactl/tags)
[![Docker Stars](https://img.shields.io/docker/stars/michelin/kafkactl?label=Stars&logo=docker&style=for-the-badge)](https://hub.docker.com/r/michelin/kafkactl)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?logo=apache&style=for-the-badge)](https://opensource.org/licenses/Apache-2.0)

**Kafkactl** is the CLI linked with [Ns4Kafka](https://github.com/michelin/ns4kafka). It lets you deploy Kafka resources using YAML descriptors.

# Table of Contents

* [Download](#download)
* [Install](#install)
  * [Configuration file](#configuration-file)

# Download

Kafkactl can be downloaded at https://github.com/michelin/kafkactl/releases and is available in 3 different formats:
- JAR (Java 11 required)
- Windows
- Linux

# Install

Kafkactl requires 3 variables to work:
- The url of Ns4kafka
- Your namespace
- Your security token (e.g., a Gitlab token)
  
These variable can be defined in the dedicated configuration file.

## Configuration file

Create a folder .kafkactl in your home directory:

- Windows: C:\Users\Name\\.kafkactl
- Linux: ~/.kafkactl

Create .kafkactl/config.yml with the following content:

```yaml
kafkactl:
  contexts:
    - name: dev
      context:
        api: https://ns4kafka-dev-api.domain.com
        user-token: my_token
        namespace: my_namespace
    - name: prod
      context:
        api: https://ns4kafka-prod-api.domain.com
        user-token: my_token
        namespace: my_namespace
```

For each context, define your token and your namespace.

Check all your available contexts:

```command
kafkactl config get-contexts
```

Set yourself on a given context:

```command
kafkactl config use-context dev
```

Check your current context:

```command
kafkactl config current-context
```
