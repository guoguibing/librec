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

import org.scalatest.{BeforeAndAfterAll, FunSuite}

/**
  * Base Test Suite
  * @author WangYuFeng
  */
class BaseTestSuite extends FunSuite with BeforeAndAfterAll {
  //netstat -ano|findstr 4040
  //taskkill -pid 6916 -f
  val appName = "Librec Test"
  val master = "local[2]"
  var conf:LibrecConf = _
  var lc:LibrecContext = _

  override protected def beforeAll(): Unit = {
    conf = new LibrecConf().setMaster(master).setAppName(appName)
    lc = new LibrecContext(conf)
    super.beforeAll()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
  }

}