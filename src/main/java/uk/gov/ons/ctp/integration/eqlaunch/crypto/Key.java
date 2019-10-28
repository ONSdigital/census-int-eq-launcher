package uk.gov.ons.ctp.integration.eqlaunch.crypto;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import java.io.IOException;
import java.io.StringReader;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import lombok.Data;
import org.bouncycastle.util.io.pem.PemReader;
import uk.gov.ons.ctp.common.error.CTPException;

@Data
/** Cryptographic Key model object */
public class Key {

  private static final Logger log = LoggerFactory.getLogger(Key.class);

  private String kid;
  private String purpose;
  private String type;
  private String value;

  /**
   * Return a PrivateKey from a Key
   *
   * @param algorithm encryption algorithm
   * @return PrivateKey
   * @throws CTPException
   */
  public PrivateKey getPrivateKey(String algorithm) throws CTPException {

    byte[] keyBytes = parsePem(value);
    java.security.Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

    PrivateKey privateKey = null;
    try {
      KeyFactory kf = KeyFactory.getInstance(algorithm);
      EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
      privateKey = kf.generatePrivate(keySpec);
    } catch (NoSuchAlgorithmException ex) {
      log.error(
          "Could not reconstruct the private key, the given algorithm could not be found", ex);
      throw new CTPException(
          CTPException.Fault.SYSTEM_ERROR,
          "Could not reconstruct the private key, the given algorithm could not be found",
          ex);
    } catch (InvalidKeySpecException ex) {
      log.error("Could not reconstruct the private key");
      throw new CTPException(
          CTPException.Fault.SYSTEM_ERROR, "Could not reconstruct the private key", ex);
    }

    return privateKey;
  }

  /**
   * Return a JWK from Pem-encoded string
   *
   * @throws CTPException
   */
  public JWK getJWK() throws CTPException {
    try {
      return JWK.parseFromPEMEncodedObjects(value);
    } catch (JOSEException ex) {
      log.error("Could not parse private key", ex);
      throw new CTPException(CTPException.Fault.SYSTEM_ERROR, "Could not parse private key", ex);
    }
  }

  /** Return well formed pem encoded key */
  private byte[] parsePem(String keyValue) throws CTPException {
    final PemReader reader = new PemReader(new StringReader(keyValue));
    try {
      byte[] parsedKey = reader.readPemObject().getContent();
      return parsedKey;
    } catch (Exception ex) {
      log.error("Could not parse private key", ex);
      throw new CTPException(CTPException.Fault.SYSTEM_ERROR, "Could not parse private key", ex);
    } finally {
      try {
        reader.close();
      } catch (IOException e) {
        log.error("Could not close key reader");
        throw new CTPException(CTPException.Fault.SYSTEM_ERROR, "Could not close key reader");
      }
    }
  }
}
