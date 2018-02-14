/*
 * Copyright 2017 HM Revenue & Customs
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
package controllers

import helpers.RequestsFinder
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.PlaySpec
import support.AppAndStubs

class DeleteSessionItemsControllerISpec extends PlaySpec with AppAndStubs with RequestsFinder with ScalaFutures {

  "deleteSessionRelatedData" should {
    "return an OK" in {
      given()
        .user.isAuthorised
        .audit.writesAudit()
        .currentProfile.setup()
        .currentProfile.withProfile()
        .businessReg.getBusinessprofileSuccessfully
        .corporationTaxRegistration.existsWithStatus("draft")
        .keystore.deleteKeystore()
        .s4lContainer.cleared

      val response = buildInternalCall(s"1/delete-session").withHeaders("X-Session-ID" -> "session-1112223355556").delete()

      whenReady(response) {
        _.status mustBe 200
      }
    }

    "return an BadRequest if regId to be deleted doesn't match current profile" in {
      given()
        .user.isAuthorised
        .audit.writesAudit()
        .currentProfile.setup()
        .currentProfile.withProfile()
        .businessReg.getBusinessprofileSuccessfully
        .corporationTaxRegistration.existsWithStatus("draft")

      val response = buildInternalCall(s"25/delete-session").withHeaders("X-Session-ID" -> "session-1112223355556").delete()

      whenReady(response) {
        _.status mustBe 400
      }
    }

    "return an Internal Server Error if an error occurs on getting current profile" in {
      given()
        .user.isAuthorised
        .audit.writesAudit()
        .keystore.keystoreGetNotFound()
        .businessReg.failsToGetBusinessProfile
        .corporationTaxRegistration.existsWithStatus("draft")

      val response = buildInternalCall(s"1/delete-session").withHeaders("X-Session-ID" -> "session-1112223355556").delete()

      whenReady(response) {
        _.status mustBe 500
      }
    }

    "return 500 if user is not authorised" in {
      given()
        .user.isNotAuthorised
        .audit.writesAudit()

      val response = buildInternalCall(s"1/delete-session").withHeaders("X-Session-ID" -> "session-1112223355556").delete()

      whenReady(response) {
        _.status mustBe 500
      }
    }
  }
}
