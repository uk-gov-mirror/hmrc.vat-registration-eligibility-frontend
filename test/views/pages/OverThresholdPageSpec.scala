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

package views.pages

import java.time.LocalDate

import forms.OverThresholdFormFactory
import models.MonthYearModel
import models.MonthYearModel.FORMAT_DD_MMMM_Y
import models.view.OverThresholdView
import org.jsoup.Jsoup
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.inject.Injector
import play.api.test.FakeRequest
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}
import views.html.pages.{over_threshold => overThresholdPage}

class OverThresholdPageSpec extends UnitSpec with WithFakeApplication with I18nSupport {
  implicit val request = FakeRequest()
  val injector : Injector = fakeApplication.injector
  implicit val messagesApi : MessagesApi = injector.instanceOf[MessagesApi]

  val incorpDate = LocalDate.of(2016, 8, 5)
  val sIncorpDate = incorpDate.format(FORMAT_DD_MMMM_Y)

  val model = OverThresholdView(true, Some(MonthYearModel("9", "2016")).flatMap(_.toLocalDate))

  lazy val form = OverThresholdFormFactory.form(incorpDate).fill(model)
  lazy val view = overThresholdPage(form, sIncorpDate)
  lazy val document = Jsoup.parse(view.body)

  "Over threshold page" should {
    "display the correct title with the incorporation date" in {
      document.getElementById("pageHeading").text shouldBe messagesApi("pages.thresholdQuestion1.heading", sIncorpDate)
    }

    "pre-select the correct radio" in {
      document.getElementById(s"${OverThresholdFormFactory.RADIO_YES_NO}-true").attr("checked") shouldBe "checked"
    }

    "display the correct prepopulated date" in {
      document.getElementsByClass("form-control").get(0).attr("value") shouldBe "9"
      document.getElementsByClass("form-control").get(1).attr("value") shouldBe "2016"
    }
  }
}
