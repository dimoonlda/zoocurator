package poc.curator.services;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonRootName;

import java.util.HashMap;
import java.util.Map;

@JsonRootName("service")
@JsonIgnoreProperties(ignoreUnknown = true)
public final class OrdersService implements MyService {

  @Override
  public String getName() {
    return ORDERS_SERVICE;
  }

  @Override
  public String getVersion() {
    return "v1";
  }

  @Override
  public String getURI() {
    return "/myservices/orders";
  }

  @Override
  public Map<String, String> getEnvironment() {
    final Map<String, String> env = new HashMap<>();
    env.put("ENV", "PRODUCTION");
    env.put("ENV2", "HELLO");
    return env;
  }

  @Override
  public String toString() {
    return getName() + "{ " + getVersion() + ", " + getURI() + ", " + getEnvironment() + " }" ;
  }
}
