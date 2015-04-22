package com.github.jodersky.skeybase
package verification

case class Service(name: String, username: String)
case class StatementBody(key: PublicKey, service: Service)
case class Statement(body: StatementBody)