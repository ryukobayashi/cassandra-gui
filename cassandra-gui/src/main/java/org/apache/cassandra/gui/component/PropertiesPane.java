package org.apache.cassandra.gui.component;

import java.awt.Dimension;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.apache.cassandra.client.Client;
import org.apache.cassandra.gui.control.callback.RepaintCallback;
import org.apache.thrift.TException;

public class PropertiesPane extends JPanel {
    private static final long serialVersionUID = 1452324774722196104L;

    private static final String COLUMN_VERSION = "version";

    private final String[] columns = { "name", "value" }; 

    private Client client;
    private RepaintCallback rCallback;
    private JScrollPane scrollPane;
    private JTable table;

    private DefaultTableModel tableModel = new DefaultTableModel(columns, 0);
    
    public PropertiesPane(Client client) {
        this.client = client;

        table = new JTable(tableModel);
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
            tableModel.addRow(new String[] {COLUMN_VERSION, client.getVersion()});
        } catch (TException e) {
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
