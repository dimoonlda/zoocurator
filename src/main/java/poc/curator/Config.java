package poc.curator;

public final class Config {

  /* Location of ZooKeeper Server. */
  public static final String ZK_CONNECTION_STRING = System.getProperty("zoo.server", "127.0.0.1:2181");

  /* THIS PATH WILL BE CREATED AUTO on THE SERVER */
  public static final String SERVICES_PATH = "/myapp/services";

  public static final String CONFIG_PATH = "/myapp/config";

}