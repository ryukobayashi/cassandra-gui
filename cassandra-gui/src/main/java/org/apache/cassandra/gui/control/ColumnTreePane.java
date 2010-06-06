package org.apache.cassandra.gui.control;

import java.awt.Dimension;
import java.text.SimpleDateFormat;
import java.util.Map;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.cassandra.Cell;
import org.apache.cassandra.Key;
import org.apache.cassandra.SColumn;
import org.apache.cassandra.client.Client;
import org.apache.cassandra.gui.control.callback.RepaintCallback;

public class ColumnTreePane extends JPanel implements TreeSelectionListener {
    private static final long serialVersionUID = -4236268406209844637L;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");

    private Client client;
    private RepaintCallback rCallback;
    private JScrollPane scrollPane;
    private JTree tree;

    public ColumnTreePane(Client client) {
        this.client = client;
        scrollPane = new JScrollPane();
        add(scrollPane);
        repaint();
    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {
    }

    @Override
    public void repaint() {
        if (scrollPane != null && rCallback != null) {
            Dimension d = rCallback.callback();
            scrollPane.setPreferredSize(new Dimension((int) d.getWidth() - 10,
                                                      (int) d.getHeight() - 10));
            scrollPane.repaint();
        }
        super.repaint();
    }

    public void showRows(String keyspaceName, String columnFamilyName, String startKey, String endKey, int rows) {
        DefaultMutableTreeNode columnFamilyNode =
            new DefaultMutableTreeNode(columnFamilyName);
        tree = new JTree(columnFamilyNode);
        tree.setRootVisible(true);

        try {
            Map<String, Key> l =
                client.listKeyAndValues(keyspaceName, columnFamilyName, startKey, endKey, rows);
            for (String keyName : l.keySet()) {
                Key k = l.get(keyName);
                DefaultMutableTreeNode keyNode = new DefaultMutableTreeNode(k.getName());
                columnFamilyNode.add(keyNode);
                if (k.isSuperColumn()) {
                    for (String sName : k.getSColumns().keySet()) {
                        SColumn sc = k.getSColumns().get(sName);
                        DefaultMutableTreeNode scNode = new DefaultMutableTreeNode(sc.getName());
                        keyNode.add(scNode);
                        for (String cName : sc.getCells().keySet()) {
                            Cell c = sc.getCells().get(cName);
                            DefaultMutableTreeNode cellNode =
                                new DefaultMutableTreeNode(c.getName() + "=" + c.getValue() + ", " + DATE_FORMAT.format(c.getDate()));
                            scNode.add(cellNode);
                        }
                    }
                } else {
                    for (String cName : k.getCells().keySet()) {
                        Cell c = k.getCells().get(cName);
                        DefaultMutableTreeNode cellNode =
                            new DefaultMutableTreeNode(c.getName() + "=" + c.getValue() + ", " + DATE_FORMAT.format(c.getDate()));
                        keyNode.add(cellNode);
                    }
                }
            }
        } catch (Exception e) {
            JOptionPane.showConfirmDialog(null, "error");
            e.printStackTrace();
        }

        scrollPane.getViewport().setView(tree);
        repaint();
    }

    /**
     * @param rCallback the rCallback to set
     */
    public void setrCallback(RepaintCallback rCallback) {
        this.rCallback = rCallback;
    }
}
