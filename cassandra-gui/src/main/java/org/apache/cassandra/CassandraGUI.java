package org.apache.cassandra;

import java.awt.BorderLayout;

import javax.swing.JFrame;

import org.apache.cassandra.gui.control.ColumnTreePane;
import org.apache.cassandra.gui.control.ConnectionDlg;
import org.apache.cassandra.gui.control.KeyspaceTreePanel;
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

        final ColumnTreePane columnTreePane = new ColumnTreePane(dlg.getClient());
        add(columnTreePane, BorderLayout.EAST);

        final KeyspaceTreePanel keyspaceTreePanel = new KeyspaceTreePanel(dlg.getClient());
        keyspaceTreePanel.setCallback(new SelectedColumnFamilyCallback() {
            @Override
            public void callback(String keyspaceName,
                                 String columnFamilyName,
                                 String startKey,
                                 String endKey,
                                 int rows) {
                columnTreePane.showRows(keyspaceName, columnFamilyName, startKey, endKey, rows);
            }
        });
        add(keyspaceTreePanel, BorderLayout.WEST);

        setSize(850, 650);
        setLocationRelativeTo(null);
        keyspaceTreePanel.repaint();
        setVisible(true);

        return true;
    }
}
