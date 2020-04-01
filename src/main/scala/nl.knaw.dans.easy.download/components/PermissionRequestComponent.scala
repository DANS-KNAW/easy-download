package nl.knaw.dans.easy.download.components

import java.util.UUID

import nl.knaw.dans.lib.logging.DebugEnhancedLogging

import scala.util.Try
import scala.xml.Elem

// using an extra interface layer here such that the implementation can easily be replaced
// by Dataverse or another permissions-store
trait PermissionRequestComponent extends DebugEnhancedLogging {

  val permissionRequest: PermissionRequest

  trait PermissionRequest {
    def userHasPermission(userId: String, bagId: UUID): Try[Boolean]
  }
}

trait FedoraPermissionRequestComponent extends PermissionRequestComponent {
  this: BagStoreComponent with FedoraComponent =>
  
  trait FedoraPermissionRequest extends PermissionRequest {
    override def userHasPermission(userId: String, bagId: UUID): Try[Boolean] = {
      for {
        ddm <- bagStore.loadDDM(bagId)
        fedoraId <- getFedoraId(ddm)
        prsql <- fedora.getPRSQL(fedoraId)
      } yield hasPermission(prsql)
    }
    
    private def getFedoraId(elem: Elem): Try[String] = ???

    private def hasPermission(elem: Elem): Boolean = ???
  }
}
