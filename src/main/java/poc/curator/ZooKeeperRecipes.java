package poc.curator;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.*;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;
import org.apache.curator.x.discovery.details.ServiceCacheListener;
import org.apache.zookeeper.KeeperException;
import poc.curator.services.MyService;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 * This class acts as a facade over the Curator APIs for Zookeeper Recipes.
 * You can register/unregister, discover all services and discover based on name.
 */
public final class ZooKeeperRecipes {

  private final CuratorFramework curatorClient;
  private final ServiceDiscovery<MyService> serviceDiscovery;
  private final ServiceCache<MyService> serviceCache;

  private final Map<String, ServiceInstance<MyService>> serviceInstances;
  private final MyPathWatcher watcher;
  private final MyGlobalCache myGlobalCache;

  // tracks all closeables so we can do a clean termination for all of them.
  private final List<Closeable> closeAbles = new ArrayList<>();

  public ZooKeeperRecipes(String zookeeperAddress) throws Exception {
    serviceInstances = new HashMap<>();
    System.out.println("Connecting to ZooKeeper: " + zookeeperAddress);

    // Specify retry mechanism in case of recoverable errors from ZooKeeper.
    curatorClient = CuratorFrameworkFactory.newClient(zookeeperAddress,
        new ExponentialBackoffRetry(1000, 3));

    // Payload Serializer
    final JsonInstanceSerializer<MyService> serializer = new JsonInstanceSerializer<>(MyService.class);

    // Service Discovery
    serviceDiscovery = ServiceDiscoveryBuilder.builder(MyService.class)
        .client(curatorClient)
        .basePath(Config.SERVICES_PATH)
        .serializer(serializer)
        .build();

    // Adding Service cache for Payment Service.= to get notifications.
    serviceCache = serviceDiscovery.serviceCacheBuilder().name("PaymentService").build();
    serviceCache.addListener(new MyServiceCacheListener());

    // Watches for any changes to given PATH
    watcher = new MyPathWatcher(curatorClient);

    myGlobalCache = new MyGlobalCache(curatorClient);
  }

  public void registerService(String serviceName, int servicePort, MyService obj) throws UnknownHostException, Exception {
    // Scheme, address and port - This will yield an address of form: http://<ip>:port/
    final UriSpec uriSpec = new UriSpec("{scheme}://{address}:{port}");

    final ServiceInstance<MyService> thisInstance = ServiceInstance.<MyService>builder().name(serviceName)
        .uriSpec(uriSpec)
        // Pass the IP address the instance is available on
        .address(InetAddress.getLocalHost().getHostAddress())
        // Pass the Port the instance is available on
        .port(servicePort)
        // Pass other Instance details that you want to expose for other services to discover
        .payload(obj)
        .build();

    serviceDiscovery.registerService(thisInstance);

    // track it so we can unregister this one.
    serviceInstances.put(serviceName + servicePort, thisInstance);
  }

  public void unregisterService(final String serviceName, final String servicePort) throws Exception {
    final ServiceInstance<MyService> thisInstance = serviceInstances.get(serviceName + servicePort);
    if (thisInstance != null) {
      serviceDiscovery.unregisterService(thisInstance);
    }
  }

  public Collection<ServiceInstance<MyService>> discover(final String serviceName) throws Exception {
    System.out.println("Looking up " + serviceName);
    final Collection<ServiceInstance<MyService>> instances = serviceDiscovery.queryForInstances(serviceName);

    for (ServiceInstance<MyService> instance : instances) {
      outputInstance(instance);
    }
    return instances;
  }

  public List<ServiceInstance<MyService>>  discoverAll() throws Exception {
    final Collection<String> serviceNames = serviceDiscovery.queryForNames();
    final List<ServiceInstance<MyService>> list = new ArrayList<>();
    for (String serviceName : serviceNames) {
      final Collection<ServiceInstance<MyService>> instances = serviceDiscovery.queryForInstances(serviceName);
      System.out.println("Looking up " + serviceName);
      for (ServiceInstance<MyService> instance : instances) {
        outputInstance(instance);
      }
      list.addAll(instances);
    }
    return list;
  }

  public void addDataWatch(String path) {
    try {
      myGlobalCache.addNodeCacheWatch(path);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void setData(String path, String data) {
    byte[] bytes = data.getBytes();
    try {
      curatorClient.setData().forPath(path, bytes);
    } catch (KeeperException.NoNodeException e) {
      try {
        curatorClient.create().creatingParentsIfNeeded().forPath(path, bytes);
      } catch (Exception e1) {
        e1.printStackTrace();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public String getData(String path) {
    String data = null;
    try {
      byte[] bytes = curatorClient.getData().forPath(path);
      if (bytes != null) {
        data = new String(bytes);
      }
    } catch (KeeperException.NoNodeException e) {
      // do nothing if node does not exist.
    }
    catch (Exception e) {
      e.printStackTrace();
    }
    return data;
  }

  public void start() {
    try {
      curatorClient.start();
      closeAbles.add(curatorClient);
      serviceDiscovery.start();
      serviceCache.start();

      // add to top so we can close it first.
      closeAbles.add(0, serviceDiscovery);
      closeAbles.add(0, serviceCache);

      closeAbles.add(0, myGlobalCache);
      // watch for changes to SERVICES_PATH
      closeAbles.add(0, watcher.addTreeWatch(Config.SERVICES_PATH));
    } catch (Exception e) {
      throw new RuntimeException("Error starting Curator Framework/Discovery", e);
    }
  }

  public void close() {
    for (Closeable closeable : closeAbles) {
      // Close all
      try {
        closeable.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private static void outputInstance(ServiceInstance<MyService> instance) {
    System.out.println("\t" + instance.getPayload() + ": " + instance.buildUriSpec());
  }

  final class MyServiceCacheListener implements ServiceCacheListener {

    @Override
    public void cacheChanged() {
      System.out.println("--> Cache changed");
    }

    @Override
    public void stateChanged(CuratorFramework curatorFramework, ConnectionState connectionState) {
      System.out.println("--> state changed: " + connectionState.isConnected());
    }
  }

}