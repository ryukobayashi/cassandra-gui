package org.apache.cassandra.gui.component;

import java.awt.Dimension;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import org.apache.cassandra.client.Client;
import org.apache.cassandra.gui.control.callback.RepaintCallback;

public class PropertiesPane extends JPanel {
    private static final long serialVersionUID = 1452324774722196104L;

    private static final String COLUMN_VERSION = "api version";
    private static final String COLUMN_NUMBER_OF_KEYSPACE = "Number of Keyspace";
    private static final String COLUMN_NUMBER_OF_CALUMN_FAMILY = "Number of CalumnFamily";

    private final String[] columns = { "name", "value" }; 

    private Client client;
    private RepaintCallback rCallback;
    private JScrollPane scrollPane;
    private JTable table;

    private DefaultTableModel tableModel;
    
    public PropertiesPane(Client client) {
        this.client = client;

        tableModel = new DefaultTableModel(columns, 0) {
            private static final long serialVersionUID = 7088445834198028640L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        scrollPane = new JScrollPane(table);
        add(scrollPane);
        repaint();
    }

    @Override
    public void repaint() {
        if (scrollPane != null && rCallback != null) {
            Dimension d = rCallback.callback();
            scrollPane.setPreferredSize(new Dimension(d.width - 5,
                                                      d.height - 5));
            scrollPane.repaint();
        }
        super.repaint();
    }

    public void showClusterProperties() {
        try {
            tableModel.setRowCount(0);
            tableModel.addRow(new String[] {COLUMN_VERSION, client.descriveVersion()});
            int n = client.getKeyspaces().size();
            tableModel.addRow(new String[] {COLUMN_NUMBER_OF_KEYSPACE, String.valueOf(n)});
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "error: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        repaint();
    }

    public void showKeyspaceProperties(String keyspace) {
        try {
            tableModel.setRowCount(0);
            int n = client.getColumnFamilys(keyspace).size();
            tableModel.addRow(new String[] {COLUMN_NUMBER_OF_CALUMN_FAMILY, String.valueOf(n)});
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "error: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        repaint();
    }

    public void showColumnFamilyProperties(String keyspace, String columnFamily) {
        try {
            tableModel.setRowCount(0);
            Map<String, String> m = client.getColumnFamily(keyspace, columnFamily);
            for (Map.Entry<String, String> e : m.entrySet()) {
                tableModel.addRow(new String[] {e.getKey(), e.getValue()});
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "error: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        repaint();        
    }

    /**
     * @param rCallback the rCallback to set
     */
    public void setrCallback(RepaintCallback rCallback) {
        this.rCallback = rCallback;
    }
}
