package uk.gov.ons.ctp.integration.eqlaunch.service.impl;

import uk.gov.ons.ctp.common.model.Channel;
import uk.gov.ons.ctp.common.model.Language;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;
import uk.gov.ons.ctp.integration.eqlaunch.service.EqLaunchService;

public class EqLaunchServiceImpl implements EqLaunchService {

  public String getEqLaunchJwe(
      Language language,
      Channel channel,
      CaseContainerDTO caseContainer,
      String userId,
      String questionnaireId,
      String accountServiceUrl,
      String accountServiceLogoutUrl) {

    String payload =
        createPayloadString(
            language,
            channel,
            caseContainer,
            userId,
            questionnaireId,
            accountServiceUrl,
            accountServiceLogoutUrl);

    return createPayloadJwe(payload);
  }

  private String createPayloadString(
      Language language,
      Channel channel,
      CaseContainerDTO caseContainer,
      String userId,
      String questionnaireId,
      String accountServiceUrl,
      String accountServiceLogoutUrl) {

    StringBuilder payloadBuilder = new StringBuilder();

    return payloadBuilder.toString();
  }

  private String createPayloadJwe(String payload) {
    return "bar";
  }
}
