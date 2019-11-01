package uk.gov.ons.ctp.integration.eqlaunch.crypto;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import com.nimbusds.jose.JWSObject;
import java.util.Map;
import java.util.Optional;
import uk.gov.ons.ctp.common.error.CTPException;

/**
 * Implementation to sign a set of claims as a JSON Web Signature (JWS) and encrypt the JWS as the
 * payload of a JSON Web Encryption (JWE)
 */
public class Codec implements EQJOSEProvider {

  private static final Logger log = LoggerFactory.getLogger(Codec.class);

  private static String KEYTYPE_PRIVATE = "private";
  private static String KEYTYPE_PUBLIC = "public";

  private JWSHelper jwsHelper = new JWSHelper();
  private JWEHelper jweHelper = new JWEHelper();

  /**
   * Implementation to produce signed JWS for a set of claims and encrypt as the payload of a JWE.
   */
  public String encrypt(Map<String, Object> claims, String keyPurpose, KeyStore keyStore)
      throws CTPException {

    Optional<Key> privateKey = keyStore.getKeyForPurposeAndType(keyPurpose, KEYTYPE_PRIVATE);
    JWSObject jws;
    if (privateKey.isPresent()) {
      jws = jwsHelper.encode(claims, privateKey.get());
    } else {
      log.with("keyPurpose", keyPurpose)
          .with("keyType", KEYTYPE_PRIVATE)
          .error("Failed to retrieve key to sign claims");
      throw new CTPException(
          CTPException.Fault.SYSTEM_ERROR, "Failed to retrieve private key to sign claims");
    }

    Optional<Key> publicKey = keyStore.getKeyForPurposeAndType(keyPurpose, KEYTYPE_PUBLIC);
    if (publicKey.isPresent()) {
      return jweHelper.encrypt(jws, publicKey.get());
    } else {
      log.with("keyPurpose", keyPurpose)
          .with("keyType", KEYTYPE_PUBLIC)
          .error("Failed to retrieve key to encode payload");
      throw new CTPException(
          CTPException.Fault.SYSTEM_ERROR, "Failed to retrieve public key to encode payload");
    }
  }

  /** Implementation to extract the JWS payload of a JWE, verify the JWS and return it's payload. */
  public String decrypt(String jwe, KeyStore keyStore) throws CTPException {

    Optional<Key> publicKey = keyStore.getKeyById(jweHelper.getKid(jwe));
    JWSObject jws;
    if (publicKey.isPresent()) {
      jws = jweHelper.decrypt(jwe, publicKey.get());
    } else {
      log.with("kid", jweHelper.getKid(jwe)).error("Failed to retrieve public key to decrypt JWE");
      throw new CTPException(
          CTPException.Fault.SYSTEM_ERROR, "Failed to retrieve public key to decrypt JWE");
    }

    Optional<Key> privateKey = keyStore.getKeyById(jwsHelper.getKid(jws));
    if (privateKey.isPresent()) {
      return jwsHelper.decode(jws, privateKey.get());
    } else {
      log.with("kid", jwsHelper.getKid(jws)).error("Failed to retrieve private key to verify JWS");
      throw new CTPException(
          CTPException.Fault.SYSTEM_ERROR, "Failed to retrieve private key to verify JWS");
    }
  }
}
