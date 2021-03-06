package poc.curator;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.utils.CloseableUtils;
import org.apache.curator.x.discovery.*;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 * This class acts as a facade over the Curator APIs for Service Registration and Discovery.
 * You can register/unregister, discover all services and discover based on name.
 */
public final class ServiceDiscoveryRecipe {

  private final CuratorFramework curatorClient;
  private final ServiceDiscovery<InstanceDetails> serviceDiscovery;
  private final Map<String, ServiceInstance<InstanceDetails>> serviceInstances;

  // tracks all closeables so we can do a clean termination for all of them.
  private final List<Closeable> closeAbles = new ArrayList<>();

  public ServiceDiscoveryRecipe(String zookeeperAddress) throws Exception {
    serviceInstances = new HashMap<>();
    System.out.println("Connecting to ZooKeeper: " + zookeeperAddress);

    // Specify retry mechanism in case of recoverable errors from ZooKeeper.
    curatorClient = CuratorFrameworkFactory.newClient(zookeeperAddress,
        new ExponentialBackoffRetry(1000, 3));

    // Payload Serializer
    final JsonInstanceSerializer<InstanceDetails> serializer = new JsonInstanceSerializer<>(InstanceDetails.class);

    // Service Discovery
    serviceDiscovery = ServiceDiscoveryBuilder.builder(InstanceDetails.class)
        .client(curatorClient)
        .basePath(Config.SERVICES_PATH)
        .serializer(serializer)
        .build();
  }

  public void registerService(String serviceName, int servicePort) throws UnknownHostException, Exception {
    // Scheme, address and port - This will yield an address of form: http://<ip>:port/
    final UriSpec uriSpec = new UriSpec("{scheme}://{address}:{port}");

    final ServiceInstance<InstanceDetails> thisInstance = ServiceInstance.<InstanceDetails>builder().name(serviceName)
        .uriSpec(uriSpec)
        // Pass the IP address the instance is available on
        .address(InetAddress.getLocalHost().getHostAddress())
        // Pass the Port the instance is available on
        .port(servicePort)
        // Pass other Instance details that you want to expose for other services to discover
        .payload(new InstanceDetails(serviceName))
        .build();

    serviceDiscovery.registerService(thisInstance);

    // track it so we can unregister this one.
    serviceInstances.put(serviceName + servicePort, thisInstance);
  }

  public void unregisterService(final String serviceName, final String servicePort) throws Exception {
    final ServiceInstance<InstanceDetails> thisInstance = serviceInstances.get(serviceName + servicePort);
    if (thisInstance != null) {
      serviceDiscovery.unregisterService(thisInstance);
    }
  }

  public void discover(final String serviceName) throws Exception {
    System.out.println("Looking up " + serviceName);
    final Collection<ServiceInstance<InstanceDetails>> instances = serviceDiscovery.queryForInstances(serviceName);

    for (ServiceInstance<InstanceDetails> instance : instances) {
      outputInstance(instance);
    }
  }

  public void discoverAll() throws Exception {
    final Collection<String> serviceNames = serviceDiscovery.queryForNames();

    for (String serviceName : serviceNames) {
      final Collection<ServiceInstance<InstanceDetails>> instances = serviceDiscovery.queryForInstances(serviceName);
      System.out.println("Looking up " + serviceName);
      for (ServiceInstance<InstanceDetails> instance : instances) {
        outputInstance(instance);
      }
    }
  }

  public void start() {
    try {
      curatorClient.start();
      closeAbles.add(curatorClient);
      serviceDiscovery.start();
      // add to top so we can close it first.
      closeAbles.add(0, serviceDiscovery);
    } catch (Exception e) {
      throw new RuntimeException("Error starting Curator Framework/Discovery", e);
    }
  }

  public void close() {
    // Close all
    for (Closeable closeable : closeAbles) {
      CloseableUtils.closeQuietly(closeable);
    }
  }

  private static void outputInstance(ServiceInstance<InstanceDetails> instance) {
    System.out.println("\t" + instance.getPayload().getDescription() + ": " + instance.buildUriSpec());
  }

}