# API Gateway V2 to EventBridge

This example creates an API Gateway V2 proxy integration with EventBridge and Lambda. It defines a single API Gateway
endpoint that publishes events to an EventBridge event bus, and an accompanying event rule that matches those events and
invokes a Lambda function.

## Prerequisites

[Follow the instructions](https://www.pulumi.com/docs/clouds/aws/get-started/begin/)
to get started with Pulumi & AWS.

### Deploying the App

1. Create a new stack:

   ```bash
   $ pulumi stack init dev
   ```

2. Specify an AWS region to deploy into:

    ```bash
    $ pulumi config set aws:region us-west-2
    ```

3. In a few moments, the API Gateway instance service will be up and running and its public URL emitted as a
   Pulumi [stack output](https://www.pulumi.com/docs/intro/concepts/stack/#outputs).

    ```bash
    ...
    Outputs:
        url: "https://andchh8hg8.execute-api.us-west-2.amazonaws.com/dev"
    ```

4. Verify the deployment with `curl` and `pulumi logs`:

    ```bash
    $ curl --data '{"some-key": "some-value"}' --header "Content-Type: application/json" "$(pulumi stack output url)/uploads"

    {"Entries":[{"EventId":"cdc44763-6976-286c-9378-7cce674dff81"}],"FailedEntryCount":0}
    ```

    ```bash
    $ pulumi logs --follow

    Collecting logs for stack dev since 2022-01-06T16:18:48.000-08:00.
    ...

    {
        source: 'my-event-source',
        detail: { 'some-key': 'some-value' }
    }
    ```

5. When you're ready, destroy your stack and remove it:

    ```bash
    pulumi destroy --yes
    pulumi stack rm --yes
    ```
