package poc.curator.services;

import java.util.Map;

public interface MyService {

  String ORDERS_SERVICE = "OrdersService";
  String PAYMENT_SERVICE = "PaymentService";

  String getName();

  String getVersion();

  String getURI();

  Map<String, String> getEnvironment();
}
