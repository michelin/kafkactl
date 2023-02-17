# Kafkactl 

[![GitHub Build](https://img.shields.io/github/actions/workflow/status/michelin/kafkactl/on_push_main.yml?branch=main&logo=github&style=for-the-badge)](https://img.shields.io/github/actions/workflow/status/michelin/kafkactl/on_push_main.yml)
[![GitHub release](https://img.shields.io/github/v/release/michelin/kafkactl?logo=github&style=for-the-badge)](https://github.com/michelin/kafkactl/releases)
[![GitHub commits since latest release (by SemVer)](https://img.shields.io/github/commits-since/michelin/kafkactl/latest?logo=github&style=for-the-badge)](https://github.com/michelin/kafkactl/commits/main)
[![GitHub Stars](https://img.shields.io/github/stars/michelin/kafkactl?logo=github&style=for-the-badge)](https://github.com/michelin/kafkactl)
[![GitHub Watch](https://img.shields.io/github/watchers/michelin/kafkactl?logo=github&style=for-the-badge)](https://github.com/michelin/kafkactl)
[![Docker Pulls](https://img.shields.io/docker/pulls/michelin/kafkactl?label=Pulls&logo=docker&style=for-the-badge)](https://hub.docker.com/r/michelin/kafkactl/tags)
[![Docker Stars](https://img.shields.io/docker/stars/michelin/kafkactl?label=Stars&logo=docker&style=for-the-badge)](https://hub.docker.com/r/michelin/kafkactl)
[![SonarCloud Coverage](https://img.shields.io/sonar/coverage/michelin_kafkactl?logo=sonarcloud&server=https%3A%2F%2Fsonarcloud.io&style=for-the-badge)](https://sonarcloud.io/component_measures?id=michelin_kafkactl&metric=coverage&view=list)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?logo=apache&style=for-the-badge)](https://opensource.org/licenses/Apache-2.0)

**Kafkactl** is the CLI linked with [Ns4Kafka](https://github.com/michelin/ns4kafka). It lets you deploy your Kafka resources using YAML descriptors.

# Table of Contents

* [Download](#download)
* [Install](#install)
* [Usage](#usage)
  * [Config](#config)
  * [Apply](#apply)
  * [Delete](#delete)
  * [Get](#get)
  * [Api Resources](#api-resources)
  * [Diff](#diff)
  * [Import](#import)
  * [Reset Offsets](#reset-offsets)
  * [Delete Records](#delete-records)
  * [Connectors](#connectors)
  * [Reset Password](#reset-password)
* [Resources](#resources)
  * [User](#user)
  * [Administrator](#administrator)
* [CI/CD](#cicd)
  
# Download

Kafkactl can be downloaded at https://github.com/michelin/kafkactl/releases and is available in 3 different formats:
- JAR (Java 11 required)
- Windows
- Linux

A Docker image is available at [https://hub.docker.com/repository/docker/michelin/kafkactl](https://hub.docker.com/repository/docker/michelin/kafkactl).

# Install

Kafkactl requires 3 variables to work:
- The url of Ns4Kafka
- Your namespace
- Your security token (e.g., a Gitlab token)
  
These variable can be defined in the dedicated configuration file.

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

# Usage

```console
Usage: kafkactl [-hvV] [-n=<optionalNamespace>] [COMMAND]

Description:

These are common Kafkactl commands.

Options:
  -h, --help      Show this help message and exit.
  -n, --namespace=<optionalNamespace>
                  Override namespace defined in config or YAML resources.
  -v, --verbose   Enable the verbose mode.
  -V, --version   Print version information and exit.

Commands:
  api-resources     Print the supported API resources on the server.
  apply             Create or update a resource.
  config            Manage configuration.
  connect-clusters  Interact with connect clusters.
  connectors        Interact with connectors.
  delete-records    Delete all records within a topic.
  delete            Delete a resource.
  diff              Get differences between a new resource and a old resource.
  get               Get resources by resource type for the current namespace.
  import            Import non-synchronized resources.
  reset-offsets     Reset consumer group offsets.
  schemas           Interact with schemas.
  reset-password    Reset a Kafka password.
```

## Config

This command allows you to manage your Kafka contexts.

```console
Usage: kafkactl config [-hvV] [-n=<optionalNamespace>] <action> <context>

Description:

Manage configuration.

Parameters:
      <action>    Action to perform (get-contexts, current-context, use-context).
      <context>   Context to use.

Options:
  -h, --help      Show this help message and exit.
  -n, --namespace=<optionalNamespace>
                  Override namespace defined in config or YAML resources.
  -v, --verbose   Enable the verbose mode.
  -V, --version   Print version information and exit.
```

## Apply

This command allows you to deploy a resource.

```console
Usage: kafkactl apply [-hRvV] [--dry-run] [-f=<file>] [-n=<optionalNamespace>]

Description:

Create or update a resource.

Options:
      --dry-run       Does not persist resources. Validate only.
  -f, --file=<file>   YAML file or directory containing resources.
  -h, --help          Show this help message and exit.
  -n, --namespace=<optionalNamespace>
                      Override namespace defined in config or YAML resources.
  -R, --recursive     Search file recursively.
  -v, --verbose       Enable the verbose mode.
  -V, --version       Print version information and exit.
```

Resources have to be described in yaml manifests.

## Delete

This command allows you to delete a resource.

Deleting a resource is permanent and instantaneous. There is no coming back after deleting it.
- if the topic contained data, this data is lost.
- if the ACL was associated to live/running user, the user will instantly lose access to the resource.

```console
Usage: kafkactl delete [-hvV] [--dry-run] [-n=<optionalNamespace>] ([<resourceType> <name>] | [[-f=<file>] [-R]])

Description:

Delete a resource.

Parameters:
      <resourceType>   Resource type.
      <name>           Resource name.

Options:
      --dry-run        Does not persist operation. Validate only.
  -f, --file=<file>    YAML file or directory containing resources.
  -h, --help           Show this help message and exit.
  -n, --namespace=<optionalNamespace>
                       Override namespace defined in config or YAML resources.
  -R, --recursive      Search file recursively.
  -v, --verbose        Enable the verbose mode.
  -V, --version        Print version information and exit.
```

## Get

This command allows you to consult one or multiple resources.

```console
Usage: kafkactl get [-hvV] [-n=<optionalNamespace>] [-o=<output>] <resourceType> [<resourceName>]

Description:

Get resources by resource type for the current namespace.

Parameters:
      <resourceType>      Resource type or 'all' to display resources of all types.
      [<resourceName>]    Resource name.

Options:
  -h, --help              Show this help message and exit.
  -n, --namespace=<optionalNamespace>
                          Override namespace defined in config or YAML resources.
  -o, --output=<output>   Output format. One of: yaml|table
  -v, --verbose           Enable the verbose mode.
  -V, --version           Print version information and exit.
```

- **resourceType** is one of the managed resources: topic, connector, acl, schema, stream or **all** to fetch all the resources.
- **resourceName** is the name of the resource to consult.

## Api Resources 

This command allows you to consult which resources can be access.

```console
Usage: kafkactl api-resources [-hvV] [-n=<optionalNamespace>]

Description:

Print the supported API resources on the server.

Options:
  -h, --help      Show this help message and exit.
  -n, --namespace=<optionalNamespace>
                  Override namespace defined in config or YAML resources.
  -v, --verbose   Enable the verbose mode.
  -V, --version   Print version information and exit.
```

## Diff

This command allows you to check differences between a new yaml descriptor and the current one deployed in Ns4Kafka.

```console
Usage: kafkactl diff [-hRvV] [-f=<file>] [-n=<optionalNamespace>]

Description:

Get differences between a new resource and a old resource.

Options:
  -f, --file=<file>   YAML file or directory containing resources.
  -h, --help          Show this help message and exit.
  -n, --namespace=<optionalNamespace>
                      Override namespace defined in config or YAML resources.
  -R, --recursive     Search file recursively.
  -v, --verbose       Enable the verbose mode.
  -V, --version       Print version information and exit.
```

## Import

This command allows you to import unsynchronized resources between Ns4Kafka, and the Kafka broker/Kafka Connect cluster.

```console
Usage: kafkactl import [-hvV] [--dry-run] [-n=<optionalNamespace>] <resourceType>

Description:

Import non-synchronized resources.

Parameters:
      <resourceType>   Resource type.

Options:
      --dry-run        Does not persist resources. Validate only.
  -h, --help           Show this help message and exit.
  -n, --namespace=<optionalNamespace>
                       Override namespace defined in config or YAML resources.
  -v, --verbose        Enable the verbose mode.
  -V, --version        Print version information and exit.
```

- **resourceType** can be topic or connector

## Reset Offsets

This command allows you to reset the offsets of consumer groups and topics.

```console
Usage: kafkactl reset-offsets [-hvV] [--dry-run] --group=<group> [-n=<optionalNamespace>] (--topic=<topic> | --all-topics) (--to-earliest | --to-latest |
                        --to-datetime=<datetime> | --shift-by=<shiftBy> | --by-duration=<duration> | --to-offset=<offset>)

Description:

Reset consumer group offsets.

Options:
      --all-topics           All topics.
      --by-duration=<duration>
                             Shift offset by a duration format (PnDTnHnMnS).
      --dry-run              Does not persist resources. Validate only.
      --group=<group>        Consumer group name.
  -h, --help                 Show this help message and exit.
  -n, --namespace=<optionalNamespace>
                             Override namespace defined in config or YAML resources.
      --shift-by=<shiftBy>   Shift offset by a number. Negative to reprocess or positive to skip.
      --to-datetime=<datetime>
                             Set offset to a specific ISO8601 date time with time zone (yyyy-MM-ddTHH:mm:ssZ).
      --to-earliest          Set offset to its earliest value (reprocess all).
      --to-latest            Set offset to its latest value (skip all).
      --to-offset=<offset>   Set offset to a specific index.
      --topic=<topic>        Topic name or topic:partition.
  -v, --verbose              Enable the verbose mode.
  -V, --version              Print version information and exit.
```

- **--group** is one of your consumer group to reset.
- **--topic/--all-topics** is a given topic or all the topics to reset.
- **method** can be: --to-earliest, --to-latest, --to-offset, --to-datetime, --shift-by

## Delete Records

This command allows you to delete all records within "delete" typed topics.

```console
Usage: kafkactl delete-records [-hvV] [--dry-run] [-n=<optionalNamespace>] <topic>

Description:

Delete all records within a topic.

Parameters:
      <topic>     Name of the topic.

Options:
      --dry-run   Does not persist resources. Validate only.
  -h, --help      Show this help message and exit.
  -n, --namespace=<optionalNamespace>
                  Override namespace defined in config or YAML resources.
  -v, --verbose   Enable the verbose mode.
  -V, --version   Print version information and exit.
```

## Connectors

This command allows you to interact with connectors.

```console
Usage: kafkactl connectors [-hvV] [-n=<optionalNamespace>] <action> <connectors>...

Description:

Interact with connectors.

Parameters:
      <action>          Action to perform (pause, resume, restart).
      <connectors>...   Connector names separated by space or "all" for all connectors.

Options:
  -h, --help            Show this help message and exit.
  -n, --namespace=<optionalNamespace>
                        Override namespace defined in config or YAML resources.
  -v, --verbose         Enable the verbose mode.
  -V, --version         Print version information and exit.
```

- **action** can be pause, resume, restart
- **connectors** is a list of connector names separated by space.

## Schemas

This command allows you to modify schema compatibility.

```console
Usage: kafkactl schemas [-hvV] [-n=<optionalNamespace>] <compatibility> <subjects>...

Description:

Interact with schemas.

Parameters:
      <compatibility>   Compatibility to set (global, backward, backward-transitive, forward, forward-transitive, full, full-transitive, none).
      <subjects>...     Subject names separated by space.

Options:
  -h, --help            Show this help message and exit.
  -n, --namespace=<optionalNamespace>
                        Override namespace defined in config or YAML resources.
  -v, --verbose         Enable the verbose mode.
  -V, --version         Print version information and exit.
```

- **compatibility** is the compatibility mode to apply.
- **subject** is the subject to update the compatibility.

## Reset Password

This command allows you to reset the password of a user.

```console
Usage: kafkactl reset-password [-hvV] [--execute] [-n=<optionalNamespace>] [-o=<output>] <user>

Description:

Reset a Kafka password.

Parameters:
      <user>              The user to reset password.

Options:
      --execute           This option is mandatory to change the password
  -h, --help              Show this help message and exit.
  -n, --namespace=<optionalNamespace>
                          Override namespace defined in config or YAML resources.
  -o, --output=<output>   Output format. One of: yaml|table
  -v, --verbose           Enable the verbose mode.
  -V, --version           Print version information and exit.
```

# Resources

## User

This is the list of resources a simple Ns4Kafka user can manage.

### Topic

```yaml
---
apiVersion: v1
kind: Topic
metadata:
  name: myPrefix.topic
spec:
  replicationFactor: 3
  partitions: 3
  configs:
    min.insync.replicas: '2'
    cleanup.policy: delete
    retention.ms: '60000'
```

- **metadata.name** must be part of your allowed ACLs. Visit your namespace ACLs to understand which topics you are allowed to manage.
- **spec** properties and more importantly **spec.config** properties validation dependend on the topic validation rules associated to your namespace.
- **spec.replicationFactor** and **spec.partitions** are immutable. They cannot be modified once the topic is created.

### ACL

In order to provide access to your topics to another namespace, you can add an ACL using the following example, where "daaagbl0" is your namespace and "dbbbgbl0" the namespace that needs access your topics.


```yaml
---
apiVersion: v1
kind: AccessControlEntry
metadata:
  name: acl-topic-a-b
  namespace: daaagbl0
spec:
  resourceType: TOPIC
  resource: aaa.
  resourcePatternType: PREFIXED
  permission: READ
  grantedTo: dbbbgbl0
```

- **spec.resourceType** can be TOPIC, GROUP, CONNECT, CONNECT_CLUSTER.
- **spec.resourcePatternType** can be PREFIXED, LITERAL.
- **spec.permission** can be READ, WRITE.
- **spec.grantedTo** must reference a namespace on the same Kafka cluster as yours.
- **spec.resource** must reference any “sub-resource” that you are owner of. For example, if you are owner of the prefix “aaa”, you can grant READ or WRITE access such as:
  - the whole prefix: “aaa” 
  - a sub prefix: “aaa_subprefix”
  - a literal topic name: “aaa_myTopic”

### Connector

```yaml
---
apiVersion: v1
kind: Connector
metadata:
  name: myPrefix.myConnector
spec:
  connectCluster: myConnectCluster
  config:
    connector.class: myConnectorClass
    tasks.max: '1'
    topics: myPrefix.myTopic
    file: /tmp/output.out
    consumer.override.sasl.jaas.config: o.a.k.s.s.ScramLoginModule required username="<user>" password="<password>";
```

- **spec.connectCluster** must refer to one of the Kafka Connect clusters authorized to your namespace. It can also refer to a Kafka Connect cluster that you self deployed or you have been granted access.
- Everything else depend on the connect validation rules associated to your namespace.

### Connect Cluster

This resource declares a Connect cluster that has been self-deployed, so namespaces are autonomous to deploy connectors on it without any Ns4Kafka outage.

```yaml
---
apiVersion: v1
kind: ConnectCluster
metadata:
  name: myPrefix.myConnectCluster
spec:
  url: http://localhost:8083
  username: myUsername
  password: myPassword
```

- **metadata.name** should not collide with the name of a Connect cluster declared in the Ns4Kafka configuration. An error message will be thrown otherwise.
- Owners of Connect clusters can authorize other namespaces to deploy connectors on their own Connect clusters by giving an ACL with the WRITE permission to the grantees.

### Kafka Streams

This resource grants the necessary ACLs for your Kafka Streams to work properly if you have internal topics.

```yaml
---
apiVersion: v1
kind: KafkaStream
metadata:
  name: myKafkaStreamsApplicationId
```

- **metadata.name** must correspond to your Kafka Streams **application.id**.

### Schema

Subjects can be declared by referencing a local _avsc_ file with **spec.schemaFile** or directly inline with **spec.schema**.

#### Local file

```yml
---
apiVersion: v1
kind: Schema
metadata:
  name: myPrefix.topic-value # your subject name
spec:
  schemaFile: schemas/topic.avsc # relative to kafkactl binary
```

#### Inline

```yml
---
apiVersion: v1
kind: Schema
metadata:
  name: myPrefix.topic-value
spec:
  schema: |
    {
      "type": "long"
    }
```

#### Reference

If your schema references a type which is already stored in the Schema Registry, you can do this:

```yml
---
apiVersion: v1
kind: Schema
metadata:
  name: myPrefix.topic-value
spec: 
  schema: |
    {
      "type": "record",
      "namespace": "com.schema.avro",
      "name": "Client",
      "fields": [
        {
          "name": "name",
          "type": "string"
        },
        {
          "name": "address",
          "type": "com.schema.avro.Address"
        }
      ]
    }
  references:
    - name: com.schema.avro.Address
      subject: commons.address-value
      version: 1
```

This example assumes there is a subject named "commons.address-value" with a version 1 already available in the Schema Registry.

Your schemas ACLs are the same as your topics ACLs.
If you are allowed to create a topic "myPrefix.topic", then you are automatically allowed to create subject myPrefix.topic-key and myPrefix.topic-value.

## Administrator

Here is the list of resources a Ns4Kafka administrator can manage.

### Namespace

Namespace resources are the core of Ns4Kafka.

```yml
---
apiVersion: v1
kind: Namespace
metadata:
  name: myNamespace
  cluster: myCluster
  labels:
    contacts: namespace.owner@mail.com
spec:
  kafkaUser: kafkaServiceAccount
  connectClusters: 
    - myConnectCluster
  topicValidator:
    validationConstraints:
      partitions:
        validation-type: Range
        min: 1
        max: 6
      replication.factor:
        validation-type: Range
        min: 3
        max: 3
      min.insync.replicas:
        validation-type: Range
        min: 2
        max: 2
      retention.ms:
        optional: true
        validation-type: Range
        min: 60000
        max: 604800000
      cleanup.policy:
        validation-type: ValidList
        validStrings:
        - delete
        - compact
  connectValidator:
    validationConstraints:
      key.converter:
        validation-type: NonEmptyString
      value.converter:
        validation-type: NonEmptyString
      connector.class:
        validation-type: ValidString
        validStrings:
          - io.confluent.connect.jdbc.JdbcSinkConnector
          - io.confluent.connect.jdbc.JdbcSourceConnector
    sourceValidationConstraints:
      producer.override.sasl.jaas.config:
        validation-type: NonEmptyString
    sinkValidationConstraints:
      consumer.override.sasl.jaas.config:
        validation-type: NonEmptyString
    classValidationConstraints:
      io.confluent.connect.jdbc.JdbcSourceConnector:
        db.timezone:
          validation-type: NonEmptyString
      io.confluent.connect.jdbc.JdbcSinkConnector:
        db.timezone:
          validation-type: NonEmptyString
```

- **metadata.cluster** is the name of the Kakfa cluster. It should refer a cluster defined in the Ns4Kafka configuration.
- **spec.kafkaUser** is the Kafka principal. It should refer to an Account ID. It will be used to create ACLs on this service account.
- **spec.connectClusters** is the list of Kafka Connect clusters. It should refer to a Kafka Connect cluster declared in the Ns4Kafka configuration
- **spec.topicValidator** is the list of constraints for topics.
- **spec.connectValidator** is the list of constraints for connectors.

### ACL Owner

ACLs with owner permission can only be deployed by administrators.

  
```yml
---
apiVersion: v1
kind: AccessControlEntry
metadata:
  name: acl-topic-myNamespace
  namespace: myNamespace
spec:
  resourceType: TOPIC
  resource: myPrefix.
  resourcePatternType: PREFIXED
  permission: OWNER
  grantedTo: myNamespace
```

- With this ACL, the namespace "myNamespace" will be owner of topics prefixed by "myPrefix.". No one else is able to modify these resources.
- **resourceType** can be topic, connect or group.

### Role Binding

This resource links a namespace to a project team.

```yaml
---
apiVersion: v1
kind: RoleBinding
metadata:
  name: rb-myNamespace
  namespace: myNamespace
spec:
  role:
    resourceTypes:
    - schemas
    - schemas/config
    - topics
    - topics/delete-records
    - connectors
    - connectors/change-state
    - acls
    - consumer-groups/reset
    - streams
    verbs:
    - GET
    - POST
    - PUT
    - DELETE
  subject:
    subjectType: GROUP
    subjectName: myGitLabGroup
```

- With this role binding, members of the group "myGitLabGroup" can use Ns4Kafka to manage topics starting with "myPrefix." on the "myCluster" Kafka cluster.  

### Quota

It is possible to define quotas on a namespace. 

```yml
apiVersion: v1
kind: ResourceQuota
metadata:
  name: quota-myNamespace
  namespace: myNamespace
spec:
  count/topics: 10
  count/partitions: 60
  count/connectors: 5
  disk/topics: 500MiB
```

- **count/topics** is the maximum number of deployable topics
- **count/partitions** is the maximum number of deployable partitions
- **count/connectors** is the maximum number of deployable connectors
- **disk/topics** is the maximum size of all topics. It is computed from the sum of _retention.bytes_ * _number of partitions_ of all topics. 
Unit of measure accepted is byte (B), kibibyte (KiB), mebibyte (MiB), gibibyte (GiB)

# CI/CD

Kafkactl can be run in CI/CD using the [Docker image](https://hub.docker.com/repository/docker/michelin/kafkactl).

```yaml
kafkactl:
  stage: kafkactl
  image:
    name: michelin/kafkactl:<version>
    entrypoint: ['/bin/sh', '-c']
  before_script:
    - export KAFKACTL_CURRENT_NAMESPACE=test
    - export KAFKACTL_API=http://ns4kafka-dev-api.domain.com
    - export KAFKACTL_USER_TOKEN=${GITLAB_TOKEN}
  script:
    - java -jar /home/app/application.jar get all
```

- **KAFKACTL_CURRENT_NAMESPACE** is the namespace to use.
- **KAFKACTL_API** is the URL of Ns4Kafka in which to deploy
- **KAFKACTL_USER_TOKEN** is a CI/CD variable that contains the GitLab token.
