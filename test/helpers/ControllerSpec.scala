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

package helpers

import java.time.LocalDate

import builders.AuthBuilder
import common.enums.VatRegStatus
import fixtures.VatRegistrationFixture
import mocks.{AuthMock, VatMocks}
import models.CurrentProfile
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.{Assertion, BeforeAndAfterEach}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.http.{HeaderNames, Status}
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, AnyContentAsFormUrlEncoded, RequestHeader, Result}
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits, ResultExtractors}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait ControllerSpec extends PlaySpec with MockitoSugar with AuthMock with AuthBuilder with VatMocks
                     with BeforeAndAfterEach with VatRegistrationFixture with FutureAwaits with DefaultAwaitTimeout
                     with ResultExtractors with Status with HeaderNames {
  implicit val hc: HeaderCarrier = HeaderCarrier()

  val incorpDate: LocalDate = LocalDate.of(2016, 12, 21)
  val incorpDateWithinYear: LocalDate = LocalDate.now().minusMonths(6)

  implicit val currentProfile = CurrentProfile("Test Me", testRegId, "000-434-1", VatRegStatus.draft, Some(incorpDate))

  override def beforeEach() {
    resetMocks()
  }

  def submitAuthorised(a: => Action[AnyContent], r: => FakeRequest[AnyContentAsFormUrlEncoded])
                      (test: Future[Result] => Assertion): Unit =
    submitWithAuthorisedUser(a, r)(test)

  def callAuthenticated(a: Action[AnyContent])(test: Future[Result] => Assertion): Unit =
    withAuthorisedUser(a)(test)

  def mockWithCurrentProfile(currentProfile: CurrentProfile): OngoingStubbing[Future[CurrentProfile]] = {
    when(mockCurrentProfileService.getCurrentProfile()(any()))
      .thenReturn(Future.successful(currentProfile))
  }

  def mockWithCurrentProfileException(except: Throwable): OngoingStubbing[Future[CurrentProfile]] = {
    when(mockCurrentProfileService.getCurrentProfile()(any()))
      .thenReturn(Future.failed(except))
  }
}

trait MockMessages {

  val mockMessagesAPI: MessagesApi

  val lang = Lang("en")
  val messages = Messages(lang, mockMessagesAPI)

  val MOCKED_MESSAGE = "mocked message"

  def mockAllMessages: OngoingStubbing[String] = {
    when(mockMessagesAPI.preferred(any[RequestHeader]()))
      .thenReturn(messages)

    when(mockMessagesAPI.apply(any[String](), any())(any()))
      .thenReturn(MOCKED_MESSAGE)
  }
}
