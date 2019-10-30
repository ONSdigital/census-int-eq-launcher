package uk.gov.ons.ctp.integration.eqlaunch.service.impl;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.common.model.Channel;
import uk.gov.ons.ctp.common.model.Language;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.eqlaunch.crypto.Codec;
import uk.gov.ons.ctp.integration.eqlaunch.crypto.KeyStore;
import uk.gov.ons.ctp.integration.eqlaunch.service.EqLaunchService;

public class EqLaunchServiceImpl implements EqLaunchService {

  private Codec codec = new Codec();

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

    Map<String, String> payload =
        createPayloadString(
            language,
            channel,
            caseContainer,
            userId,
            questionnaireId,
            accountServiceUrl,
            accountServiceLogoutUrl);

    return codec.encrypt(payload, "authenticate", keyStore);
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
  Map<String, String> createPayloadString(
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

    LinkedHashMap<String, String> payload = new LinkedHashMap<>();

    payload.put("jti", UUID.randomUUID().toString());
    payload.put("tx_id", UUID.randomUUID().toString());
    payload.put("iat", Long.toString(currentTimeInSeconds));
    payload.put("exp", Long.toString(currentTimeInSeconds + (5 * 60)));
    payload.put("case_type", caseContainer.getCaseType());
    payload.put("collection_exercise_sid", caseContainer.getCollectionExerciseId().toString());
    payload.put("region_code", convertRegionCode(caseContainer.getRegion().charAt(0)));
    payload.put("ru_ref", caseContainer.getUprn());
    payload.put("case_id", caseContainer.getId().toString());
    payload.put("language_code", language.getIsoLikeCode());
    payload.put(
        "display_address",
        buildDisplayAddress(
            caseContainer.getAddressLine1(),
            caseContainer.getAddressLine2(),
            caseContainer.getAddressLine3(),
            caseContainer.getTownName(),
            caseContainer.getPostcode()));
    payload.put("response_id", questionnaireId);
    payload.put("account_service_url", accountServiceUrl);
    payload.put("account_service_log_out_url", accountServiceLogoutUrl);
    payload.put("channel", channel.name().toLowerCase());
    payload.put("user_id", userId);
    payload.put("questionnaire_id", questionnaireId);
    payload.put("eq_id", "census"); // hardcoded for rehearsal
    payload.put("period_id", "2019"); // hardcoded for rehearsal
    payload.put("form_type", "individual_gb_eng"); // hardcoded for rehearsal
    payload.put("survey", caseContainer.getSurveyType());

    return payload;
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
}
