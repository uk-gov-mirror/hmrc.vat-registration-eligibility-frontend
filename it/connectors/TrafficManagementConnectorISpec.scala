package connectors

import helpers.{AuthHelper, IntegrationSpecBase, SessionStub, TrafficManagementStub}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

class TrafficManagementConnectorISpec extends IntegrationSpecBase
  with AuthHelper
  with SessionStub
  with TrafficManagementStub {

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .configure(fakeConfig())
    .build()

  val testRegId = "1"

  val connector = app.injector.instanceOf[TrafficManagementConnector]

  "POST /traffic-management/:regId/allocate" must {
    "return Allocated when the API responds with CREATED" in {
      stubSuccessfulLogin()
      stubAllocation(testRegId)(CREATED)

      val res = await(connector.allocate(testRegId)(HeaderCarrier()))

      res mustBe Allocated
    }
    "return QuotaReached when the API responds with TOO_MANY_REQUESTS" in {
      stubSuccessfulLogin()
      stubAllocation(testRegId)(TOO_MANY_REQUESTS)

      val res = await(connector.allocate(testRegId)(HeaderCarrier()))

      res mustBe QuotaReached
    }
    "throw an exception for any other status" in {
      stubSuccessfulLogin()
      stubAllocation(testRegId)(IM_A_TEAPOT)

      intercept[Exception] {
        await(connector.allocate(testRegId)(HeaderCarrier()))
      }
    }
  }

}
