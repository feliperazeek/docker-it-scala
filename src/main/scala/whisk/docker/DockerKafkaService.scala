package whisk.docker

import com.github.dockerjava.api.model.Link

trait DockerKafkaService extends DockerKit {

  val zkContainer = SingleDockerContainer("jplock/zookeeper:3.4.6")
    .withPorts(2181 -> Some(2181), 2888 -> Some(2888), 3888 -> Some(3888))
    .withReadyChecker(DockerReadyChecker.LogLine(_.contains("binding to port")))
    .withNetworkMode("host")

  val kafkaContainer = SingleDockerContainer("ches/kafka")
    .withPorts(9092 -> Some(9092), 7203 -> Some(7203))
    .withReadyChecker(DockerReadyChecker.LogLine(_.contains("started (kafka.server.KafkaServer)")))
    .withNetworkMode("host")

  abstract override def dockerContainers: List[DockerContainer] = LinkedContainers(zkContainer, kafkaContainer) :: super.dockerContainers
}
