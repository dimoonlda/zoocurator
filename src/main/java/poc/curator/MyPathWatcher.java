package poc.curator;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.utils.ZKPaths;

/**
 * Watches for changes to a certain tree/path.
 *
 * TreeWatch - Watch changes to root and all its sub-trees.
 * PathWatch - Watch only that node level and no sub-tree within it.
 */
public final class MyPathWatcher {

  private final CuratorFramework client;

  public MyPathWatcher(CuratorFramework client) {
    this.client = client;
  }

  public TreeCache addTreeWatch(final String path) throws Exception {
    final TreeCache myPath = new TreeCache(client, path);
    myPath.start();
    addTreeListener(myPath);
    return myPath;
  }

  public PathChildrenCache addPathWatch(final String path) throws Exception {
    final PathChildrenCache myPath = new PathChildrenCache(client, path, true);
    myPath.start();
    addPathListener(myPath);
    return myPath;
  }

  private void addTreeListener(final TreeCache myPath) {

    final TreeCacheListener listener = new TreeCacheListener() {

      @Override
      public void childEvent(CuratorFramework curatorFramework, TreeCacheEvent event) throws Exception {
        System.out.println("Got event: " + event.getType() + " data:" + new String(event.getData().getData()));
        switch (event.getType()) {

          case NODE_ADDED:
            //System.out.println("Node added: " + ZKPaths.getNodeFromPath(event.getData().getPath()));
            break;
          case NODE_UPDATED:
            break;
          case NODE_REMOVED:
            //System.out.println("Node removed: " + ZKPaths.getNodeFromPath(event.getData().getPath()));
            break;
          case CONNECTION_SUSPENDED:
            break;
          case CONNECTION_RECONNECTED:
            break;
          case CONNECTION_LOST:
            break;
        }
      }
    };
    myPath.getListenable().addListener(listener);
    System.out.println("Added Tree listener");
  }

  private void addPathListener(final PathChildrenCache myPath) {

    final PathChildrenCacheListener listener = new PathChildrenCacheListener() {

      @Override
      public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent event) throws Exception {
        System.out.println("Got event: " + event.getType() + " data:" + new String(event.getData().getData()));
        switch (event.getType()) {

          case CHILD_ADDED:
            System.out.println("Node added: " + ZKPaths.getNodeFromPath(event.getData().getPath()));
            break;
          case CHILD_UPDATED:
            break;
          case CHILD_REMOVED:
            System.out.println("Node removed: " + ZKPaths.getNodeFromPath(event.getData().getPath()));
            break;
          case CONNECTION_SUSPENDED:
            break;
          case CONNECTION_RECONNECTED:
            break;
          case CONNECTION_LOST:
            break;
        }
      }
    };
    myPath.getListenable().addListener(listener);
    System.out.println("Added Path listener");
  }
}
