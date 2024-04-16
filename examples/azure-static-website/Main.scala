import besom.*
import besom.api.azurenative

@main def main = Pulumi.run {
  // All resources will share a resource group.
  val resourceGroupName = azurenative.resources.ResourceGroup("resourceGroup").name

  val profile = azurenative.cdn.Profile(
    name = "profile",
    azurenative.cdn.ProfileArgs(
      resourceGroupName = resourceGroupName,
      sku = azurenative.cdn.inputs.SkuArgs(
        name = azurenative.cdn.enums.SkuName.Standard_Microsoft
      )
    )
  )

  val storageAccount = azurenative.storage.StorageAccount(
    name = "storageaccount",
    azurenative.storage.StorageAccountArgs(
      enableHttpsTrafficOnly = true,
      kind = azurenative.storage.enums.Kind.StorageV2,
      resourceGroupName = resourceGroupName,
      sku = azurenative.storage.inputs.SkuArgs(
        name = azurenative.storage.enums.SkuName.Standard_LRS
      )
    )
  )

  // Enable static website support
  val staticWebsite = azurenative.storage.StorageAccountStaticWebsite(
    name = "staticWebsite",
    azurenative.storage.StorageAccountStaticWebsiteArgs(
      accountName = storageAccount.name,
      resourceGroupName = resourceGroupName,
      indexDocument = "index.html",
      error404Document = "404.html"
    )
  )

  // Upload the files
  val blobs = List("index.html", "404.html").map(name =>
    azurenative.storage.Blob(
      name = NonEmptyString(name).get,
      azurenative.storage.BlobArgs(
        resourceGroupName = resourceGroupName,
        accountName = storageAccount.name,
        containerName = staticWebsite.containerName,
        source = besom.types.Asset.FileAsset(s"./wwwroot/$name"),
        contentType = "text/html"
      )
    ),
  )

  val endpointOrigin = storageAccount.primaryEndpoints.web.map(_.replace("https://", "").replace("/", ""))

  val endpoint = azurenative.cdn.Endpoint(
    name = "endpoint",
    azurenative.cdn.EndpointArgs(
      endpointName = storageAccount.name.map(sa => s"cdn-endpnt-$sa"),
      isHttpAllowed = false,
      isHttpsAllowed = true,
      originHostHeader = endpointOrigin,
      origins = List(
        azurenative.cdn.inputs.DeepCreatedOriginArgs(
          hostName = endpointOrigin,
          httpsPort = 443,
          name = "origin-storage-account"
        )
      ),
      profileName = profile.name,
      queryStringCachingBehavior = azurenative.cdn.enums.QueryStringCachingBehavior.NotSet,
      resourceGroupName = resourceGroupName
    )
  )

  Stack(Output.sequence(blobs)).exports(
    staticEndpoint = storageAccount.primaryEndpoints.web,
    // CDN endpoint to the website.
    // Allow it some time after the deployment to get ready.
    cdnEndpoint = p"https://${endpoint.hostName}/"
  )
}
