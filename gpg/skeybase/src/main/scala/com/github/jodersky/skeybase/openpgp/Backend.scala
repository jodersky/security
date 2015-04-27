package com.github.jodersky.skeybase
package openpgp

import scala.util.Try

trait Backend {

  /** Imports a key into this backend so that it is available for verification. */
  def importKey(key: String): Unit

  /**
   * Verifies a signed message.
   * @param signed the message to verify
   * @param fingerprint the fingerprint of the key that allegedly signed this statement
   */
  def verifySignature(signed: String, fingerprint: String): Try[String]

}