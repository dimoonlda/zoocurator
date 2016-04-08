package poc.curator;

import org.apache.curator.test.TestingServer;
import org.apache.curator.utils.CloseableUtils;
import org.apache.curator.x.discovery.ServiceInstance;
import org.junit.*;
import poc.curator.services.MyService;
import poc.curator.services.OrdersService;
import poc.curator.services.PaymentService;

import java.util.Collection;

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
      Collection<ServiceInstance<MyService>> instances =  zooKeeperRecipes.discover(MyService.ORDERS_SERVICE);
      assertEquals("Expecting no services", 0, instances.size());

      final OrdersService ordersService = new OrdersService();
      zooKeeperRecipes.registerService(ordersService.getName(), 2000, ordersService);

      Thread.sleep(1000);

      instances =  zooKeeperRecipes.discover(MyService.ORDERS_SERVICE);
      assertEquals("Expecting 1 service(s)", 1, instances.size());

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

    } catch (Exception e) {
      e.printStackTrace();
      fail("Got exception while discovering");
    }
  }


}
