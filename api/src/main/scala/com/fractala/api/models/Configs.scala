package com.fractala.api.models

import pureconfig.ConfigReader
import pureconfig.generic.derivation.default.*

case class ServerConfig(host: String, port: Int) derives ConfigReader

case class AppConfig(server: ServerConfig) derives ConfigReader
