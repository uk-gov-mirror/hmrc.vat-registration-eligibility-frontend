/*
 * Copyright 2019 HM Revenue & Customs
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

import java.time.LocalDate

import config.FrontendAppConfig
import models.CurrentProfile
import models.requests.{CacheIdentifierRequest, DataRequest}
import org.scalatestplus.play.guice._
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.Injector
import play.api.test.FakeRequest
import uk.gov.hmrc.http.cache.client.CacheMap
import utils.UserAnswers

trait SpecBase extends CommonSpecBase with GuiceOneAppPerSuite {

  def injector: Injector = app.injector

  def frontendAppConfig: FrontendAppConfig = injector.instanceOf[FrontendAppConfig]

  def messagesApi: MessagesApi = injector.instanceOf[MessagesApi]

  def fakeRequest = FakeRequest("", "")
  def fakeDataRequestIncorped = new DataRequest(fakeRequest,"1",CurrentProfile("foo","bar",Some(LocalDate.of(2018,9,24)), "Test Company"), new UserAnswers((CacheMap("1",Map()))))
  def fakeDataRequestIncorpedOver12m = new DataRequest(fakeRequest,"1",CurrentProfile("foo","bar",Some(LocalDate.of(2016,9,24)), "Test Company"), new UserAnswers((CacheMap("1",Map()))))

  def fakeDataRequest = new DataRequest(fakeRequest,"1",CurrentProfile("foo","bar",Some(LocalDate.of(2016,10,1)), "Test Company"), new UserAnswers((CacheMap("1",Map()))))

  def fakeCacheDataRequestIncorped = new CacheIdentifierRequest(fakeRequest,"1",CurrentProfile("foo","bar",Some(LocalDate.of(2018,9,24)), "Test Company"))


  def messages: Messages = messagesApi.preferred(fakeRequest)
  def messagesIncorped: Messages = messagesApi.preferred(fakeDataRequestIncorped)
}
