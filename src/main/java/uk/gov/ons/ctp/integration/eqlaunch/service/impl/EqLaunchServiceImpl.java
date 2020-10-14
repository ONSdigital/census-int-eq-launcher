package uk.gov.ons.ctp.integration.eqlaunch.service.impl;

import static uk.gov.ons.ctp.common.domain.Source.FIELD_SERVICE;

import com.godaddy.logging.Logger;
import com.godaddy.logging.LoggerFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bouncycastle.util.encoders.Hex;
import uk.gov.ons.ctp.common.domain.Source;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.error.CTPException.Fault;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.eqlaunch.crypto.Codec;
import uk.gov.ons.ctp.integration.eqlaunch.service.EqLaunchData;
import uk.gov.ons.ctp.integration.eqlaunch.service.EqLaunchService;

public class EqLaunchServiceImpl implements EqLaunchService {

  private static final Logger log = LoggerFactory.getLogger(EqLaunchServiceImpl.class);
  private static final String ROLE_FLUSHER = "flusher";
  private Codec codec = new Codec();

  @Override
  public String getEqLaunchJwe(
      EqLaunchData launchData,
      CaseContainerDTO caseContainer,
      String userId,
      String accountServiceUrl,
      String accountServiceLogoutUrl)
      throws CTPException {

    Map<String, Object> payload =
        createPayloadMap(
            launchData, caseContainer, userId, null, accountServiceUrl, accountServiceLogoutUrl);

    return codec.encrypt(payload, "authentication", launchData.getKeyStore());
  }

  @Override
  public String getEqFlushLaunchJwe(EqLaunchData launchData) throws CTPException {

    Map<String, Object> payload =
        createPayloadMap(launchData, null, null, ROLE_FLUSHER, null, null);

    return codec.encrypt(payload, "authentication", launchData.getKeyStore());
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
   * @param role
   * @param questionnaireId
   * @param accountServiceUrl
   * @param accountServiceLogoutUrl
   * @return
   * @throws CTPException
   */
  Map<String, Object> createPayloadMap(
      EqLaunchData launchData,
      CaseContainerDTO caseContainer,
      String userId,
      String role,
      String accountServiceUrl,
      String accountServiceLogoutUrl)
      throws CTPException {

    String questionnaireId = launchData.getQuestionnaireId();
    Source source = launchData.getSource();

    long currentTimeInSeconds = System.currentTimeMillis() / 1000;

    LinkedHashMap<String, Object> payload = new LinkedHashMap<>();

    payload.computeIfAbsent("jti", (k) -> UUID.randomUUID().toString());
    payload.computeIfAbsent("tx_id", (k) -> UUID.randomUUID().toString());
    payload.computeIfAbsent("iat", (k) -> currentTimeInSeconds);
    payload.computeIfAbsent("exp", (k) -> currentTimeInSeconds + (5 * 60));

    if (role == null || !role.equals(ROLE_FLUSHER)) {
      Objects.requireNonNull(
          caseContainer, "CaseContainer mandatory unless role is '" + ROLE_FLUSHER + "'");

      payload.computeIfAbsent("case_type", (k) -> caseContainer.getCaseType());
      validateCase(source, caseContainer, questionnaireId);
      String caseIdStr = caseContainer.getId().toString();
      payload.computeIfAbsent(
          "collection_exercise_sid", (k) -> caseContainer.getCollectionExerciseId().toString());
      String convertedRegionCode = convertRegionCode(caseContainer.getRegion());
      payload.computeIfAbsent("region_code", (k) -> convertedRegionCode);
      payload.computeIfAbsent(
          "ru_ref", (k) -> source == FIELD_SERVICE ? caseIdStr : caseContainer.getUprn());
      payload.computeIfAbsent("case_id", (k) -> caseIdStr);
      payload.computeIfAbsent(
          "display_address",
          (k) ->
              buildDisplayAddress(
                  caseContainer.getAddressLine1(),
                  caseContainer.getAddressLine2(),
                  caseContainer.getAddressLine3(),
                  caseContainer.getTownName(),
                  caseContainer.getPostcode()));
      payload.computeIfAbsent("survey", (k) -> caseContainer.getSurveyType());
    }
    String responseId = encryptResponseId(questionnaireId, launchData.getSalt());
    payload.computeIfAbsent("language_code", (k) -> launchData.getLanguage().getIsoLikeCode());
    payload.computeIfAbsent("response_id", (k) -> responseId);
    payload.computeIfAbsent("account_service_url", (k) -> accountServiceUrl);
    payload.computeIfAbsent("account_service_log_out_url", (k) -> accountServiceLogoutUrl);
    payload.computeIfAbsent("channel", (k) -> launchData.getChannel().name().toLowerCase());
    payload.computeIfAbsent("user_id", (k) -> userId);
    payload.computeIfAbsent("roles", (k) -> role);
    payload.computeIfAbsent("questionnaire_id", (k) -> questionnaireId);

    payload.computeIfAbsent("eq_id", (k) -> "census"); // hardcoded for rehearsal
    payload.computeIfAbsent("period_id", (k) -> "2019"); // hardcoded for rehearsal
    payload.computeIfAbsent("form_type", (k) -> launchData.getFormType());

    log.with("payload", payload).debug("Payload for EQ");

    return payload;
  }

  private void validateCase(Source source, CaseContainerDTO caseContainer, String questionnaireId)
      throws CTPException {
    UUID caseId = caseContainer.getId();

    verifyNotNull(caseContainer.getId(), "case id", caseId);
    verifyNotNull(caseContainer.getCaseType(), "case type", caseId);
    verifyNotNull(caseContainer.getCollectionExerciseId(), "collection id", caseId);
    verifyNotNull(questionnaireId, "questionnaireId", caseId);
    if (source != FIELD_SERVICE) {
      verifyNotNull(caseContainer.getUprn(), "address uprn", caseId);
    }
    verifyNotNull(caseContainer.getSurveyType(), "survey type", caseId);
  }

  private void verifyNotNull(Object fieldValue, String fieldName, UUID caseId) throws CTPException {
    if (fieldValue == null) {
      throw new CTPException(
          Fault.VALIDATION_FAILED,
          "No value supplied for " + fieldName + " field of case " + caseId);
    }
  }

  private String convertRegionCode(String caseRegionStr) throws CTPException {
    String regionValue = "GB-ENG";

    if (caseRegionStr != null) {
      char caseRegion = caseRegionStr.charAt(0);
      if (caseRegion == 'N') {
        regionValue = "GB-NIR";
      } else if (caseRegion == 'W') {
        regionValue = "GB-WLS";
      } else if (caseRegion == 'E') {
        regionValue = "GB-ENG";
      }
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

  // Creates encrypted response id from SALT and questionnaireId
  private String encryptResponseId(String questionnaireId, String salt) throws CTPException {
    StringBuilder responseId = new StringBuilder(questionnaireId);
    try {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update(salt.getBytes());
      byte[] bytes = md.digest(questionnaireId.getBytes());
      responseId.append((new String(Hex.encode(bytes)).substring(0, 16)));
    } catch (NoSuchAlgorithmException ex) {
      log.with(questionnaireId).error("No SHA-256 algorithm while encrypting questionnaire", ex);
      throw new CTPException(Fault.SYSTEM_ERROR, ex);
    }
    return responseId.toString();
  }
}
