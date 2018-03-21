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

package mocks

import config.AppConfig
import play.api.libs.json.{JsValue, Json}

trait ConfigMock {
  def generateConfig(newAnalyticsToken: String                    = "",
                     newAnalyticsHost: String                     = "",
                     newReportAProblemPartialUrl: String          = "",
                     newReportAProblemNonJSUrl: String            = "",
                     newTimeoutInSeconds: String                  = "",
                     newContactFrontendPartialBaseUrl: String     = "",
                     newVatRegFrontendFeedbackUrl: String            = "",
                     newWhitelistedPostIncorpRegIds: Seq[String]  = Seq(),
                     newWhitelistedPreIncorpRegIds: Seq[String]   = Seq(),
                     newWhitelistedCompanyName: JsValue           = Json.obj()) = new AppConfig {
    val analyticsToken: String                      = newAnalyticsToken
    val analyticsHost: String                       = newAnalyticsHost
    val reportAProblemPartialUrl: String            = newReportAProblemPartialUrl
    val reportAProblemNonJSUrl: String              = newReportAProblemNonJSUrl
    val timeoutInSeconds: String                    = newTimeoutInSeconds
    val vatRegFrontendFeedbackUrl: String           = newVatRegFrontendFeedbackUrl
    val contactFrontendPartialBaseUrl: String       = newContactFrontendPartialBaseUrl
    val whitelistedPostIncorpRegIds: Seq[String]    = newWhitelistedPostIncorpRegIds
    val whitelistedPreIncorpRegIds: Seq[String]     = newWhitelistedPreIncorpRegIds
    val defaultCompanyName: JsValue                 = newWhitelistedCompanyName
  }
}
