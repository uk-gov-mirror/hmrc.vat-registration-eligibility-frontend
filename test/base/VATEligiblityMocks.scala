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

import config.WSHttp
import connectors._
import org.scalatest.mockito.MockitoSugar
import play.api.i18n.MessagesApi
import services._
import utils.VATFeatureSwitch


trait VATEligiblityMocks {
  self: MockitoSugar =>

  //Connectors
  lazy val mockDataCacheConnector    = mock[DataCacheConnector]
  lazy val mockVatRegConnector       = mock[VatRegistrationConnector]

  //Services
  lazy val mockVRService             = mock[VatRegistrationService]
  lazy val mockCurrentProfileService = mock[CurrentProfileService]
  lazy val mockThresholdService      = mock[ThresholdService]

  //Other
  lazy val mockWSHttp                = mock[WSHttp]
  lazy val mockVATFeatureSwitch      = mock[VATFeatureSwitch]
  lazy val mockMessagesAPI           = mock[MessagesApi]
}
