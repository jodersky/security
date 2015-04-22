package com.github.jodersky.skeybase
package openpgp

import scala.util.Try

trait Backend {

  /** Imports a key into this backend so that it is available for verification. */
  def importKey(key: String): Unit

  /**
   * verifies a signed statement.
   * @param signed the statement to verify
   * @param fingerprint the fingerprint of the key that allegedly signed this statement
   */
  def verifySignature(signed: String, fingerprint: String): Try[String]

}