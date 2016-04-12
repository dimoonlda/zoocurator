package poc.curator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Random;

/**
 * Main program to add, remove and list services.
 *
 * Some snippets taken from Curator examples.
 */
public final class Main {

  Random random = new Random();

  private void registerService(final ServiceDiscoveryRecipe discoverer, final String name) throws Exception {
    // get a random port where we want to access request for our service. Typically this should be fixed though.
    // we do this so we can simulate the same service on multiple ports.

    final int randomPort = random.nextInt(10000);
    discoverer.registerService(name, randomPort);
    System.out.println("Service " + name + " registered on port " + randomPort);
  }

  private void unregisterService(final ServiceDiscoveryRecipe discoverer, final String name, final String port) throws Exception {
    discoverer.unregisterService(name, port);
  }

  private void listInstance(final ServiceDiscoveryRecipe discoverer, final String name)  throws Exception {
    discoverer.discover(name);
  }

  private void listAllInstances(final ServiceDiscoveryRecipe discoverer) throws Exception {
    discoverer.discoverAll();
  }

  public static void main(String[] args) {
    final Main main = new Main();
    ServiceDiscoveryRecipe discoverer = null;

    try {
      discoverer = new ServiceDiscoveryRecipe(Config.ZK_CONNECTION_STRING);
      discoverer.start();

      main.doOperations(discoverer);

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (discoverer != null) {
        discoverer.close();
      }
    }
  }

  private void doOperations(final ServiceDiscoveryRecipe discoverer) throws Exception {

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
            registerService(discoverer, args[0]);
          }
        } else if (operation.equals("delete")) {
          if (args.length == 2) {
            unregisterService(discoverer, args[0], args[1]);
          }
        } else if (operation.equals("list")) {
          if (args.length == 1) {
            listInstance(discoverer, args[0]);
          }
        } else if (operation.equals("listall")) {
          listAllInstances(discoverer);
        }
      }
    } finally {
    }
  }

  private static void printHelp() {
    System.out.println("Supported commands at the prompt:\n");
    System.out.println("add <name> <description>: Adds a mock service with the given name and description");
    System.out.println("delete <name>: Deletes one of the mock services with the given name");
    System.out.println("listall: Lists all the currently registered services");
    System.out.println("list <name>: Lists an instance of the service with the given name");
    System.out.println("quit: Quit the program");
    System.out.println();
  }
}
