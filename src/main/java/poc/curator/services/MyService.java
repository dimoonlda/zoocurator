package poc.curator.services;

import java.util.Map;

public interface MyService {

  String getName();

  String getVersion();

  String getURI();

  Map<String, String> getEnvironment();
}
