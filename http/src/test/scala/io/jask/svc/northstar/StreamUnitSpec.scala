package io.jask.svc.northstar

import akka.testkit.TestKitBase
import org.scalatest._

abstract class StreamUnitSpec
    extends AsyncFlatSpec
    with AkkaGlobals
    with Matchers
    with TestKitBase {}
