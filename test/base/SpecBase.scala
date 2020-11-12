/*
 * Copyright 2020 HM Revenue & Customs
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

package base

import config.FrontendAppConfig
import controllers.actions.FakeDataRetrievalAction
import models.CurrentProfile
import models.requests.{CacheIdentifierRequest, DataRequest}
import org.scalatestplus.play.guice._
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.Injector
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.MessagesControllerComponents
import play.api.test.FakeRequest
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.UserAnswers

import scala.concurrent.ExecutionContext

trait SpecBase extends CommonSpecBase with GuiceOneAppPerSuite {

  override lazy val app: Application = new GuiceApplicationBuilder()
    .configure("metrics.enabled" -> "false")
    .build()

  lazy val injector: Injector = app.injector

  implicit lazy val frontendAppConfig: FrontendAppConfig = injector.instanceOf[FrontendAppConfig]

  lazy val controllerComponents: MessagesControllerComponents = injector.instanceOf[MessagesControllerComponents]

  implicit lazy val executionContext: ExecutionContext = injector.instanceOf[ExecutionContext]

  lazy val messagesApi: MessagesApi = injector.instanceOf[MessagesApi]

  lazy val fakeRequest = FakeRequest("", "")

  lazy val fakeDataRetrievalAction = new FakeDataRetrievalAction(Some(CacheMap("id", Map())))

  lazy val fakeDataRequestIncorped = new DataRequest(fakeRequest, "1", CurrentProfile("foo"), new UserAnswers((CacheMap("1", Map()))))

  lazy val fakeDataRequestIncorpedOver12m = new DataRequest(fakeRequest, "1", CurrentProfile("foo"), new UserAnswers((CacheMap("1", Map()))))

  lazy val fakeDataRequest = new DataRequest(fakeRequest, "1", CurrentProfile("foo"), new UserAnswers(CacheMap("1", Map())))

  lazy val fakeCacheDataRequestIncorped = new CacheIdentifierRequest(fakeRequest, "1", CurrentProfile("foo"))

  lazy val messages: Messages = messagesApi.preferred(fakeRequest)

  lazy val messagesIncorped: Messages = messagesApi.preferred(fakeDataRequestIncorped)
}
