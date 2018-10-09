package www

import helpers.{AuthHelper, IntegrationSpecBase, SessionStub}
import identifiers.CompletionCapacityFillingInForId
import org.jsoup.Jsoup
import play.api.test.FakeApplication
import play.mvc.Http.HeaderNames

class CompletionCapacityControllerISpec extends IntegrationSpecBase with AuthHelper with SessionStub {

  val internalId = "testInternalId"
  val singleOfficer = """
      |{
      |  "company_name" : "Test Company",
      |  "officers": [
      |    {
      |      "name_elements" : {
      |        "forename" : "test1",
      |        "other_forenames" : "test11",
      |        "surname" : "testa",
      |        "title" : "Mr"
      |      },
      |      "officer_role" : "director"
      |    }, {
      |      "name_elements" : {
      |        "forename" : "test2",
      |        "other_forenames" : "test22",
      |        "surname" : "testb",
      |        "title" : "Mr"
      |      },
      |      "officer_role" : "corporate-director"
      |    }
      |  ]
      |}""".stripMargin

  val multipleOfficers = """
       |{
       |  "company_name" : "Test Company",
       |  "officers": [
       |    {
       |      "name_elements" : {
       |        "forename" : "test1",
       |        "other_forenames" : "test11",
       |        "surname" : "testa",
       |        "title" : "Mr"
       |      },
       |      "officer_role" : "director"
       |    }, {
       |      "name_elements" : {
       |        "forename" : "test2",
       |        "other_forenames" : "test22",
       |        "surname" : "testb",
       |        "title" : "Mr"
       |      },
       |      "officer_role" : "secretary"
       |    }
       |  ]
       |}""".stripMargin

  override implicit lazy val app = FakeApplication(additionalConfiguration = fakeConfig())

  s"GET ${controllers.routes.CompletionCapacityController.onPageLoad()} in a post incorp flow" should {
    "render page" when {
      "only a single officer is present in II" in {
        stubSuccessfulLogin()
        stubSuccessfulRegIdGet()
        stubSuccessfulTxIdGet()
        stubSuccessfulIncorpDataGet()
        stubSuccessfulCompanyNameGet()
        stubAudits()

        stubGet("/incorporation-information/testTxId/officer-list", 200, singleOfficer)

        val request = buildClient("/applicant-name").withHeaders(HeaderNames.COOKIE -> getSessionCookie()).get()
        val result = await(request)
        result.status mustBe 200

        val document = Jsoup.parse(result.body)

        document.getElementById("main-heading").text() mustBe "Are you test1 testa?"
        document.getElementsByAttributeValue("for", "completionCapacity-no").text() mustBe "No"
        document.getElementsByAttributeValue("for", "completionCapacity-yes").text() mustBe "Yes"
      }

      "multiple officers are present in II" in {
        stubSuccessfulLogin()
        stubSuccessfulRegIdGet()
        stubSuccessfulTxIdGet()
        stubSuccessfulIncorpDataGet()
        stubSuccessfulCompanyNameGet()
        stubAudits()

        stubGet("/incorporation-information/testTxId/officer-list", 200, multipleOfficers)

        val request = buildClient("/applicant-name").withHeaders(HeaderNames.COOKIE -> getSessionCookie()).get()
        val result = await(request)
        result.status mustBe 200

        val document = Jsoup.parse(result.body)

        document.getElementById("main-heading").text() mustBe "Which company officer are you?"
        document.getElementsByAttributeValue("for", "completionCapacity-noneOfThese").text() mustBe "None of these"
        document.getElementsByAttributeValue("for", "completionCapacity-Mrtest1test11testadirector").text() mustBe "test1 testa"
        document.getElementsByAttributeValue("for", "completionCapacity-Mrtest2test22testbsecretary").text() mustBe "test2 testb"
      }
    }
  }

  s"POST ${controllers.routes.CompletionCapacityController.onSubmit()}" should {
    "return a 400 with the correct error message" when {
      "nothing is selection for only one director" in {
        stubSuccessfulLogin()
        stubSuccessfulRegIdGet()
        stubSuccessfulTxIdGet()
        stubSuccessfulIncorpDataGet()
        stubSuccessfulCompanyNameGet()
        stubAudits()

        stubGet("/incorporation-information/testTxId/officer-list", 200, singleOfficer)

        val request = buildClient("/applicant-name").withHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
          .post(Map("value" -> Seq("")))

        val result = await(request)

        result.status mustBe 400

        val document = Jsoup.parse(result.body)

        document.getElementById("error-summary-heading").text() mustBe "This page has errors"
        document.getElementsByAttributeValue("href","#value").text() mustBe "Tell us if you are test1 testa"
      }

      "nothing is selected for multiple directors" in {
        stubSuccessfulLogin()
        stubSuccessfulRegIdGet()
        stubSuccessfulTxIdGet()
        stubSuccessfulIncorpDataGet()
        stubSuccessfulCompanyNameGet()
        stubAudits()

        stubGet("/incorporation-information/testTxId/officer-list", 200, multipleOfficers)

        val request = buildClient("/applicant-name").withHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
          .post(Map("value" -> Seq("")))

        val result = await(request)

        result.status mustBe 400

        val document = Jsoup.parse(result.body)

        document.getElementById("error-summary-heading").text() mustBe "This page has errors"
        document.getElementsByAttributeValue("href","#value").text() mustBe "Tell us which of these people you are"
      }
    }

    s"redirect to ${controllers.routes.CompletionCapacityFillingInForController.onPageLoad().url}" when {
      "only a single officer is present in II and none of these is selected" in {
        stubSuccessfulLogin()
        stubSuccessfulRegIdGet()
        stubSuccessfulTxIdGet()
        stubSuccessfulIncorpDataGet()
        stubSuccessfulCompanyNameGet()
        stubAudits()

        cacheSessionData(internalId, CompletionCapacityFillingInForId.toString, "MrNewDirector")

        stubGet("/incorporation-information/testTxId/officer-list", 200, singleOfficer)

        val request = buildClient("/applicant-name").withHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
          .post(Map("value" -> Seq("noneOfThese")))

        val result = await(request)

        result.status mustBe 303
        result.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.CompletionCapacityFillingInForController.onPageLoad().url)
        verifySessionCacheData(internalId, CompletionCapacityFillingInForId.toString, Some("MrNewDirector"))
      }

      "multiple officers are present in II and none of these is selected" in {
        stubSuccessfulLogin()
        stubSuccessfulRegIdGet()
        stubSuccessfulTxIdGet()
        stubSuccessfulIncorpDataGet()
        stubSuccessfulCompanyNameGet()
        stubAudits()

        cacheSessionData(internalId, CompletionCapacityFillingInForId.toString, "MrNewDirector")

        stubGet("/incorporation-information/testTxId/officer-list", 200, multipleOfficers)

        val request = buildClient("/applicant-name").withHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
          .post(Map("value" -> Seq("noneOfThese")))

        val result = await(request)

        result.status mustBe 303
        result.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.CompletionCapacityFillingInForController.onPageLoad().url)
        verifySessionCacheData(internalId, CompletionCapacityFillingInForId.toString, Some("MrNewDirector"))
      }
    }
    "redirect to first eligibility page" should {
      "a director is selected clearing CompletionCapacityFillingInFor data" in {
        stubSuccessfulLogin()
        stubSuccessfulRegIdGet()
        stubSuccessfulTxIdGet()
        stubSuccessfulIncorpDataGet()
        stubSuccessfulCompanyNameGet()
        stubAudits()

        cacheSessionData(internalId, CompletionCapacityFillingInForId.toString, "MrNewDirector")

        stubGet("/incorporation-information/testTxId/officer-list", 200, multipleOfficers)

        val request = buildClient("/applicant-name").withHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
          .post(Map("value" -> Seq("Mrtest1test11testadirector")))

        val result = await(request)

        result.status mustBe 303
        result.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.InvolvedInOtherBusinessController.onPageLoad().url)
        verifySessionCacheData(internalId, CompletionCapacityFillingInForId.toString, Option.empty[String])
      }

      "yes is selected for a single director clearing CompletionCapacityFillingInFor data" in {
        stubSuccessfulLogin()
        stubSuccessfulRegIdGet()
        stubSuccessfulTxIdGet()
        stubSuccessfulIncorpDataGet()
        stubSuccessfulCompanyNameGet()
        stubAudits()

        stubGet("/incorporation-information/testTxId/officer-list", 200, multipleOfficers)

        cacheSessionData(internalId, CompletionCapacityFillingInForId.toString, "MrNewDirector")

        val request = buildClient("/applicant-name").withHeaders(HeaderNames.COOKIE -> getSessionCookie(), "Csrf-Token" -> "nocheck")
          .post(Map("value" -> Seq("Mrtest1test11testadirector")))

        val result = await(request)

        result.status mustBe 303
        result.header(HeaderNames.LOCATION) mustBe Some(controllers.routes.InvolvedInOtherBusinessController.onPageLoad().url)

        verifySessionCacheData(internalId, CompletionCapacityFillingInForId.toString, Option.empty[String])
      }
    }
  }
}
