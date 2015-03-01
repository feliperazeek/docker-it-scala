package whisk.docker

import com.github.dockerjava.api.command.{ CreateContainerCmd, StartContainerCmd }
import com.github.dockerjava.api.model.{ ExposedPort, Ports, Link }
import scala.concurrent.{ Future, ExecutionContext }
import org.slf4j.LoggerFactory

case class DockerContainer(
    image: String,
    command: Option[Seq[String]] = None,
    bindPorts: Map[Int, Option[Int]] = Map.empty,
    tty: Boolean = false,
    stdinOpen: Boolean = false,
    readyChecker: DockerReadyChecker = DockerReadyChecker.Always,
    name: Option[String] = None,
    links: Seq[Link] = Nil,
    withPublishAllPorts: Boolean = true,
    networkMode: String = "bridge",
    dependencies: Seq[DockerContainer] = Nil) extends DockerContainerOps {

  def withCommand(cmd: String*) = copy(command = Some(cmd))

  def withPorts(ps: (Int, Option[Int])*) = copy(bindPorts = ps.toMap)

  def withName(value: String) = copy(name = Some(value))

  def withNetworkMode(value: String) = copy(networkMode = value)

  def withLinks(value: Link*) = copy(links = value)

  def withReadyChecker(checker: DockerReadyChecker) = copy(readyChecker = checker)

  def dependsOn(value: DockerContainer*) = copy(dependencies = value)

  val hasDependency: Boolean = !(dependencies.isEmpty)

  private[docker] def prepareCreateCmd(cmd: CreateContainerCmd): CreateContainerCmd = {
    val c = command
      .fold(cmd)(cmd.withCmd(_: _*))
      .withPortSpecs(bindPorts.map(kv => kv._2.fold("")(_.toString + ":") + kv._1).toSeq: _*)
      .withExposedPorts(bindPorts.keys.map(ExposedPort.tcp).toSeq: _*)
      .withTty(tty)
      .withStdinOpen(stdinOpen)

    name map c.withName getOrElse c
  }

  def init(_id: SinglePromise[String])(implicit docker: Docker, ec: ExecutionContext): Future[this.type] =
    for {
      s <- _id.init(Future(prepareCreateCmd(docker.client.createContainerCmd(image)).exec()).map { resp =>
        if (resp.getId != null && resp.getId != "") {
          resp.getId
        } else {
          throw new RuntimeException(s"Cannot run container $image: ${resp.getWarnings.mkString(", ")}")
        }
      })
      _ <- Future(prepareStartCmd(docker.client.startContainerCmd(s)).exec())
    } yield this

  private[docker] def prepareStartCmd(cmd: StartContainerCmd): StartContainerCmd =
    cmd
      .withPortBindings(
        bindPorts.foldLeft(new Ports()) {
          case (ps, (guestPort, Some(hostPort))) =>
            ps.bind(ExposedPort.tcp(guestPort), Ports.Binding(hostPort))
            ps
          case (ps, (guestPort, None)) =>
            ps.bind(ExposedPort.tcp(guestPort), new Ports.Binding())
            ps
        }
      ).withLinks(links: _*).withPublishAllPorts(withPublishAllPorts).withNetworkMode(networkMode)
}