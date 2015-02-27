package whisk.docker

trait DockerZooKeeperService extends DockerKit {

  val zkContainer = SingleDockerContainer("jplock/zookeeper:3.4.6")
    .withPorts(2181 -> None, 2888 -> None, 3888 -> None)
    .withReadyChecker(DockerReadyChecker.LogLine(_.contains("binding to port")))
    .withName("zk")

  abstract override def dockerContainers: List[DockerContainer] = zkContainer :: super.dockerContainers
}
