package uk.gov.ons.ctp.launcher.crypto;

import java.util.Map;
import lombok.Data;

/** Holder for Cryptographic keys */
@Data
public class Keys {

  private Map<String, Key> keys;
}
