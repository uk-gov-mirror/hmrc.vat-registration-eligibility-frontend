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

import org.mockito.Matchers
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import play.api.i18n.{Lang, Messages, MessagesApi}
import play.api.mvc.RequestHeader

trait MockMessages {

  val mockMessagesAPI: MessagesApi

  val lang = Lang("en")
  val messages = Messages(lang, mockMessagesAPI)

  val MOCKED_MESSAGE = "mocked message"

  def mockAllMessages: OngoingStubbing[String] = {
    when(mockMessagesAPI.preferred(Matchers.any[RequestHeader]()))
      .thenReturn(messages)

    when(mockMessagesAPI.apply(Matchers.any[String](), Matchers.any())(Matchers.any()))
      .thenReturn(MOCKED_MESSAGE)
  }
}