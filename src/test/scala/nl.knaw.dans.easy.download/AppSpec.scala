package nl.knaw.dans.easy.download

import java.io.FileNotFoundException
import java.util.UUID

import nl.knaw.dans.easy.download.components.{ FileItem, User }
import nl.knaw.dans.easy.download.components.RightsFor.{ ANONYMOUS, KNOWN, RESTRICTED_GROUP, RESTRICTED_REQUEST }
import org.apache.commons.configuration.PropertiesConfiguration
import org.joda.time.DateTime
import org.scalamock.scalatest.MockFactory

import scala.util.{ Failure, Success }

class AppSpec extends TestSupportFixture with MockFactory {

  private val uuid = UUID.randomUUID()
  private val app: EasyDownloadApp = new EasyDownloadApp {
    // mocking at a low level to test the chain of error handling
    override val http: HttpWorker = mock[HttpWorker]
    override val bagStore: BagStore = mock[BagStore]
    override val authentication: Authentication = mock[Authentication]
    override val permissionRequest: PermissionRequest = mock[PermissionRequest]
    override lazy val configuration: Configuration = new Configuration("1.0.0",
      new PropertiesConfiguration() {
        addProperty("bag-store.url", "http://localhost:20110/")
        addProperty("bag-store.connection-timeout-ms", 600000)
        addProperty("bag-store.read-timeout-ms", 600000)
        addProperty("auth-info.url", "http://localhost:20170/")
        addProperty("ark.name-assigning-authority-number", "123456")
      })
  }

  "hasDownloadPermissionFor" should "allow owner" in {
    val fileItem = FileItem(itemId = "uuid/file.txt", owner = "someone",
      dateAvailable = new DateTime("4016-12-15"),
      accessibleTo = RESTRICTED_REQUEST,
      visibleTo = RESTRICTED_REQUEST,
      licenseKey = "http://opensource.org/licenses/MIT",
      licenseTitle = "MIT.txt",
    )
    app.hasDownloadPermissionFor(uuid, fileItem, Some(User("someone"))) shouldBe a[Success[_]]
  }

  it should "allow known" in {
    val fileItem = FileItem(itemId = "uuid/data/file.txt", owner = "someone",
      dateAvailable = new DateTime("2016-12-15"),
      accessibleTo = KNOWN,
      visibleTo = KNOWN,
      licenseKey = "http://opensource.org/licenses/MIT",
      licenseTitle = "MIT.txt",
    )
    app.hasDownloadPermissionFor(uuid, fileItem, Some(User("somebody"))) shouldBe a[Success[_]]
  }

  it should "allow restricted group" in {
    val fileItem = FileItem(itemId = "uuid/data/file.txt", owner = "someone",
      dateAvailable = new DateTime("2016-12-15"),
      accessibleTo = RESTRICTED_GROUP,
      visibleTo = RESTRICTED_GROUP,
      licenseKey = "http://opensource.org/licenses/MIT",
      licenseTitle = "MIT.txt",
    )
    app.hasDownloadPermissionFor(uuid, fileItem, Some(User("someone else", Seq("Archaeology")))) shouldBe a[Success[_]]
  }

  it should "allow restricted request" in {
    val fileItem = FileItem(itemId = "uuid/data/file.txt", owner = "someone",
      dateAvailable = new DateTime("2016-12-15"),
      accessibleTo = RESTRICTED_REQUEST,
      visibleTo = RESTRICTED_REQUEST,
      licenseKey = "http://opensource.org/licenses/MIT",
      licenseTitle = "MIT.txt",
    )
    val user = User("someone else")
    app.permissionRequest.userHasPermission _ expects(user.id, uuid) once() returning Success(true)
    app.hasDownloadPermissionFor(uuid, fileItem, Some(user)) shouldBe a[Success[_]]
  }

  it should "allow metadata for owner" in {
    val fileItem = FileItem(itemId = "uuid/file.txt", owner = "someone",
      dateAvailable = new DateTime("2016-12-15"),
      accessibleTo = KNOWN,
      visibleTo = KNOWN,
      licenseKey = "http://opensource.org/licenses/MIT",
      licenseTitle = "MIT.txt",
    )
    app.hasDownloadPermissionFor(uuid, fileItem, Some(User("someone"))) shouldBe a[Success[_]]
  }

  it should "announce availability after login" in {
    val fileItem = FileItem(itemId = "uuid/data/file.txt", owner = "someone",
      dateAvailable = new DateTime("2016-12-15"),
      accessibleTo = KNOWN,
      visibleTo = ANONYMOUS,
      licenseKey = "http://opensource.org/licenses/MIT",
      licenseTitle = "MIT.txt",
    )
    app.hasDownloadPermissionFor(uuid, fileItem, None) should matchPattern {
      case Failure(NotAccessibleException("Please login to download: uuid/data/file.txt")) =>
    }
  }

  it should "announce availability if under embargo" in {
    val fileItem = FileItem(itemId = "uuid/data/file.txt", owner = "someone",
      dateAvailable = new DateTime("4016-12-15"),
      accessibleTo = KNOWN,
      visibleTo = ANONYMOUS,
      licenseKey = "http://opensource.org/licenses/MIT",
      licenseTitle = "MIT.txt",
    )
    app.hasDownloadPermissionFor(uuid, fileItem, Some(User("somebody"))) should matchPattern {
      case Failure(NotAccessibleException("Download becomes available on 4016-12-15 [uuid/data/file.txt]")) =>
    }
  }

  it should "refuse to user without group" in {
    val fileItem = FileItem(itemId = "uuid/data/file.txt", owner = "someone",
      dateAvailable = new DateTime("2016-12-15"),
      accessibleTo = RESTRICTED_GROUP,
      visibleTo = ANONYMOUS,
      licenseKey = "http://opensource.org/licenses/MIT",
      licenseTitle = "MIT.txt",
    )
    app.hasDownloadPermissionFor(uuid, fileItem, Some(User("somebody"))) should matchPattern {
      case Failure(NotAccessibleException("Download not allowed of: uuid/data/file.txt")) =>
    }
  }

  it should "be invisible for user without group" in {
    val fileItem = FileItem(itemId = "uuid/data/file.txt", owner = "someone",
      dateAvailable = new DateTime("2016-12-15"),
      accessibleTo = RESTRICTED_GROUP,
      visibleTo = RESTRICTED_GROUP,
      licenseKey = "http://opensource.org/licenses/MIT",
      licenseTitle = "MIT.txt",
    )
    app.hasDownloadPermissionFor(uuid, fileItem, Some(User("somebody"))) should matchPattern {
      case Failure(t: FileNotFoundException) if t.getMessage == "uuid/data/file.txt" =>
    }
  }

  it should "refuse to user without authentication, requesting a restricted group file" in {
    val fileItem = FileItem(itemId = "uuid/data/file.txt", owner = "someone",
      dateAvailable = new DateTime("2016-12-15"),
      accessibleTo = RESTRICTED_GROUP,
      visibleTo = ANONYMOUS,
      licenseKey = "http://opensource.org/licenses/MIT",
      licenseTitle = "MIT.txt",
    )
    app.hasDownloadPermissionFor(uuid, fileItem, None) should matchPattern {
      case Failure(NotAccessibleException("Please login to download: uuid/data/file.txt")) =>
    }
  }

  it should "refuse to user that is has no permission to view a restricted request file" in {
    val fileItem = FileItem(itemId = "uuid/data/file.txt", owner = "someone",
      dateAvailable = new DateTime("2016-12-15"),
      accessibleTo = RESTRICTED_REQUEST,
      visibleTo = ANONYMOUS,
      licenseKey = "http://opensource.org/licenses/MIT",
      licenseTitle = "MIT.txt",
    )
    val user = User("someone else")
    app.permissionRequest.userHasPermission _ expects(user.id, uuid) once() returning Success(false)
    app.hasDownloadPermissionFor(uuid, fileItem, Some(user)) should matchPattern {
      case Failure(NotAccessibleException("Download not allowed of: uuid/data/file.txt")) =>
    }
  }

  it should "refuse to user without authentication, requesting a restricted request file" in {
    val fileItem = FileItem(itemId = "uuid/data/file.txt", owner = "someone",
      dateAvailable = new DateTime("2016-12-15"),
      accessibleTo = RESTRICTED_REQUEST,
      visibleTo = ANONYMOUS,
      licenseKey = "http://opensource.org/licenses/MIT",
      licenseTitle = "MIT.txt",
    )
    app.hasDownloadPermissionFor(uuid, fileItem, None) should matchPattern {
      case Failure(NotAccessibleException("Please login to download: uuid/data/file.txt")) =>
    }
  }

  it should "announce availability if under embargo for group" in {
    val fileItem = FileItem(itemId = "uuid/data/file.txt", owner = "someone",
      dateAvailable = new DateTime("4016-12-15"),
      accessibleTo = RESTRICTED_GROUP,
      visibleTo = ANONYMOUS,
      licenseKey = "http://opensource.org/licenses/MIT",
      licenseTitle = "MIT.txt",
    )
    app.hasDownloadPermissionFor(uuid, fileItem, Some(User("somebody"))) should matchPattern {
      case Failure(NotAccessibleException("Download becomes available on 4016-12-15 [uuid/data/file.txt]")) =>
    }
  }
}
