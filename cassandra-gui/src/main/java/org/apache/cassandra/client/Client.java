package org.apache.cassandra.client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.cassandra.Cell;
import org.apache.cassandra.Key;
import org.apache.cassandra.NodeInfo;
import org.apache.cassandra.RingNode;
import org.apache.cassandra.SColumn;
import org.apache.cassandra.Tpstats;
import org.apache.cassandra.concurrent.IExecutorMBean;
import org.apache.cassandra.dht.Range;
import org.apache.cassandra.thrift.Cassandra;
import org.apache.cassandra.thrift.Column;
import org.apache.cassandra.thrift.ColumnOrSuperColumn;
import org.apache.cassandra.thrift.ColumnParent;
import org.apache.cassandra.thrift.ConsistencyLevel;
import org.apache.cassandra.thrift.InvalidRequestException;
import org.apache.cassandra.thrift.KeyRange;
import org.apache.cassandra.thrift.KeySlice;
import org.apache.cassandra.thrift.NotFoundException;
import org.apache.cassandra.thrift.SlicePredicate;
import org.apache.cassandra.thrift.SliceRange;
import org.apache.cassandra.thrift.SuperColumn;
import org.apache.cassandra.thrift.TimedOutException;
import org.apache.cassandra.thrift.TokenRange;
import org.apache.cassandra.thrift.UnavailableException;
import org.apache.cassandra.tools.NodeProbe;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

public class Client {
    public static final String DEFAULT_THRIFT_HOST = "localhost";
    public static final int DEFAULT_THRIFT_PORT = 9160;
    public static final int DEFAULT_JMX_PORT = 8080;

    private TTransport transport;
    private TProtocol protocol;
    private Cassandra.Client client;
    private NodeProbe probe;

    private boolean connected = false;
    private String host;
    private int thriftPort;
    private int jmxPort;

    public Client() {
        this(DEFAULT_THRIFT_HOST, DEFAULT_THRIFT_PORT, DEFAULT_JMX_PORT);
    }

    public Client(String host) {
        this(host, DEFAULT_THRIFT_PORT, DEFAULT_JMX_PORT);
    }

    public Client(String host, int thriftPort, int jmxPort) {
        this.host = host;
        this.thriftPort = thriftPort;
        this.jmxPort = jmxPort;
    }

    public void connect() throws TTransportException, IOException, InterruptedException {
        if (!connected) {
            transport = new TSocket(host, thriftPort);
            protocol = new TBinaryProtocol(transport);
            client = new Cassandra.Client(protocol);
            probe = new NodeProbe(host, jmxPort);
            transport.open();
            connected = true;
        }
    }

    public void disconnect() {
        if (connected) {
            transport.close();
            connected = false;
        }
    }

    public boolean isConnected() {
        return connected;
    }

    public String describeClusterName() throws TException {
        return client.describe_cluster_name();
    }

    public String descriveVersion() throws TException {
        return client.describe_version();
    }

    public String getTokenMap() throws TException {
        return client.get_string_property("token map");
    }

    public String getConfigFile() throws TException {
        return client.get_string_property("config file");
    }

    public List<TokenRange> describeRing(String keyspace) throws TException, InvalidRequestException {
        return client.describe_ring(keyspace);
    }

    public RingNode listRing() {
        RingNode r = new RingNode();

        r.setRangeMap(probe.getRangeToEndPointMap(null));

        List<Range> ranges = new ArrayList<Range>(r.getRangeMap().keySet());
        Collections.sort(ranges);
        r.setRanges(ranges);

        r.setLiveNodes(probe.getLiveNodes());
        r.setDeadNodes(probe.getUnreachableNodes());
        r.setLoadMap(probe.getLoadMap());

        return r;
    }

    public NodeInfo getNodeInfo(String endpoint) throws IOException, InterruptedException {
        NodeProbe p = new NodeProbe(endpoint, jmxPort);

        NodeInfo ni = new NodeInfo();
        ni.setEndpoint(endpoint);
        ni.setLoad(p.getLoadString());
        ni.setGenerationNumber(p.getCurrentGenerationNumber());
        ni.setUptime(p.getUptime() / 1000);

        MemoryUsage heapUsage = p.getHeapMemoryUsage();
        ni.setMemUsed((double) heapUsage.getUsed() / (1024 * 1024));
        ni.setMemMax((double) heapUsage.getMax() / (1024 * 1024));

        return ni;
    }

    public List<Tpstats> getTpstats(String endpoint) throws IOException, InterruptedException {
        List<Tpstats> l = new ArrayList<Tpstats>();

        NodeProbe p = new NodeProbe(endpoint, jmxPort);
        Iterator<Map.Entry<String, IExecutorMBean>> threads = p.getThreadPoolMBeanProxies();
        for (;threads.hasNext();) {
            Map.Entry<String, IExecutorMBean> thread = threads.next();

            Tpstats tp = new Tpstats();
            tp.setPoolName(thread.getKey());

            IExecutorMBean threadPoolProxy = thread.getValue();
            tp.setActiveCount(threadPoolProxy.getActiveCount());
            tp.setPendingTasks(threadPoolProxy.getPendingTasks());
            tp.setCompletedTasks(threadPoolProxy.getCompletedTasks());
            l.add(tp);
        }

        return l;
    }

    public Set<String> getKeyspaces() throws TException {
        return client.describe_keyspaces();
    }

    public Map<String, String> getColumnFamily(String keyspace, String columnFamily)
                throws NotFoundException, TException {
        return client.describe_keyspace(keyspace).get(columnFamily);
    }

    public Set<String> getColumnFamilys(String keyspace) throws NotFoundException, TException {
        Set<String> s = new TreeSet<String>();
        for (Map.Entry<String, Map<String, String>> entry : client.describe_keyspace(keyspace).entrySet()) {
            s.add(entry.getKey());
        }

        return s;
    }

    public Map<String, Key> listKeyAndValues(String keyspace, String columnFamily, String startKey, String endKey, int rows)
            throws InvalidRequestException, UnavailableException, TimedOutException, TException, UnsupportedEncodingException {
        Map<String, Key> l = new TreeMap<String, Key>();

        ColumnParent columnParent = new ColumnParent(columnFamily);

        KeyRange keyRange = new KeyRange(rows);
        keyRange.setStart_key(startKey);
        keyRange.setEnd_key(endKey);

        SliceRange sliceRange = new SliceRange();
        sliceRange.setStart(new byte[0]);
        sliceRange.setFinish(new byte[0]);

        SlicePredicate slicePredicate = new SlicePredicate();
        slicePredicate.setSlice_range(sliceRange);

        List<KeySlice> keySlices =
            client.get_range_slices(keyspace, columnParent, slicePredicate, keyRange, ConsistencyLevel.ONE);
        for (KeySlice keySlice : keySlices) {
            Key key = new Key(keySlice.getKey(), new TreeMap<String, SColumn>(), new TreeMap<String, Cell>());

            for (ColumnOrSuperColumn columns : keySlice.getColumns()) {
                key.setSuperColumn(columns.isSetSuper_column());
                if (columns.isSetSuper_column()) {
                    SuperColumn scol = columns.getSuper_column();
                    SColumn s = new SColumn(new String(scol.getName(), "UTF8"), new TreeMap<String, Cell>());
                    for (Column col : scol.getColumns()) {
                        Cell c = new Cell(new String(col.getName(), "UTF8"),
                                          new String(col.getValue(), "UTF8"),
                                          new Date(col.getTimestamp() / 1000));
                        s.getCells().put(c.getName(), c);
                    }

                    key.getSColumns().put(s.getName(), s);
                } else {
                    Column col = columns.getColumn();
                    Cell c = new Cell(new String(col.getName(), "UTF8"),
                                      new String(col.getValue(), "UTF8"),
                                      new Date(col.getTimestamp() / 1000));
                    key.getCells().put(c.getName(), c);
                }
            }

            l.put(key.getName(), key);
        }

        return l;
    }
}
