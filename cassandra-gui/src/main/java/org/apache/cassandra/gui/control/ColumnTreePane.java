package org.apache.cassandra.gui.control;

import java.awt.Dimension;
import java.text.SimpleDateFormat;
import java.util.List;

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

public class ColumnTreePane extends JPanel implements TreeSelectionListener {
    private static final long serialVersionUID = -4236268406209844637L;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");

    private Client client;
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
        if (scrollPane != null) {
            int width =
                getParent() == null || getParent().getWidth() == 0 ?
                        655 : getParent().getWidth() - 10;
            int height =
                getParent() == null || getParent().getHeight() == 0 ?
                        615 : getParent().getHeight() - 15;
            scrollPane.setPreferredSize(new Dimension(width, height));
        }

        super.repaint();
    }

    public void showRows(String keyspaceName, String columnFamilyName, String startKey, String endKey, int rows) {
        DefaultMutableTreeNode columnFamilyNode =
            new DefaultMutableTreeNode(columnFamilyName);
        tree = new JTree(columnFamilyNode);
        tree.setRootVisible(true);

        try {
            List<Key> l =
                client.listKeyAndValues(keyspaceName, columnFamilyName, startKey, endKey, rows);
            for (Key k : l) {
                DefaultMutableTreeNode keyNode = new DefaultMutableTreeNode(k.getName());
                columnFamilyNode.add(keyNode);
                if (k.isSuperColumn()) {
                    for (SColumn sc : k.getSColumns()) {
                        DefaultMutableTreeNode scNode = new DefaultMutableTreeNode(sc.getName());
                        keyNode.add(scNode);
                        for (Cell c : sc.getCells()) {
                            DefaultMutableTreeNode cellNode =
                                new DefaultMutableTreeNode(c.getName() + "=" + c.getValue() + ", " + DATE_FORMAT.format(c.getDate()));
                            scNode.add(cellNode);
                        }
                    }
                } else {
                    for (Cell c : k.getCells()) {
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
}
