package poc.curator;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.ZKPaths;
import org.apache.curator.x.discovery.*;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;
import org.apache.zookeeper.KeeperException;
import poc.curator.services.MyService;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class acts as a facade over the Curator APIs for Zookeeper Recipes.
 * You can register/unregister, discover all services and discover based on name.
 */
public final class ZooKeeperRecipes {

  private final CuratorFramework curatorClient;
  private final ServiceDiscovery<MyService> serviceDiscovery;
  private final Map<String, ServiceInstance<MyService>> serviceInstances;
  private final PathWatcherRecipe pathWatcherRecipe;
  private final CacheRecipe cacheRecipe;

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

    // Watches for any changes to given PATH
    pathWatcherRecipe = new PathWatcherRecipe(curatorClient);

    cacheRecipe = new CacheRecipe(curatorClient);
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

  public void unregisterService(final String serviceName, final int servicePort) throws Exception {
    final ServiceInstance<MyService> thisInstance = serviceInstances.get(serviceName + servicePort);
    if (thisInstance != null) {
      serviceDiscovery.unregisterService(thisInstance);
    }
  }

  public Collection<ServiceInstance<MyService>> discover(final String serviceName) throws Exception {
    final Collection<ServiceInstance<MyService>> instances = serviceDiscovery.queryForInstances(serviceName);
    for (ServiceInstance<MyService> instance : instances) {
      //outputInstance(instance);
    }
    return instances;
  }

  public List<ServiceInstance<MyService>>  discoverAll() throws Exception {
    Collection<String> serviceNames = null;
    try {
      serviceNames = serviceDiscovery.queryForNames();
    } catch (KeeperException.NoNodeException ke) {
    }
    final List<ServiceInstance<MyService>> list = new ArrayList<>();
    if (serviceNames != null) {
      for (String serviceName : serviceNames) {
        final Collection<ServiceInstance<MyService>> instances = serviceDiscovery.queryForInstances(serviceName);
        for (ServiceInstance<MyService> instance : instances) {
          //outputInstance(instance);
        }
        list.addAll(instances);
      }
    }
    return list;
  }

  public void addDataWatch(String path, CacheRecipe.CacheListener listener) {
    try {
      final String newPath = ZKPaths.makePath(Config.CONFIG_PATH, path);
      cacheRecipe.addNodeCacheWatch(newPath, listener);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void setData(String path, String data) {
    byte[] bytes = data.getBytes();
    try {
      final String newPath = ZKPaths.makePath(Config.CONFIG_PATH, path);
      curatorClient.setData().forPath(newPath, bytes);
    } catch (KeeperException.NoNodeException e) {
      try {
        final String newPath = ZKPaths.makePath(Config.CONFIG_PATH, path);
        curatorClient.create().creatingParentsIfNeeded().forPath(newPath, bytes);
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
      final String newPath = ZKPaths.makePath(Config.CONFIG_PATH, path);
      byte[] bytes = curatorClient.getData().forPath(newPath);
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

  public void remove(String path) throws Exception {
    try {
      final String newPath = ZKPaths.makePath(Config.CONFIG_PATH, path);
      curatorClient.delete().forPath(newPath);
    } catch (KeeperException.NoNodeException e) {
      // ignore
    }
  }

  public PathWatcherRecipe getPathWatcher() {
    return pathWatcherRecipe;
  }

  public void start() {
    try {
      curatorClient.start();
      closeAbles.add(curatorClient);
      serviceDiscovery.start();
      // add to top so we can close it first.
      closeAbles.add(0, serviceDiscovery);
      closeAbles.add(0, cacheRecipe);
      closeAbles.add(0, pathWatcherRecipe);
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

//  private static void outputInstance(ServiceInstance<MyService> instance) {
//    System.out.println("\t" + instance.getPayload() + ": " + instance.buildUriSpec());
//  }

}