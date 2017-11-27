package nl.knaw.dans.easy.download.components


import java.io.{ InputStream, OutputStream }
import java.net.{ URI, URLEncoder }
import java.nio.file.Path
import java.util.UUID

import nl.knaw.dans.lib.logging.DebugEnhancedLogging
import org.apache.commons.io.IOUtils

import scala.util.Try
import scalaj.http.Http

trait BagStoreComponent extends DebugEnhancedLogging {
  val bagStore: BagStore

  trait BagStore {
    val baseUri: URI

    def copyStream(bagId: UUID, path: Path, outputStream: OutputStream): Try[Unit] = {
      for {
        f <- Try(URLEncoder.encode(path.toString, "UTF8"))
        uri <- Try(baseUri.resolve(s"stores/pdbs/bags/$bagId/$f")) // TODO drop 'stores/pdbs' when easy-bag-store#43 not only merged but also versioned
        _ <- Try(Http(uri.toString).method("GET").execute((is: InputStream) => IOUtils.copy(is, outputStream)))
      } yield ()
    }
  }
}
