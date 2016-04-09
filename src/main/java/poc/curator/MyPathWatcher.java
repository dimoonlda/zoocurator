package poc.curator;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.curator.utils.CloseableUtils;
import org.apache.curator.utils.ZKPaths;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Watches for changes to a certain tree/path.
 *
 * TreeWatch - Watch changes to root and all its sub-trees.
 * PathWatch - Watch only that node level and no sub-tree within it.
 */
public final class MyPathWatcher implements Closeable {

  private final CuratorFramework client;
  // tracks all closeables so we can do a clean termination for all of them.
  private final List<Closeable> closeAbles = new ArrayList<>();

  interface PathListener {
    void nodeAdded(String path, String data);
    void nodeDeleted(String path, String data);
    void nodeUpdated(String path, String data);
  }

  public MyPathWatcher(CuratorFramework client) {
    this.client = client;
  }

  public TreeCache addTreeWatch(final String path, PathListener listener) throws Exception {
    final TreeCache myPath = new TreeCache(client, path);
    myPath.start();
    addTreeListener(myPath, listener);
    closeAbles.add(myPath);
    return myPath;
  }

  public PathChildrenCache addPathWatch(final String path, PathListener listener) throws Exception {
    final PathChildrenCache myPath = new PathChildrenCache(client, path, true);
    myPath.start();
    addPathListener(myPath, listener);
    closeAbles.add(myPath);
    return myPath;
  }

  private void addTreeListener(final TreeCache myPath, final PathListener pathListener) {

    final TreeCacheListener listener = new TreeCacheListener() {

      @Override
      public void childEvent(CuratorFramework curatorFramework, TreeCacheEvent event) throws Exception {
        System.out.println("Got event: " + event.getType() + " data:" + new String(event.getData().getData()));
        switch (event.getType()) {

          case NODE_ADDED:
            pathListener.nodeAdded(event.getData().getPath(), new String(event.getData().getData()));
            break;
          case NODE_UPDATED:
            pathListener.nodeUpdated(event.getData().getPath(), new String(event.getData().getData()));
            break;
          case NODE_REMOVED:
            pathListener.nodeDeleted(event.getData().getPath(), new String(event.getData().getData()));
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
  }

  private void addPathListener(final PathChildrenCache myPath, final PathListener pathListener) {

    final PathChildrenCacheListener listener = new PathChildrenCacheListener() {

      @Override
      public void childEvent(CuratorFramework curatorFramework, PathChildrenCacheEvent event) throws Exception {
        System.out.println("Got event: " + event.getType() + " data:" + new String(event.getData().getData()));
        switch (event.getType()) {

          case CHILD_ADDED:
            pathListener.nodeAdded(event.getData().getPath(), new String(event.getData().getData()));
            break;
          case CHILD_UPDATED:
            pathListener.nodeUpdated(event.getData().getPath(), new String(event.getData().getData()));
            break;
          case CHILD_REMOVED:
            pathListener.nodeDeleted(event.getData().getPath(), new String(event.getData().getData()));
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
  }

  @Override
  public void close() throws IOException {
    for (Closeable closeable : closeAbles) {
      CloseableUtils.closeQuietly(closeable);
    }
  }
}
