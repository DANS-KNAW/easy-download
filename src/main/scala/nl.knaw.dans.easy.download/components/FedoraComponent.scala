package nl.knaw.dans.easy.download.components

import scala.util.Try
import scala.xml.Elem

trait FedoraComponent {

  val fedora: Fedora

  trait Fedora {
    def getPRSQL(datasetId: String): Try[Elem] = ???
  }
}
