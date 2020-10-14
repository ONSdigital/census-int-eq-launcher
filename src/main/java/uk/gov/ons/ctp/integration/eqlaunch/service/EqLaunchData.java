package uk.gov.ons.ctp.integration.eqlaunch.service;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import uk.gov.ons.ctp.common.domain.Channel;
import uk.gov.ons.ctp.common.domain.Language;
import uk.gov.ons.ctp.common.domain.Source;
import uk.gov.ons.ctp.integration.eqlaunch.crypto.KeyStore;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Data
public class EqLaunchData {
  @NonNull private Language language;
  @NonNull private Source source;
  @NonNull private Channel channel;
  @NonNull private String questionnaireId;
  @NonNull private String formType;
  @NonNull private KeyStore keyStore;
  @NonNull private String salt;
}
