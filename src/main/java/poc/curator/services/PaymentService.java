package poc.curator.services;

import org.codehaus.jackson.map.annotate.JsonRootName;

import java.util.HashMap;
import java.util.Map;

@JsonRootName("service")
public final class PaymentService implements MyService {

  @Override
  public String getName() {
    return "PaymentService";
  }

  @Override
  public String getVersion() {
    return "v1";
  }

  @Override
  public String getURI() {
    return "http://server:port/myservices/payment";
  }

  @Override
  public Map<String, String> getEnvironment() {
    final Map<String, String> env = new HashMap<>();
    env.put("ENV", "PRODUCTION");
    env.put("ENV2", "SHOW ME THE MONEY");
    return env;
  }
}
