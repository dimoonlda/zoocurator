package poc.curator;

public class Config {

  /* Location of ZooKeeper Server. */
  public static final String ZK_CONNECTION_STRING = System.getProperty("zoo.server", "127.0.0.1:2181");
  /* THIS PATH WILL BE CREATED AUTO on THE SERVER */
  public static final String BASE_PATH = "/myapp/services";

  public static final String ORDERS_SERVICE  = "orders";
  public static final String PAYMENT_SERVICE = "payment";

}