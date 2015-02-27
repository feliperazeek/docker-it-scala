package whisk.docker

import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time._
import whisk.docker.test.DockerTestKit

class ZooKeeperServiceSpec extends FlatSpec with Matchers with BeforeAndAfterAll with GivenWhenThen with ScalaFutures
    with DockerZooKeeperService with DockerTestKit {

  implicit val pc = PatienceConfig(Span(20, Seconds), Span(1, Second))

  "zookeeper container" should "be ready" in {
    zkContainer.isReady().futureValue shouldBe true
  }

}