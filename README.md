# zoocurator

Explore the various recipes for Apache Zookeeper along with Apache Curator.

[![Build Status](https://travis-ci.org/rahulsh1/zoocurator.svg?branch=master)](https://travis-ci.org/rahulsh1/zoocurator)

## Recipes covered
- Discovery
- Path Watch: Tree (all changes on a certain tree), Path (only immediate level children changes)


## Discovery
We will be creating some mock services, registering them so that can be discovered by other services.

Note I am using Zookeeper `3.4.6` along with Curator `2.10.0`.
I installed everything latest just to find it was not working. I used Zookeeper `3.4.8` along with Curator `3.1.0` and that kept throwing

     2016-03-24 22:43:03,759 [myid:] - WARN  [NIOServerCxn.Factory:0.0.0.0/0.0.0.0:2181:ZooKeeperServer@707] - Received packet at server of unknown type 15

just to realize Curator `3.1.0` was using an alpha version of Zookeeper from maven dependencies.
Curator `2.10.0` is latest too but that works with Zookeeper `3.4.6` which was released quite some time back.

### Pre-requisites
- Zookeeper `3.4.6`
- JDK 1.7/1.8
- Maven 3.x

### Build
Download all sources and build with maven. Maven will download the correct dependencies.

    $ git clone https://github.com/rahulsh1/zoocurator.git
    $ cd zoocurator
    $ mvn install

### Run
- Start ZooKeeper server from zookeeper directory. I start this in foreground to look for interesting messages on the console.

        $ bin/zkServer.sh start-foreground

- Start our program.
This assumes our server is running on `127.0.0.1:2181`. 

        $ mvn install -Prun

   If your server is not running on localhost or port 2181, you can use:

         $ mvn install -Prun -Dzoo.server=<server>:<port>

### Output

    $ mvn install -Prun

    > help
    Supported commands at the prompt:

    add <name> <description>: Adds a mock service with the given name and description
    delete <name>: Deletes one of the mock services with the given name
    listall: Lists all the currently registered services
    list <name>: Lists an instance of the service with the given name
    quit: Quit the program

    > add orders
    Service orders registered on port 437
    > add orders
    Service orders registered on port 1238
    > listall
    Looking up orders
    	orders: http://192.168.0.102:1238
    	orders: http://192.168.0.102:437


From another prompt and same directory as before, run

    $ mvn install -Prun

    > list orders
    Looking up orders
    	orders: http://192.168.0.102:1238
    	orders: http://192.168.0.102:437
    >

> Feel free to add/delete services and check from other terminal.

> Also shutdown the ZooKeeper Server and bring it back up and see if you can still see all the services registered.

### Checking results with ZooKeeper Client

    $ bin/zkCli.sh -server 127.0.0.1:2181

    [zk: 127.0.0.1:2181(CONNECTED) 1] ls /myapp
    [services]
    [zk: 127.0.0.1:2181(CONNECTED) 2] ls /myapp/services/orders
    [92cd30ef-d015-43d0-a7c3-730eb088f158, 897a31a6-b862-41e4-a63c-2b4210f3147c]
    [zk: 127.0.0.1:2181(CONNECTED) 3] get /myapp/services/orders/

    92cd30ef-d015-43d0-a7c3-730eb088f158   897a31a6-b862-41e4-a63c-2b4210f3147c
    [zk: 127.0.0.1:2181(CONNECTED) 3] get /myapp/services/orders/92cd30ef-d015-43d0-a7c3-730eb088f158
    {"name":"orders","id":"92cd30ef-d015-43d0-a7c3-730eb088f158","address":"192.168.0.102","port":1238,"sslPort":null,"payload":{"@class":"poc.curator.InstanceDetails","description":"orders"},"registrationTimeUTC":1458925251363,"serviceType":"DYNAMIC","uriSpec":{"parts":[{"value":"scheme","variable":true},{"value":"://","variable":false},{"value":"address","variable":true},{"value":":","variable":false},{"value":"port","variable":true}]}}
    cZxid = 0x48
    ctime = Fri Mar 25 10:00:51 PDT 2016
    mZxid = 0x48
    mtime = Fri Mar 25 10:00:51 PDT 2016
    pZxid = 0x48
    cversion = 0
    dataVersion = 0
    aclVersion = 0
    ephemeralOwner = 0x153aeb83ec60000
    dataLength = 438
    numChildren = 0
    [zk: 127.0.0.1:2181(CONNECTED) 4]


## License

MIT
