package org.apache.cassandra;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;

import org.apache.cassandra.client.Client;
import org.apache.cassandra.gui.component.dialog.ConnectionDialog;
import org.apache.cassandra.gui.component.panel.ColumnTreePanel;
import org.apache.cassandra.gui.component.panel.KeyspaceTreePanel;
import org.apache.cassandra.gui.component.panel.PropertiesPanel;
import org.apache.cassandra.gui.control.callback.PropertiesCallback;
import org.apache.cassandra.gui.control.callback.RepaintCallback;
import org.apache.cassandra.gui.control.callback.SelectedColumnFamilyCallback;

public class CassandraGUI extends JFrame {
	
    private static final long serialVersionUID = -7402974525268824644L;

    private Client client;
    
    /**
     * @param args
     */
    public static void main(String[] args) {
    	
    	CassandraGUI gui = new CassandraGUI("Cassandra GUI v0.6.1");
        
        String host = (args.length > 0 ? args[0] : null);
        int thriftPort = (args.length > 1 ? Integer.valueOf(args[1]) : Client.DEFAULT_THRIFT_PORT);
        int jmxPort = (args.length > 2 ? Integer.valueOf(args[2]) : Client.DEFAULT_JMX_PORT);
        
        boolean done = false;
        
        if (host != null)
        	done = gui.startConnection(host, thriftPort, jmxPort);

        if (!done && !gui.showConnectionDialog())
        	System.exit(0);
        
        if (!gui.createAndShow())
            System.exit(0);
    }

    public CassandraGUI(String title) {
        super(title);
    }

    public boolean showConnectionDialog() {
        ConnectionDialog dlg = new ConnectionDialog(this);
        boolean done = false;
        if (!dlg.wasCancelled()) {
	        do {
	        	if (dlg.wasCancelled()) {
	        		break;
	        	}
	        	done = this.startConnection(dlg.getHost(), dlg.getThriftPort(), dlg.getJmxPort());
	        	if (!done) {
	        		JOptionPane.showMessageDialog(null, "Connection failed.");
	        		dlg.setVisible(true);
	        	}
	        } while (!done);
        }
		return done;
    }
    
    public boolean startConnection(String host, int thriftPort, int jmxPort) {
        client = new Client(host, thriftPort, jmxPort);
        try {
			client.connect();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
    }
    
    public boolean createAndShow() {
    	
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
        	public void windowClosing(WindowEvent e) {
        		if (client != null)
        			client.disconnect();
        	}
		});
        
        Toolkit.getDefaultToolkit().setDynamicLayout(true);

        final PropertiesPanel propertiesPane = new PropertiesPanel(this.getClient());
        final ColumnTreePanel columnTreePane = new ColumnTreePanel(this.getClient());
        final KeyspaceTreePanel keyspaceTreePanel = new KeyspaceTreePanel(this.getClient());
        
        keyspaceTreePanel.setcCallback(new SelectedColumnFamilyCallback() {
            @Override
            public void rangeCallback(String keyspaceName,
                                      String columnFamilyName,
                                      String startKey,
                                      String endKey,
                                      int rows) {
                columnTreePane.showRows(keyspaceName, columnFamilyName, startKey, endKey, rows);
            }

            @Override
            public void getCacllback(String keyspace, String columnFamily, String key) {
                columnTreePane.showRow(keyspace, columnFamily, key);
            }
        });
        keyspaceTreePanel.setPropertiesCallback(new PropertiesCallback() {
            @Override
            public void clusterCallback() {
                propertiesPane.showClusterProperties();
                columnTreePane.clear();
            }

            @Override
            public void keyspaceCallback(String keyspace) {
                propertiesPane.showKeyspaceProperties(keyspace);
                columnTreePane.clear();
            }

            @Override
            public void columnFamilyCallback(String keyspace, String columnFamily) {
                propertiesPane.showColumnFamilyProperties(keyspace, columnFamily);
                columnTreePane.clear();
            }
        });

        final JSplitPane rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        rightSplitPane.setLeftComponent(propertiesPane);
        rightSplitPane.setRightComponent(columnTreePane);
        rightSplitPane.setOneTouchExpandable(true);
        rightSplitPane.setDividerSize(6);

        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(keyspaceTreePanel);
        splitPane.setRightComponent(rightSplitPane);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerSize(6);

        add(splitPane);
        setBounds(10, 10, 850, 650);
        setLocationRelativeTo(null);

        setVisible(true);

        splitPane.getLeftComponent().setSize(keyspaceTreePanel.getPreferredSize());
        keyspaceTreePanel.setrCallback(new RepaintCallback() {
            @Override
            public Dimension callback() {
                return splitPane.getLeftComponent().getSize();
            }
        });

        splitPane.getRightComponent().setSize(new Dimension(850 - keyspaceTreePanel.getPreferredSize().width,
                                                            keyspaceTreePanel.getPreferredSize().height));
        columnTreePane.setrCallback(new RepaintCallback() {
            @Override
            public Dimension callback() {
                return rightSplitPane.getRightComponent().getSize();
            }
        });
        propertiesPane.setrCallback(new RepaintCallback() {
            @Override
            public Dimension callback() {
                return rightSplitPane.getLeftComponent().getSize();
            }
        });

        keyspaceTreePanel.repaint();
        keyspaceTreePanel.revalidate();
        columnTreePane.repaint();
        columnTreePane.revalidate();
        propertiesPane.repaint();
        propertiesPane.revalidate();

        return true;
    }
    
    public Client getClient() {
        return client;
    }

}
