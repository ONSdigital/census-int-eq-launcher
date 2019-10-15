package uk.gov.ons.ctp.integration.eqlaunch.service;

import uk.gov.ons.ctp.common.model.Channel;
import uk.gov.ons.ctp.common.model.Language;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;

public interface EqLaunchService {

  String getEqLaunchJwe(
      Language language,
      Channel channel,
      CaseContainerDTO caseContainer,
      String userId,
      String accountServiceUrl,
      String accountServiceLogoutUrl);
}
