package whisk.docker

import java.io.ByteArrayInputStream
import java.util.logging.LogManager

import org.slf4j.LoggerFactory

import scala.concurrent.{ Future, ExecutionContext }
import com.github.dockerjava.core.DockerClientConfig

trait DockerKit {
  implicit val docker: Docker = new Docker(DockerClientConfig.createDefaultConfigBuilder().build())

  private lazy val log = LoggerFactory.getLogger(this.getClass)

  {
    val lm = LogManager.getLogManager
    lm.reset()
    val lmConfig =
      """handlers = java.util.logging.ConsoleHandler
        |.level = SEVERE
        |java.util.logging.ConsoleHandler.level = SEVERE
        |java.util.logging.ConsoleHandler.formatter = java.util.logging.SimpleFormatter
        |""".stripMargin

    lm.readConfiguration(new ByteArrayInputStream(lmConfig.getBytes))
  }

  // we need ExecutionContext in order to run docker.init() / docker.stop() there
  implicit def dockerExecutionContext: ExecutionContext = ExecutionContext.global

  def dockerContainers: List[DockerContainer] = Nil

  def listImages(): Future[Set[String]] = {
    import scala.collection.JavaConverters._
    Future(docker.client.listImagesCmd().exec().asScala.flatMap(_.getRepoTags).toSet)
  }

  def stopRmAll(): Future[Seq[DockerContainer]] =
    // TODO improve the order of containers to shutdown
    Future.traverse(dockerContainers)(_.remove(force = true))

  def pullImages(): Future[Seq[DockerContainer]] = {
    listImages().flatMap { images =>
      val containersToPull = dockerContainers.filterNot { c =>
        val cImage = if (c.image.contains(":")) c.image else c.image + ":latest"
        images(cImage)
      }
      Future.traverse(containersToPull)(_.pull())
    }
  }

  def initReadyAll(): Future[Seq[(DockerContainer, Boolean)]] = {
    // TODO improve this by defining the right order and concurrency for containers to get initialized
    for {
      (childs, parents) <- Future successful dockerContainers.partition(_.hasDependency)

      // Init containers that don't have any dependency
      p <- initReadyAll(parents)

      // Init containers that have dependencies
      c <- initReadyAll(childs)

      results <- Future successful (p ::: c)

    } yield results
  }

  private[this] def initReadyAll(containers: List[DockerContainer]) =
    Future.traverse(containers)(_.init()).flatMap(Future.traverse(_)(c => c.isReady().map(c -> _).recover {
      case e =>
        log.error(e.getMessage, e)
        c -> false
    }))
}
