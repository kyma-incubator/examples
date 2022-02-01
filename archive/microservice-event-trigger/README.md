# Trigger a microservice with an Event

## Overview 

This example shows how to set up an Event trigger for a microservice deployed in Kyma. It uses the [HTTP DB service](https://github.com/kyma-project/examples/tree/master/http-db-service) as a sample microservice and the [Commerce mock](https://github.com/SAP/xf-application-mocks/tree/master/commerce-mock) to mock the sending of an Event trigger. You may use your own microservices and Applications in their place. 

## Prerequisites

* Kyma as the target deployment environment 

## Installation

### Set up the commerce mock

1. Open the Kyma console and choose or create the Namespace where you want to deploy the example.
2. Click on **Deploy new resource to the namespace** and upload the [commerce mock](https://raw.githubusercontent.com/SAP/xf-application-mocks/master/commerce-mock/deployment/xf.yaml).
3. Go to **Integration** > **Application** > **Create Application** to create a new Application. In this example it is referred to as **{application}**.
4. Copy the URL.
5. Access the commerce mock at `https://commerce.{CLUSTER_DOMAIN}`.

Once you are in the commerce mock UI:

1. Connect the Application to the commerce mock.
2. Register `SAP Commerce Cloud - Events`. After registration, it appears under the **Remote APIs** tab.

### Expose Events from the Application to a Namespace in Kyma

After the Application registers the Events, you need to make them accessible to various serverless workloads running inside Kyma, such as lambdas and microservices. Since multiple applications can connect to Kyma, each Kyma Namespace is declaratively bound to an Application. The Events are then made accessible by adding the Event catalog to the Namespace.

1. Create a Namespace for the Application. In this example, the Namespace is called `demo-event-service-trigger`.
2. Bind the Application to the Namespace.
3. `SAP Commerce Cloud - Events` should be available in the Service Catalog under Services. Click on **Add once** to enable serverless workloads to consume Events.

### Deploy the service

1. Navigate to the `demo-event-service-trigger` Namespace.
2. Click on **Deploy new resource to the namespace** and upload the [HTTP DB service](https://raw.githubusercontent.com/kyma-project/examples/master/http-db-service/deployment/deployment.yaml). It exposes the `/events/order/created` endpoint to handle the **order.created** Event.

### Create the Kyma subscription

By creating a Kyma Subscription, you configure an **order.created** Event trigger for the microservice. This deploys whenever the Application sends an **order.created** Event. The Event gets delivered as an HTTP **POST** request to the endpoint of the microservice.

Create the [Kyma subscription](./assets/event-trigger-subscription.yaml) by either deploying the Kyma Subscription custom resource from the Console UI or using the kubectl command.

> **TIP:** Refer to [custom resource parameters](https://github.com/kyma-project/kyma/blob/master/docs/event-bus/06-01-subscription.md#custom-resource-parameters) for details on all parameters. 

The main parameter is **endpoint** which takes the value in the `http://{service-name}.{namespace-in-which-service-is-deployed}:{service-port}/{uri-path-to-handle-events}` format.

```yaml
apiVersion: eventing.kyma-project.io/v1alpha1
kind: Subscription
metadata:
  name: example-subscription
  labels:
    example: example-subscription
spec:
  endpoint: http://http-db-service.demo-event-service-trigger:8017/events/order/created
  include_subscription_name_header: true
  max_inflight: 10
  push_request_timeout_ms: 60
  event_type: order.created
  event_type_version: v1
  source_id: {application}
```

### Send an Event and verify it

1. Send the Event from the commerce mock UI. It is accessible at `https://commerce.{CLUSTER_DOMAIN}`.

    * Go to `SAP Commerce Cloud - Events` under **Remote APIs**. 
    * Select the **order.created.v1** Event from the dropdown and click on **Send Event**.
    
2. Go to **Diagnostics** > **Logs** and verify the logs.