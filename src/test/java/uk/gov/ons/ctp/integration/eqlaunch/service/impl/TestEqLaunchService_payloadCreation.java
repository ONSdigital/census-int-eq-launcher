package uk.gov.ons.ctp.integration.eqlaunch.service.impl;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.UUID;
import org.junit.Test;
import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.common.model.Channel;
import uk.gov.ons.ctp.common.model.Language;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;

public class TestEqLaunchService_payloadCreation {

  @Test
  public void createPayloadString() throws CTPException {
    EqLaunchServiceImpl eqLaunchService = new EqLaunchServiceImpl();

    Language language = Language.ENGLISH;
    Channel channel = Channel.FIELD;
    CaseContainerDTO caseContainer = new CaseContainerDTO();
    String userId = "1234567890";
    String questionnaireId = "11100000009";
    String accountServiceUrl = "http://localhost:9092/start";
    String accountServiceLogoutUrl = "http://localhost:9092/start/save-and-exit";

    UUID collectionExerciseId = UUID.randomUUID();
    String uprn = "10023122451";
    UUID caseId = UUID.randomUUID();

    caseContainer.setCaseType("H");
    caseContainer.setCollectionExerciseId(collectionExerciseId);
    caseContainer.setRegion("E");
    caseContainer.setUprn(uprn); // for the 'ru_ref' field
    caseContainer.setId(caseId);
    caseContainer.setAddressLine1("ONS");
    caseContainer.setAddressLine2(null);
    caseContainer.setAddressLine3("Segensworth's Road");
    caseContainer.setTownName("Titchfield");
    caseContainer.setPostcode("PO15 5RR");
    caseContainer.setSurveyType("CENSUS");

    // Run code under to test to get a payload. Then replace elements which change on each run with
    // fixed text
    Map<String, String> payloadMap =
        eqLaunchService.createPayloadString(
            language,
            channel,
            caseContainer,
            userId,
            questionnaireId,
            accountServiceUrl,
            accountServiceLogoutUrl);

    String payload = payloadMap.toString();
    payload = cleanUUID(payload, "jti=", "88888888-8888-8888-8888-888888888888");
    payload = cleanUUID(payload, "tx_id=", "88888888-8888-8888-8888-888888888888");
    payload = cleanTimestamp(payload, "iat=", "12345");
    payload = cleanTimestamp(payload, "exp=", "12345");

    StringBuilder expectedPayload = new StringBuilder();
    expectedPayload.append("{");
    expectedPayload.append("jti=88888888-8888-8888-8888-888888888888, ");
    expectedPayload.append("tx_id=88888888-8888-8888-8888-888888888888, ");
    expectedPayload.append("iat=12345, ");
    expectedPayload.append("exp=12345, ");
    expectedPayload.append("case_type=H, ");
    expectedPayload.append("collection_exercise_sid=" + collectionExerciseId.toString() + ", ");
    expectedPayload.append("region_code=GB-ENG, ");
    expectedPayload.append("ru_ref=" + uprn + ", ");
    expectedPayload.append("case_id=" + caseId + ", ");
    expectedPayload.append("language_code=en, ");
    expectedPayload.append("display_address=ONS, Segensworth\'s Road, ");
    expectedPayload.append("response_id=11100000009, ");
    expectedPayload.append("account_service_url=http://localhost:9092/start, ");
    expectedPayload.append(
        "account_service_log_out_url=http://localhost:9092/start/save-and-exit, ");
    expectedPayload.append("channel=field, ");
    expectedPayload.append("user_id=1234567890, ");
    expectedPayload.append("questionnaire_id=11100000009, ");
    expectedPayload.append("eq_id=census, ");
    expectedPayload.append("period_id=2019, ");
    expectedPayload.append("form_type=individual_gb_eng, ");
    expectedPayload.append("survey=CENSUS");
    expectedPayload.append("}");
    String expected = expectedPayload.toString();

    assertEquals(expected, payload);
  }

  private String cleanUUID(String payload, String prefix, String replacement) {
    return payload.replaceAll(
        prefix + "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}",
        prefix + replacement);
  }

  private String cleanTimestamp(String payload, String prefix, String replacement) {
    return payload.replaceAll(prefix + "[0-9]*", prefix + replacement);
  }
}
