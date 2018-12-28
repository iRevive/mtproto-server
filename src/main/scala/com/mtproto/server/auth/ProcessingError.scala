package com.mtproto.server.auth

import scala.util.control.NoStackTrace

final case class ProcessingError(cause: String) extends Exception with NoStackTrace
