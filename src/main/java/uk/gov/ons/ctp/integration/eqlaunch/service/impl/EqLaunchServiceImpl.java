package uk.gov.ons.ctp.integration.eqlaunch.service.impl;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.model.Channel;
import uk.gov.ons.ctp.common.model.Language;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.eqlaunch.crypto.KeyStore;
import uk.gov.ons.ctp.integration.eqlaunch.service.EqLaunchService;

public class EqLaunchServiceImpl implements EqLaunchService {

  public String getEqLaunchJwe(
      Language language,
      Channel channel,
      CaseContainerDTO caseContainer,
      String userId,
      String questionnaireId,
      String accountServiceUrl,
      String accountServiceLogoutUrl,
      KeyStore keyStore)
      throws CTPException {

    String payload =
        createPayloadString(
            language,
            channel,
            caseContainer,
            userId,
            questionnaireId,
            accountServiceUrl,
            accountServiceLogoutUrl);

    return createPayloadJwe(payload, keyStore);
  }

  /**
   * This method builds the payload of a URL that will be used to launch EQ. This code replicates
   * the payload building done by the Python code in the census-rh-ui project for class /app/eq.py.
   *
   * <p>EQ requires a payload string formatted as a Python serialised dictionary, so this code has
   * to replicate all Python formatting quirks.
   *
   * <p>This code assumes that the channel is CC or field, and will need the user_id field to be
   * cleared if it is ever used from RH.
   *
   * @param language
   * @param channel
   * @param caseContainer
   * @param userId
   * @param questionnaireId
   * @param accountServiceUrl
   * @param accountServiceLogoutUrl
   * @return
   * @throws CTPException
   */
  String createPayloadString(
      Language language,
      Channel channel,
      CaseContainerDTO caseContainer,
      String userId,
      String questionnaireId,
      String accountServiceUrl,
      String accountServiceLogoutUrl)
      throws CTPException {

    validateCase(caseContainer, questionnaireId);

    long currentTimeInSeconds = System.currentTimeMillis() / 1000;

    PythonDictionary dictionary = new PythonDictionary();

    dictionary.put("jti", UUID.randomUUID().toString());
    dictionary.put("tx_id", UUID.randomUUID().toString());
    dictionary.put("iat", currentTimeInSeconds);
    dictionary.put("exp", currentTimeInSeconds + (5 * 60));
    dictionary.put("case_type", caseContainer.getCaseType());
    dictionary.put("collection_exercise_sid", caseContainer.getCollectionExerciseId());
    dictionary.put("region_code", convertRegionCode(caseContainer.getRegion().charAt(0)));
    dictionary.put("ru_ref", caseContainer.getUprn());
    dictionary.put("case_id", caseContainer.getId());
    dictionary.put("language_code", language.getIsoLikeCode());
    dictionary.put(
        "display_address",
        buildDisplayAddress(
            caseContainer.getAddressLine1(),
            caseContainer.getAddressLine2(),
            caseContainer.getAddressLine3(),
            caseContainer.getTownName(),
            caseContainer.getPostcode()));
    dictionary.put("response_id", questionnaireId);
    dictionary.put("account_service_url", accountServiceUrl);
    dictionary.put("account_service_log_out_url", accountServiceLogoutUrl);
    dictionary.put("channel", channel.name().toLowerCase());
    dictionary.put("user_id", userId);
    dictionary.put("questionnaire_id", questionnaireId);
    dictionary.put("eq_id", "census"); // hardcoded for rehearsal
    dictionary.put("period_id", "2019"); // hardcoded for rehearsal
    dictionary.put("form_type", "individual_gb_eng"); // hardcoded for rehearsal
    dictionary.put("survey", caseContainer.getSurveyType());

    return dictionary.toPythonSerialisedString();
  }

  private void validateCase(CaseContainerDTO caseContainer, String questionnaireId)
      throws CTPException {
    UUID caseId = caseContainer.getId();

    verifyNotNull(caseContainer.getId(), "case id", caseId);
    verifyNotNull(caseContainer.getCaseType(), "case type", caseId);
    verifyNotNull(caseContainer.getCollectionExerciseId(), "collection id", caseId);
    verifyNotNull(questionnaireId, "questionnaireId", caseId);
    verifyNotNull(caseContainer.getUprn(), "address uprn", caseId);
    verifyNotNull(caseContainer.getRegion(), "region", caseId);
    verifyNotNull(caseContainer.getSurveyType(), "survey type", caseId);
  }

  private void verifyNotNull(Object fieldValue, String fieldName, UUID caseId) throws CTPException {
    if (fieldValue == null) {
      throw new CTPException(
          Fault.VALIDATION_FAILED,
          "No value supplied for " + fieldName + " field of case " + caseId);
    }
  }

  private String convertRegionCode(char caseRegion) throws CTPException {
    String regionValue;

    if (caseRegion == 'N') {
      regionValue = "GB-NIR";
    } else if (caseRegion == 'W') {
      regionValue = "GB-WLS";
    } else if (caseRegion == 'E') {
      regionValue = "GB-ENG";
    } else {
      throw new CTPException(Fault.VALIDATION_FAILED, "Unknown region code: " + caseRegion);
    }

    return regionValue;
  }

  // Create an address from the first 2 non-null parts of the address.
  // This replicates RHUI's creation of the display address.
  private String buildDisplayAddress(String... addressElements) {
    String displayAddress =
        Arrays.stream(addressElements)
            .filter(a -> a != null)
            .limit(2)
            .collect(Collectors.joining(", "));

    return displayAddress;
  }

  private String createPayloadJwe(String payload, KeyStore keystore) {
    return "bar";
  }
}
