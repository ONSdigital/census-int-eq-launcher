package uk.gov.ons.ctp.integration.eqlaunch.crypto;

import java.util.Map;
import uk.gov.ons.ctp.common.error.CTPException;

/**
 * Should be an implementation according to what is required by EQ of the JavaScript Object Signing
 * and Encryption (JOSE IETF) standards for creating a JWS token and encrypting it as the payload of
 * a JWE.
 */
public interface EQJOSEProvider {

  /**
   * Produce signed JWS for a set of claims and encrypt as the payload of a JWE.
   *
   * @param claims payload to be sent
   * @param keyPurpose e.g. authentication
   * @param keyStore Store of asymmetric keys for signing and encryption
   * @return String representing encrypted JWE token
   * @throws CTPException on error
   */
  String encrypt(Map<String, Object> claims, String keyPurpose, KeyStore keyStore)
      throws CTPException;

  /**
   * Decrypt a JWE token extracting the JWS payload, verify the signed JWS payload and decode.
   *
   * @param jwe JSON Web Encrypted token to decrypt.
   * @param keyStore Store of asymmetric keys for signing and encryption
   * @return String representing JWE payload
   * @throws CTPException on error
   */
  String decrypt(String jwe, KeyStore keyStore) throws CTPException;
}
