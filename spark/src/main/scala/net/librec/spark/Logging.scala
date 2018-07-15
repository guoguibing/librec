/**
  * Copyright (C) 2016 LibRec
  * <p>
  * This file is part of LibRec.
  * LibRec is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  * <p>
  * LibRec is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
  * GNU General Public License for more details.
  * <p>
  * You should have received a copy of the GNU General Public License
  * along with LibRec. If not, see <http://www.gnu.org/licenses/>.
  */
package net.librec.spark

import org.apache.log4j.{Level, Logger}

/**
  * Utility trait for classes that want to log data. Creates a SLF4J logger for the class and allows
  * logging messages at different levels using methods that only evaluate parameters lazily if the
  * log level is enabled.
  */
trait Logging {

  /** Compute `expr` if debug is on, only */
  def debugDo[T](expr: => T)(implicit log: Logger): Option[T] = {
    if (log.isDebugEnabled) Some(expr)
    else None
  }

  /** Compute `expr` if trace is on, only */
  def traceDo[T](expr: => T)(implicit log: Logger): Option[T] = {
    if (log.isTraceEnabled) Some(expr) else None
  }

  /** Shorter, and lazy, versions of logging methods. Just declare log implicit. */
  def debug(msg: => AnyRef)(implicit log: Logger) { if (log.isDebugEnabled) log.debug(msg) }

  def debug(msg: => AnyRef, t: Throwable)(implicit log: Logger) { if (log.isDebugEnabled()) log.debug(msg, t) }

  /** Shorter, and lazy, versions of logging methods. Just declare log implicit. */
  def trace(msg: => AnyRef)(implicit log: Logger) { if (log.isTraceEnabled) log.trace(msg) }

  def trace(msg: => AnyRef, t: Throwable)(implicit log: Logger) { if (log.isTraceEnabled()) log.trace(msg, t) }

  def info(msg: => AnyRef)(implicit log: Logger) { if (log.isInfoEnabled) log.info(msg)}

  def info(msg: => AnyRef, t:Throwable)(implicit log: Logger) { if (log.isInfoEnabled) log.info(msg,t)}

  def warn(msg: => AnyRef)(implicit log: Logger) { if (log.isEnabledFor(Level.WARN)) log.warn(msg) }

  def warn(msg: => AnyRef, t: Throwable)(implicit log: Logger) { if (log.isEnabledFor(Level.WARN)) error(msg, t) }

  def error(msg: => AnyRef)(implicit log: Logger) { if (log.isEnabledFor(Level.ERROR)) log.warn(msg) }

  def error(msg: => AnyRef, t: Throwable)(implicit log: Logger) { if (log.isEnabledFor(Level.ERROR)) error(msg, t) }

  def fatal(msg: => AnyRef)(implicit log: Logger) { if (log.isEnabledFor(Level.FATAL)) log.fatal(msg) }

  def fatal(msg: => AnyRef, t: Throwable)(implicit log: Logger) { if (log.isEnabledFor(Level.FATAL)) log.fatal(msg, t) }

  def getLog(name: String): Logger = Logger.getLogger(name)

  def getLog(clazz: Class[_]): Logger = Logger.getLogger(clazz)

  def librecLog :Logger = getLog("net.librec.spark")

  def setLogLevel(l:Level)(implicit log:Logger) = {
    log.setLevel(l)
  }

  def setAdditivity(a:Boolean)(implicit log:Logger) = log.setAdditivity(a)

}
