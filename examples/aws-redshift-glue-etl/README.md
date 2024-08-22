# ETL pipeline with Amazon Redshift and AWS Glue

This example creates an ETL pipeline using Amazon Redshift and AWS Glue. The pipeline extracts data from an S3 bucket
with a Glue crawler, transforms it with a Python script wrapped in a Glue job, and loads it into a Redshift database
deployed in a VPC.

## Prerequisites

[Follow the instructions](https://www.pulumi.com/docs/clouds/aws/get-started/begin/)
to get started with Pulumi & AWS.

## Deploying

1. Create a new stack, which is an isolated deployment target for this example:

   ```bash
   pulumi stack init dev
   ```

2. Set the AWS region:

   ```bash
   pulumi config set aws:region us-west-2
   ```

3. Stand up the cluster:

   ```bash
   pulumi up
   ```
4. In a few moments, the Redshift cluster and Glue components will be up and running and the S3 bucket name emitted as a
   Pulumi stack output:

5. Upload the included sample data file to S3 to verify the automation works as expected:

    ```bash
    aws s3 cp events-1.txt s3://$(pulumi stack output dataBucketName)
    ```

6. When you're ready, destroy your stack and remove it:

    ```bash
    pulumi destroy --yes
    pulumi stack rm --yes
    ```