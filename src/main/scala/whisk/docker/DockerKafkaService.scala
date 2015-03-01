package whisk.docker

import com.github.dockerjava.api.model.Link

trait DockerKafkaService extends DockerKit { self: DockerZooKeeperService =>

  val kafkaContainer = DockerContainer("ches/kafka")
    .withPorts(9092 -> Some(9092), 7203 -> Some(7203))
    .withReadyChecker(DockerReadyChecker.LogLine(_.contains("started (kafka.server.KafkaServer)")))
    .withNetworkMode("host") // TODO get kafka to find zk container through links
    .dependsOn(zkContainer)

  abstract override def dockerContainers: List[DockerContainer] = kafkaContainer :: super.dockerContainers
}
