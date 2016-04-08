package poc.curator;

import poc.curator.services.MyService;
import poc.curator.services.OrdersService;
import poc.curator.services.PaymentService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Random;

/**
 * Main program to add, remove and list services.
 *
 * Some snippets taken from Curator examples.
 */
public final class Main2 {

  private Random random = new Random();

  private void registerService(final ZooKeeperRecipes zooKeeperRecipes, final String name) throws Exception {
    // get a random port where we want to access request for our service. Typically this should be fixed though.
    // we do this so we can simulate the same service on multiple ports.
    final int randomPort = random.nextInt(10000);
    MyService service;
    if (randomPort % 2 == 0) {
      service = new OrdersService();
    } else {
      service = new PaymentService();
    }
    zooKeeperRecipes.registerService(service.getName(), randomPort, service);
    System.out.println("Service " + service.getName() + " registered on port " + randomPort);
  }

  private void unregisterService(final ZooKeeperRecipes zooKeeperRecipes, final String name, final String port) throws Exception {
    zooKeeperRecipes.unregisterService(name, port);
  }

  private void listInstance(final ZooKeeperRecipes zooKeeperRecipes, final String name)  throws Exception {
    zooKeeperRecipes.discover(name);
  }

  private void listAllInstances(final ZooKeeperRecipes zooKeeperRecipes) throws Exception {
    zooKeeperRecipes.discoverAll();
  }

  public static void main(String[] args) {
    final Main2 main = new Main2();
    ZooKeeperRecipes zooKeeperRecipes = null;

    try {
      zooKeeperRecipes = new ZooKeeperRecipes(Config.ZK_CONNECTION_STRING);
      zooKeeperRecipes.start();

      main.doOperations(zooKeeperRecipes);

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (zooKeeperRecipes != null) {
        zooKeeperRecipes.close();
      }
    }
  }

  private void doOperations(final ZooKeeperRecipes zooKeeperRecipes) throws Exception {

    try {
      final BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
      boolean done = false;
      while (!done) {
        System.out.print("> ");

        String line = in.readLine();
        if (line == null) {
          break;
        }

        String command = line.trim();
        String[] parts = command.split("\\s");
        if (parts.length == 0) {
          continue;
        }
        String operation = parts[0];
        String args[] = Arrays.copyOfRange(parts, 1, parts.length);

        if (operation.equalsIgnoreCase("help") || operation.equalsIgnoreCase("?")) {
          printHelp();
        } else if (operation.equalsIgnoreCase("q") || operation.equalsIgnoreCase("quit")) {
          done = true;
        } else if (operation.equals("add")) {
          if (args.length == 1) {
            registerService(zooKeeperRecipes, args[0]);
          }
        } else if (operation.equals("delete")) {
          if (args.length == 2) {
            unregisterService(zooKeeperRecipes, args[0], args[1]);
          }
        } else if (operation.equals("list")) {
          if (args.length == 1) {
            listInstance(zooKeeperRecipes, args[0]);
          }
        } else if (operation.equals("listall")) {
          listAllInstances(zooKeeperRecipes);
        } else if (operation.equals("listen")) {
          if (args.length == 1) {
            zooKeeperRecipes.addDataWatch(args[0], (d) -> {
              System.out.println("Got data:" + d);
            });
          }
        } else if (operation.equals("set")) {
          if (args.length == 2) {
            zooKeeperRecipes.setData(args[0], args[1]);
          }
        } else if (operation.equals("get")) {
          if (args.length == 1) {
            String data = zooKeeperRecipes.getData(args[0]);
            System.out.println("Got: " + data);
          }
        }
      }
    } finally {
    }
  }

  private static void printHelp() {
    System.out.println("Supported commands at the prompt:\n");
    System.out.println("add: Adds a random Orders/Payment service");
    System.out.println("delete <name>: Deletes one of the mock services with the given name");
    System.out.println("listall: Lists all the currently registered services");
    System.out.println("list <name>: Lists an instance of the service with the given name");
    System.out.println("set <path> <data>: Set data for node with the given path");
    System.out.println("get <path>: Get data for node with the given path");
    System.out.println("listen <path>: Listens to changes for the given path");
    System.out.println("quit: Quit the program");
    System.out.println();
  }
}
