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

package forms

import models.view.VoluntaryRegistration
import uk.gov.hmrc.play.test.UnitSpec
import helpers.FormInspectors._
/**
  * Created by eric on 28/09/17.
  */
class VoluntaryRegistrationFormSpec extends UnitSpec {
  val testForm = VoluntaryRegistrationForm.form

  "binding a VoluntaryRegistration to a form" should {
    "bind sucessfully if answer is 'NO'" in {
      val data = Map(VoluntaryRegistrationForm.RADIO_YES_NO -> VoluntaryRegistration.REGISTER_NO)
      val model = VoluntaryRegistration(VoluntaryRegistration.REGISTER_NO)
      val boundForm = testForm.bind(data).fold(
        errors => errors,
        success => success
      )
      boundForm shouldBe model
    }

    "bind sucessfully if answer is 'YES'" in {
      val data = Map(VoluntaryRegistrationForm.RADIO_YES_NO -> VoluntaryRegistration.REGISTER_YES)
      val model = VoluntaryRegistration(VoluntaryRegistration.REGISTER_YES)
      val boundForm = testForm.bind(data).fold(
        errors => errors,
        success => success
      )
      boundForm shouldBe model
    }

    "should have errors if no field selected" in {
      val data = Map(VoluntaryRegistrationForm.RADIO_YES_NO -> "")

      val boundForm = testForm.bind(data)

      boundForm shouldHaveErrors Seq(VoluntaryRegistrationForm.RADIO_YES_NO -> "error.unknown")
    }

    "should have errors if an invalid reason is given" in {
      val data = Map(VoluntaryRegistrationForm.RADIO_YES_NO -> "skdfsdsdf")
      val boundForm = testForm.bind(data)
      boundForm shouldHaveErrors Seq(VoluntaryRegistrationForm.RADIO_YES_NO -> "error.unknown")
    }
  }

  "unbinding a VoluntaryRegistration from form" should {
    "unbind successfully for Yes" in {
      val data = Map(VoluntaryRegistrationForm.RADIO_YES_NO -> VoluntaryRegistration.REGISTER_YES)
      val model = VoluntaryRegistration(VoluntaryRegistration.REGISTER_YES)
      testForm.fill(model).data shouldBe data
    }
    "unbind sucessfully for No" in {
      val data = Map(VoluntaryRegistrationForm.RADIO_YES_NO -> VoluntaryRegistration.REGISTER_NO)
      val model = VoluntaryRegistration(VoluntaryRegistration.REGISTER_NO)
      testForm.fill(model).data shouldBe data
    }
  }
}
