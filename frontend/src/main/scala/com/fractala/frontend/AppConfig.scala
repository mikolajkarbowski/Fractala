package com.fractala.frontend

import org.scalajs.dom
import scala.scalajs.js

/** Frontend runtime configuration.
  *
  * Values are injected by `config.js` (from Vite env vars) before the app starts and read here.
  */
object AppConfig:

  val apiBaseUrl: String =
    val configured = dom.window.asInstanceOf[js.Dynamic].__FRACTALA_API__
    if js.typeOf(configured) == "string" && configured.asInstanceOf[String].nonEmpty then
      configured.asInstanceOf[String]
    else "http://localhost:9000"
