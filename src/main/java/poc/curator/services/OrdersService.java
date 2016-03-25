package poc.curator.services;

import org.codehaus.jackson.map.annotate.JsonRootName;

import java.util.HashMap;
import java.util.Map;

@JsonRootName("service")
public final class OrdersService implements MyService {

  @Override
  public String getName() {
    return "OrdersService";
  }

  @Override
  public String getVersion() {
    return "v1";
  }

  @Override
  public String getURI() {
    return "http://server:port/myservices/orders";
  }

  @Override
  public Map<String, String> getEnvironment() {
    final Map<String, String> env = new HashMap<>();
    env.put("ENV", "PRODUCTION");
    env.put("ENV2", "HELLO");
    return env;
  }
}
