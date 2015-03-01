package whisk.docker

trait DockerZooKeeperService extends DockerKit {

  val zkContainer = DockerContainer("jplock/zookeeper:3.4.6")
    .withPorts(2181 -> Some(2181), 2888 -> Some(2888), 3888 -> Some(3888))
    .withReadyChecker(DockerReadyChecker.LogLine(_.contains("binding to port")))
    .withNetworkMode("host") // TODO get kafka to find zk container through links

  abstract override def dockerContainers: List[DockerContainer] = zkContainer :: super.dockerContainers
}
