package uk.gov.ons.ctp.integration.eqlaunch.service;

import uk.gov.ons.ctp.common.error.CTPException;
import uk.gov.ons.ctp.integration.caseapiclient.caseservice.model.CaseContainerDTO;

public interface EqLaunchService {

  String getEqLaunchJwe(
      EqLaunchData launchData,
      CaseContainerDTO caseContainer,
      String userId,
      String accountServiceUrl,
      String accountServiceLogoutUrl)
      throws CTPException;

  String getEqFlushLaunchJwe(EqLaunchData launchData) throws CTPException;
}
