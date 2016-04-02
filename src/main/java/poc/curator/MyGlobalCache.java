package poc.curator;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.zookeeper.KeeperException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Global cache that can be accessed by any client.
 */
public final class MyGlobalCache {

  private final CuratorFramework client;
  private final List<NodeCache> nodeCacheList;

  public MyGlobalCache(CuratorFramework client) {
    this.client = client;
    nodeCacheList = new ArrayList<>();
  }

  public NodeCache addNodeCacheWatch(String path) throws Exception {
    final NodeCache cache = new NodeCache(client, path);
    cache.getListenable().addListener(new MyNodeCacheListener(cache));
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

    public MyNodeCacheListener(NodeCache cache) {
      nodeCache = cache;
    }

    @Override
    public void nodeChanged() throws Exception {
      System.out.println("Cache changed: " + new String(nodeCache.getCurrentData().getData()));
    }
  }
}
