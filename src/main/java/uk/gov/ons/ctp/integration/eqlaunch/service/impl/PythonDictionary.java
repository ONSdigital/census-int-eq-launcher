package uk.gov.ons.ctp.integration.eqlaunch.service.impl;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;

/**
 * This class simulates a Python dictionary.
 *
 * <p>The caller adds name/value pairs until they require a serialised version of the contents. To
 * replicate the Python behaviour backslashes and single quotes are escaped with a backslash.
 */
public class PythonDictionary {
  private LinkedHashMap<String, Object> payloadAttributes = new LinkedHashMap<>();

  public void put(String name, String value) {
    payloadAttributes.put(name, value);
  }

  public void put(String name, Long value) {
    payloadAttributes.put(name, value);
  }

  public void put(String name, UUID value) {
    payloadAttributes.put(name, value);
  }

  public String toPythonSerialisedString() throws CTPException {
    StringBuilder serialised = new StringBuilder();

    for (Map.Entry<String, Object> entry : payloadAttributes.entrySet()) {
      if (serialised.length() > 0) {
        serialised.append(", ");
      }

      String serialisedName = "'" + entry.getKey() + "'";

      Object value = entry.getValue();
      if (value == null) {
        throw new CTPException(
            Fault.BAD_REQUEST, "Null value found in dictionary for key: " + serialisedName);
      }

      String valueAsString = asPythonString(value.toString());
      String serialisedValue;
      if (value instanceof String) {
        serialisedValue = "'" + valueAsString + "'";
      } else if (value instanceof UUID) {
        serialisedValue = "'" + valueAsString + "'";
      } else if (value instanceof Long) {
        serialisedValue = valueAsString;
      } else {
        throw new CTPException(
            Fault.BAD_REQUEST,
            "Unknown type of value object found for key "
                + serialisedName
                + " in dictionary: "
                + value.getClass().getCanonicalName());
      }

      serialised.append(serialisedName + ": " + serialisedValue);
    }

    return "{" + serialised.toString() + "}";
  }

  // Returns a Python representation of a String
  private String asPythonString(String javaString) {
    String pythonString = javaString.replace("\\", "\\\\").replace("'", "\\'");
    return pythonString;
  }
}
