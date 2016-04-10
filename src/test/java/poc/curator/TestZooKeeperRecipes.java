package poc.curator;

import org.apache.curator.test.TestingServer;
import org.apache.curator.utils.CloseableUtils;
import org.apache.curator.x.discovery.ServiceInstance;
import org.junit.*;
import poc.curator.services.MyService;
import poc.curator.services.OrdersService;
import poc.curator.services.PaymentService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class TestZooKeeperRecipes {

  private static TestingServer server;
  private static ZooKeeperRecipes zooKeeperRecipes = null;

  @BeforeClass
  public static void setupInitial() {
    try {
      server = new TestingServer();
      zooKeeperRecipes = new ZooKeeperRecipes(server.getConnectString());
      zooKeeperRecipes.start();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @AfterClass
  public static void tearDownFinal() {
    zooKeeperRecipes.close();
    CloseableUtils.closeQuietly(server);
  }

  @Test
  public void testNoService() {
    try {
      Collection<ServiceInstance<MyService>> instances =  zooKeeperRecipes.discover("SomeService");
      assertEquals("Expecting no services", 0, instances.size());
    } catch (Exception e) {
      e.printStackTrace();
      fail("Got exception while discovering");
    }
  }

  @Test
  public void testRegisterOrdersService() {
    try {
      Collection<ServiceInstance<MyService>> instances = zooKeeperRecipes.discoverAll();
      assertEquals("Expecting 0 service(s)", 0, instances.size());

      zooKeeperRecipes.discover(MyService.ORDERS_SERVICE);
      assertEquals("Expecting no services", 0, instances.size());

      final OrdersService ordersService = new OrdersService();
      zooKeeperRecipes.registerService(ordersService.getName(), 2000, ordersService);

      Thread.sleep(1000);

      instances =  zooKeeperRecipes.discover(MyService.ORDERS_SERVICE);
      assertEquals("Expecting 1 service(s)", 1, instances.size());

      zooKeeperRecipes.unregisterService(ordersService.getName(), 2000);
      instances =  zooKeeperRecipes.discover(MyService.ORDERS_SERVICE);
      assertEquals("Expecting 0 service(s)", 0, instances.size());

      instances = zooKeeperRecipes.discoverAll();
      assertEquals("Expecting 0 service(s)", 0, instances.size());
    } catch (Exception e) {
      e.printStackTrace();
      fail("Got exception while discovering");
    }
  }

  @Test
  public void testRegisterPaymentService() {
    try {
      Collection<ServiceInstance<MyService>> instances =  zooKeeperRecipes.discover(MyService.PAYMENT_SERVICE);
      assertEquals("Expecting no services", 0, instances.size());

      final PaymentService paymentService = new PaymentService();
      zooKeeperRecipes.registerService(paymentService.getName(), 3000, paymentService);

      Thread.sleep(1000);

      instances =  zooKeeperRecipes.discover(MyService.PAYMENT_SERVICE);
      assertEquals("Expecting 1 service(s)", 1, instances.size());

      instances = zooKeeperRecipes.discoverAll();
      assertEquals("Expecting 1 service(s)", 1, instances.size());

      zooKeeperRecipes.unregisterService(paymentService.getName(), 3000);
      Thread.sleep(1000);
      instances =  zooKeeperRecipes.discover(MyService.PAYMENT_SERVICE);
      assertEquals("Expecting 0 service(s)", 0, instances.size());

    } catch (Exception e) {
      e.printStackTrace();
      fail("Got exception while discovering");
    }
  }

  @Test
  public void testCache() {
    final String path = "/data1";
    final String pathValue = "value1";

    final List<String> data = new ArrayList<>();
    final CountDownLatch latch = new CountDownLatch(1);
    final MyGlobalCache.CacheListener listener = (s) -> {
      data.add(s);
      latch.countDown();
    };
    zooKeeperRecipes.addDataWatch(path, listener);

    zooKeeperRecipes.setData(path, pathValue);

    // Wait for 1 second or until we get notified.
    try {
      latch.await(1, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
    }
    final String result = zooKeeperRecipes.getData(path);

    assertEquals("Size not same", 1, data.size());
    assertEquals("Values different", pathValue, result);
    assertEquals("Notified Value different", "value1", data.get(0));
  }

  @Ignore
  public void testPathWatcher() {
    Collection<ServiceInstance<MyService>> instances = null;
    try {
      final List<List<String>> dataList = new ArrayList<>();
      CyclicBarrier barrier = new CyclicBarrier(2);
      MyPathWatcher.PathListener listener = new MyPathWatcher.PathListener() {

        @Override
        public void nodeAdded(String path, String data) {
          List<String> values = new ArrayList<>();
          values.add("ADD");
          values.add(path);
          values.add(data);
          dataList.add(values);
          try {
            barrier.await();
          } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
          }
        }

        @Override
        public void nodeDeleted(String path, String data) {
          List<String> values = new ArrayList<>();
          values.add("DELETE");
          values.add(path);
          values.add(data);
          dataList.add(values);
          try {
            barrier.await();
          } catch (InterruptedException | BrokenBarrierException e) {
            e.printStackTrace();
          }
        }

        @Override
        public void nodeUpdated(String path, String data) {
        }
      };
      // Watch for path changes
      zooKeeperRecipes.getPathWatcher().addPathWatch(Config.SERVICES_PATH, listener);

      final OrdersService ordersService = new OrdersService();
      zooKeeperRecipes.registerService(ordersService.getName(), 2000, ordersService);
      try {
        barrier.await(1, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
      }

      assertEquals("Size not same", 1, dataList.size());
      assertEquals("Values different", "ADD", dataList.get(0).get(0));
      dataList.remove(0);
      barrier.reset();


      zooKeeperRecipes.unregisterService(ordersService.getName(), 2000);
      try {
        barrier.await(1, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
      }
      assertEquals("Size not same", 1, dataList.size());
      assertEquals("Values different", "DELETE", dataList.get(0).get(0));
    } catch (Exception e) {
      e.printStackTrace();
    }

  }
}
