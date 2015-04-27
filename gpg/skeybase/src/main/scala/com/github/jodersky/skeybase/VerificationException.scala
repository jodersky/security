package com.github.jodersky.skeybase

/** 
 * Thrown when the verification of a proof fails. This exception may only be thrown in case of an error
 * during verification (such as an invalid signature), NOT in related circumstances (such as an offline service
 * or missing resource).
 */
class VerificationException(message: String) extends RuntimeException(message)