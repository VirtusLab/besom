# Host a Static Website on Amazon S3 with the AWS Provider

A static website that uses [S3's website support](https://docs.aws.amazon.com/AmazonS3/latest/dev/WebsiteHosting.html).

## Prerequisites

[Follow the instructions](https://www.pulumi.com/docs/clouds/aws/get-started/begin/)
to get started with Pulumi & AWS.

## Deploying and running the program

Note: some values in this example will be different from run to run. 
These values are indicated with `***`.

1. Create a new stack, which is an isolated deployment target for this example:

    ```bash
    pulumi stack init aws-s3-folder-dev
    ```

2. Set the AWS region:

    ```bash
    pulumi config set aws:region us-west-2
    ```

3. Run `pulumi up` to preview and deploy changes. After the preview is shown 
you will be prompted if you want to continue or not.

    ```bash
    pulumi up
    ```
    ```
    Previewing update (dev):
         Type                               Name                                         Plan       
     +   pulumi:pulumi:Stack                aws-s3-folder-dev                            create     
     +   ├─ aws:s3:Bucket                   s3-website-bucket                            create     
     +   ├─ aws:s3:BucketPublicAccessBlock  s3-website-bucket-***-publicaccessblock      create     
     +   ├─ aws:s3:BucketPolicy             s3-website-bucket-***-access-policy          create     
     +   ├─ aws:s3:BucketObject             favicon.ico                                  create     
     +   └─ aws:s3:BucketObject             index.html                                   create    
    
    
    Outputs:
        bucketName: "s3-website-bucket-***"
        websiteUrl: output<string>
    
    Resources:
    + 6 to create
    
    Do you want to perform this update? yes
    Updating (example-aws-native-s3-folder):
    Type                                   Name                                     Status
    +   pulumi:pulumi:Stack                aws-s3-folder-dev                        created (4s)
    +   ├─ aws:s3:Bucket                   s3-website-bucket                        created (1s)
    +   ├─ aws:s3:BucketPublicAccessBlock  s3-website-bucket-***-publicaccessblock  created (0.38s)
    +   ├─ aws:s3:BucketPolicy             s3-website-bucket-***-access-policy      created (0.30s)
    +   ├─ aws:s3:BucketObject             favicon.ico                              created (0.37s)
    +   └─ aws:s3:BucketObject             index.html                               created (0.29s)
    
    
    Outputs:
    bucketName: "s3-website-bucket-***"
    websiteUrl: "s3-website-bucket-***.s3-website.us-west-2.amazonaws.com"
    
    Resources:
    + 6 created
    
    Duration: 7s
    ```

4. To see the resources that were created, run `pulumi stack output`:

    ```bash
    pulumi stack output
    ```
    ```
    Current stack outputs (2):
    OUTPUT      VALUE
        bucketName  s3-website-bucket-***
        websiteUrl  s3-website-bucket-***.s3-website.us-west-2.amazonaws.com
    ```

5. To see that the S3 objects exist, you can either use the AWS Console or the AWS CLI:

    ```bash
    aws s3 ls $(pulumi stack output bucketName)
    ```
    ```
    2023-09-28 11:44:12      15406 favicon.ico
    2023-09-28 11:44:12        223 index.html
    ```

6. Open the site URL in a browser to see both the rendered HTML and the favicon:

    ```bash
    open http://$(pulumi stack output websiteUrl)
    ```

7. From there, feel free to experiment. Simply making edits and running pulumi up will incrementally update your infrastructure.

8. To clean up resources, destroy your stack and remove it:

    ```bash
    pulumi destroy
    ```
    ```bash
    pulumi stack rm aws-s3-folder-dev
    ```