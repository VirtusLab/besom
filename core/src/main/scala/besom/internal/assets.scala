package besom.internal

sealed trait AssetOrArchive

enum Asset extends AssetOrArchive:
  case FileAsset(path: String) // TODO: java.nio.file.Path? validate it's a correct extension or MIME at compile time?
  case StringAsset(text: String)
  // TODO: a proper URI type? validate it's a proper URI? allows file://, http(s)://, custom schemes
  case RemoteAsset(uri: String)
  // case InvalidAsset // TODO - should we use this?

enum Archive extends AssetOrArchive:
  case FileArchive(path: String) // TODO: java.nio.file.Path? validate it's a correct extension or MIME at compile time?
  // TODO: a proper URI type? validate it's a proper URI? allows file://, http(s)://, custom schemes
  case RemoteArchive(uri: String)
  case AssetArchive(assets: Map[String, AssetOrArchive])
  // case InvalidArchive // TODO - should we use this?
