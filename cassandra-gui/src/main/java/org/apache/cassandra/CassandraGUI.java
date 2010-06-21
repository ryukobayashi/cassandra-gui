package org.apache.cassandra;


import java.awt.Dimension;
import java.awt.Toolkit;

import javax.swing.JFrame;
import javax.swing.JSplitPane;

import org.apache.cassandra.gui.component.ColumnTreePane;
import org.apache.cassandra.gui.component.ConnectionDlg;
import org.apache.cassandra.gui.component.KeyspaceTreePanel;
import org.apache.cassandra.gui.component.PropertiesPane;
import org.apache.cassandra.gui.control.callback.PropertiesCallback;
import org.apache.cassandra.gui.control.callback.RepaintCallback;
import org.apache.cassandra.gui.control.callback.SelectedColumnFamilyCallback;

public class CassandraGUI extends JFrame {
    private static final long serialVersionUID = -7402974525268824644L;

    /**
     * @param args
     */
    public static void main(String[] args) {
        CassandraGUI gui = new CassandraGUI("Cassandra GUI");
        if (!gui.createAndShow()) {
            System.exit(0);
        }
    }

    public CassandraGUI(String title) {
        super(title);
    }

    public boolean createAndShow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        ConnectionDlg dlg = new ConnectionDlg(this);
        if (dlg.getClient() == null) {
            return false;
        }

        Toolkit.getDefaultToolkit().setDynamicLayout(true);

        final PropertiesPane propertiesPane = new PropertiesPane(dlg.getClient());
        final ColumnTreePane columnTreePane = new ColumnTreePane(dlg.getClient());
        final KeyspaceTreePanel keyspaceTreePanel = new KeyspaceTreePanel(dlg.getClient());
        keyspaceTreePanel.setcCallback(new SelectedColumnFamilyCallback() {
            @Override
            public void callback(String keyspaceName,
                                 String columnFamilyName,
                                 String startKey,
                                 String endKey,
                                 int rows) {
                columnTreePane.showRows(keyspaceName, columnFamilyName, startKey, endKey, rows);
            }
        });
        keyspaceTreePanel.setPropertiesCallback(new PropertiesCallback() {
            @Override
            public void clusterCallback() {
                propertiesPane.showClusterProperties();
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
}
