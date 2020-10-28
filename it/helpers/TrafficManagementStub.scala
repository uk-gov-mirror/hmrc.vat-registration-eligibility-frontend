package helpers

import com.github.tomakehurst.wiremock.client.WireMock._

trait TrafficManagementStub {

  private def trafficManagementUrl(regId: String) = s"/traffic-management/$regId/allocate"

  def stubAllocation(regId: String)(status: Int) =
    stubFor(post(urlMatching(trafficManagementUrl(regId)))
      .willReturn(aResponse.withStatus(status)))

}
