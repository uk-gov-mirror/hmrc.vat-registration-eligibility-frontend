/*
 * Copyright 2021 HM Revenue & Customs
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

import javax.inject.Inject

sealed trait FeatureSwitch {
  def name: String
  def enabled: Boolean
  def value: String
}

case class BooleanFeatureSwitch(name: String, enabled: Boolean) extends FeatureSwitch {
  override def value = ""
}

class FeatureSwitchManager extends FeatureManager {
  override val datePatternRegex = """([12]\d{3}-(0[1-9]|1[0-2])-(0[1-9]|[12]\d|3[01]))T(0[0-9]|1[0-9]|2[0-3]):([0-59]\d):([0-59]\d)"""
}

case class ValueSetFeatureSwitch(name: String, setValue: String) extends FeatureSwitch {
  override def enabled = true
  override def value   = setValue
}

trait FeatureManager {

  private[utils] def systemPropertyName(name: String) = s"feature.$name"

  val datePatternRegex: String

  private[utils] def getProperty(name: String): FeatureSwitch = {
    val value = sys.props.get(systemPropertyName(name))

    value match {
      case Some("true")                                 => BooleanFeatureSwitch(name, enabled = true)
      case Some("time-clear")                           => ValueSetFeatureSwitch(name, "time-clear")
      case Some(date) if date.matches(datePatternRegex) => ValueSetFeatureSwitch(name, date)
      case _                                            => BooleanFeatureSwitch(name, enabled = false)
    }
  }

  private[utils] def setProperty(name: String, value: String): FeatureSwitch = {
    sys.props += ((systemPropertyName(name), value))
    getProperty(name)
  }

  def enable(fs: FeatureSwitch): FeatureSwitch  = setProperty(fs.name, "true")
  def disable(fs: FeatureSwitch): FeatureSwitch = setProperty(fs.name, "false")
  def setSystemDate(fs: FeatureSwitch): FeatureSwitch   = setProperty(fs.name, fs.value)
  def clearSystemDate(fs: FeatureSwitch): FeatureSwitch = setProperty(fs.name, "")
}

class VATFeatureSwitchImpl @Inject()(injManager: FeatureSwitchManager) extends VATFeatureSwitch {
  override val manager: FeatureManager = injManager
  override val crStubbed: String       = "crStubbed"
  override val iiStubbed: String       = "iiStubbed"
  override val setSystemDate           = "system-date"

}

trait VATFeatureSwitch {
  val crStubbed: String
  val iiStubbed: String
  val setSystemDate: String
  val manager: FeatureManager

  def useCrStubbed: FeatureSwitch = manager.getProperty(crStubbed)
  def useIiStubbed: FeatureSwitch = manager.getProperty(iiStubbed)
  def systemDate: FeatureSwitch   = manager.getProperty(setSystemDate)

  def apply(name: String): Option[FeatureSwitch] = name match {
    case `crStubbed`     => Some(useCrStubbed)
    case `iiStubbed`     => Some(useIiStubbed)
    case `setSystemDate` => Some(systemDate)
    case _               => None
  }

  lazy val all = Seq(
    useCrStubbed,
    useIiStubbed,
    systemDate
  )
}