package www

import helpers.IntegrationSpecBase
import play.mvc.Http.HeaderNames

class IndexControllerISpec extends IntegrationSpecBase {

  s"GET ${controllers.routes.IndexController.navigateToPageId("foo").url}" should {
    "redirect to the start of eligibility because question id is invalid" in {
      val request = buildClient("/question?pageId=foo").get()
      val result = await(request)
      result.status mustBe 303
      result.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.IntroductionController.onPageLoad().url)
    }
    "redirect to page specified" in {
      val request = buildClient("/question?pageId=zeroRatedSales").get()
      val result = await(request)
      result.status mustBe 303
      result.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.ZeroRatedSalesController.onPageLoad().url)
    }
  }
}