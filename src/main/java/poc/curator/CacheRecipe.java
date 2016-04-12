package poc.curator;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.zookeeper.KeeperException;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Global cache that can be accessed by any client.
 */
public final class CacheRecipe implements Closeable {

  private final CuratorFramework client;
  private final List<NodeCache> nodeCacheList;

  interface CacheListener {
    void dataChanged(String newData);
  }

  public CacheRecipe(CuratorFramework client) {
    this.client = client;
    nodeCacheList = new ArrayList<>();
  }

  public NodeCache addNodeCacheWatch(String path, CacheListener listener) throws Exception {
    final NodeCache cache = new NodeCache(client, path);
    cache.getListenable().addListener(new MyNodeCacheListener(cache, listener));
    cache.start();
    nodeCacheList.add(cache);
    System.out.println("Added watch for " + path);
    return cache;
  }

  public void close() throws IOException {
    for (NodeCache cache : nodeCacheList) {
      cache.close();
    }
  }

  final class MyNodeCacheListener implements NodeCacheListener {

    private NodeCache nodeCache;
    private CacheListener listener;

    public MyNodeCacheListener(NodeCache cache, CacheListener listener) {
      nodeCache = cache;
      this.listener = listener;
    }

    @Override
    public void nodeChanged() throws Exception {
      String data = new String(nodeCache.getCurrentData().getData());
      System.out.println("Cache changed: " + data);
      listener.dataChanged(data);
    }
  }
}
