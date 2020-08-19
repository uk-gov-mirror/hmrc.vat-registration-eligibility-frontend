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

package config

import akka.actor.ActorSystem
import com.typesafe.config.{Config, ConfigFactory}
import javax.inject.Inject
import org.slf4j.{Logger, LoggerFactory}
import play.api.Environment
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.hooks.HttpHooks
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.config.LoadAuditingConfig
import uk.gov.hmrc.play.http.ws._

class FrontendAuditConnector @Inject()(config: FrontendAppConfig, environment: Environment) extends AuditConnector {
  override lazy val auditingConfig = LoadAuditingConfig(config.runModeConfiguration, environment.mode, s"auditing")
}

trait Hooks extends HttpHooks with HttpAuditing

trait WSHttp extends
  HttpGet with WSGet with
  HttpPut with WSPut with
  HttpPatch with WSPatch with
  HttpPost with WSPost with
  HttpDelete with WSDelete with Hooks

class Http @Inject()(config: FrontendAppConfig, frontendAuditConnector: FrontendAuditConnector, val actorSystem: ActorSystem) extends WSHttp {
  override val auditConnector = frontendAuditConnector
  override def appName = config.getString("appName")
  override val hooks   = Seq(AuditingHook)

  override def configuration: Option[Config] = Option(config.runModeConfiguration.underlying)
}

trait Logging {
  val logger: Logger = LoggerFactory.getLogger(getClass)
}