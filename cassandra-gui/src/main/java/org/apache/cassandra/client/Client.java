package org.apache.cassandra.client;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.cassandra.Cell;
import org.apache.cassandra.Key;
import org.apache.cassandra.SColumn;
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
import org.apache.cassandra.thrift.UnavailableException;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

public class Client {
    public static final String DEFAULT_THRIFT_HOST = "localhost";
    public static final int DEFAULT_THRIFT_PORT = 9160;

    private TTransport transport = null;
    private TProtocol protocol = null;
    private Cassandra.Client client = null;

    private boolean connected = false;
    private String host;
    private int port;

    public Client() {
        this(DEFAULT_THRIFT_HOST, DEFAULT_THRIFT_PORT);
    }
    
    public Client(String host) {
        this(host, DEFAULT_THRIFT_PORT);
    }

    public Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() throws TTransportException {
        if (!connected) {
            transport = new TSocket(host, port);
            protocol = new TBinaryProtocol(transport);
            client = new Cassandra.Client(protocol);
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

    public String getClusterName() throws TException {
        return client.describe_cluster_name();
    }

    public String getVersion() throws TException {
        return client.describe_version();
    }

    public Set<String> getKeyspaces() throws TException {
        return client.describe_keyspaces();
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
