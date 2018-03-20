/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package utils

import java.time.LocalDate

import helpers.FutureAssertions
import mocks.VatMocks
import models.external._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class RegistrationWhitelistSpec extends PlaySpec with MockitoSugar with VatMocks with FutureAssertions {

  class Setup extends RegistrationWhitelist {
    override lazy val config = generateConfig(
      newWhitelistedPostIncorpRegIds = Seq("foo1"),
      newWhitelistedPreIncorpRegIds  = Seq("foo2"),
      newWhitelistedCompanyName      = Json.obj("company_name" -> "FOO BAR WIZZ AND BANG LTD")
    )
  }

  "isRegIDWhitelisted" should {
    "return obtained company name Future.successful(String) when regId is not Whitelisted Result" in new Setup {
      val res = ifRegIdNotWhitelisted("foo")(Future.successful(Json.obj("name" -> "fooBarWizz").as[JsValue]))(returnDefaultCompanyName)
      await(res) mustBe Json.obj("name" -> "fooBarWizz")
    }
    "return the whitelisted company name when regId is Whitelisted" in new Setup {
      val res = ifRegIdNotWhitelisted("foo1")(Future.successful(Json.obj("name" -> "fooBarWizz").as[JsValue]))(returnDefaultCompanyName)
      await(res) mustBe Json.obj("company_name" -> "FOO BAR WIZZ AND BANG LTD")
    }
  }

  "returnDefaultCompanyName" should {
    "return JsValue CompanyName" in new Setup {
      returnDefaultCompanyName("foo1") mustBe Json.obj("company_name" -> "FOO BAR WIZZ AND BANG LTD")
    }
  }
  "returnDefaultIncorpInfo" should {
    "return Some(IncorpInfo) if it is a Post Incorp regId" in new Setup {
      returnDefaultIncorpInfo("foo1") mustBe Some(IncorporationInfo(
        IncorpSubscription(
          transactionId = "fakeTxId-foo1",
          regime        = "vat",
          subscriber    = "scrs",
          callbackUrl   = "#"),
        IncorpStatusEvent(
          status = "accepted",
          crn = Some("90000001"),
          incorporationDate = Some(LocalDate.parse("2016-08-05")),
          description = None)))
    }
    "return None if it is a Pre Incorp regId" in new Setup {
      returnDefaultIncorpInfo("foo2") mustBe None
    }
  }
  "returnDefaultCompRegProfile" should {
    "return Some(CompanyRegistrationProfile)" in new Setup {
      returnDefaultCompRegProfile("foo1") mustBe CompanyRegistrationProfile("accepted", "fakeTxId-foo1")
    }
  }

  "returnDefaultTransId" should {
    "return transId" in new Setup {
      returnDefaultTransId("foo1") mustBe "fakeTxId-foo1"
    }
  }
}