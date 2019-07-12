# Overview

A small script to quickly test sending events against a cluster

## Set Domain

```bash
export KYMA_DOMAIN="nightly.build.kyma-project.io"
export APP_NAME=test-ce-extension-headers
```

## Pair

```bash
./one-click-integration.sh -u <connector url>
```

## Register events

```bash
http POST https://gateway.${KYMA_DOMAIN}/${APP_NAME}/v1/metadata/services --cert=generated.pem --verify=no < register-events.json
```

## set up event trigger

* Create namespace
* bind application to namespace
* Add one for events in service catalog
* create a lambda with event trigger

## send the event

```bash
echo '{"event-type" : "order.created", "event-type-version" : "v1", "event-time" : "2018-05-02T22:08:41+00:00", "data" : {"orderCode" : "1234"}}'|http POST https://gateway.${KYMA_DOMAIN}/${APP_NAME}/v1/events Ce-Corr-Id:123 --cert=generated.pem --verify=no
```

## verify

check the lambda logs
