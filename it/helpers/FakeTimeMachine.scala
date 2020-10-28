package helpers

import java.time.LocalDate

import utils.TimeMachine

class FakeTimeMachine extends TimeMachine {
  override def today: LocalDate = LocalDate.of(2020, 1, 1)
}
