package whisk.docker

trait DockerMongodbService extends DockerKit {

  val DefaultMongodbPort = 27017

  val mongodbContainer = SingleDockerContainer("mongo:2.6.5")
    .withPorts(DefaultMongodbPort -> None)
    .withReadyChecker(DockerReadyChecker.LogLine(_.contains("waiting for connections on port")))
    .withCommand("mongod", "--nojournal", "--smallfiles", "--syncdelay", "0")

  abstract override def dockerContainers: List[DockerContainer] = mongodbContainer :: super.dockerContainers
}
