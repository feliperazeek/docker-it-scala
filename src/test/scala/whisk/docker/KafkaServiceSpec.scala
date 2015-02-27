package whisk.docker

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Second, Seconds, Span }
import org.scalatest.{ GivenWhenThen, BeforeAndAfterAll, Matchers, FlatSpec }
import whisk.docker.test.DockerTestKit

class KafkaServiceSpec extends FlatSpec with Matchers with BeforeAndAfterAll with GivenWhenThen with ScalaFutures
    with DockerKafkaService with DockerTestKit {

  implicit val pc = PatienceConfig(Span(30, Seconds), Span(2, Seconds))

  "all containers" should "be ready at the same time" in {
    dockerContainers.map(_.list).flatten.map(_.image).foreach(println)
    dockerContainers.map(_.list).flatten.forall(_.isReady().futureValue) shouldBe true
  }
}