package org.apache.cassandra.gui.component;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.apache.cassandra.Cell;
import org.apache.cassandra.Key;
import org.apache.cassandra.SColumn;
import org.apache.cassandra.client.Client;
import org.apache.cassandra.gui.control.callback.RepaintCallback;

public class ColumnTreePane extends JPanel {
    private static final long serialVersionUID = -4236268406209844637L;

    private class PopupAction extends AbstractAction {
        private static final long serialVersionUID = 4235052996425858520L;

        private Cell c;

        public PopupAction(String name, Cell c) {
            this.c = c;
            putValue(Action.NAME, name);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            CellPropertiesDlg cpdlg = new CellPropertiesDlg(c.getName(), c.getValue());
            cpdlg.setVisible(true);
        }
    }

    private class MousePopup extends MouseAdapter {
        @Override
        public void mouseReleased(MouseEvent e) {
            if (SwingUtilities.isRightMouseButton(e)) {
                TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                if (path == null) {
                    return;
                }

                tree.setSelectionPath(path);
                DefaultMutableTreeNode node =
                    (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                Cell c = cellMap.get(node);
                if (node != null && node.getChildCount() == 0 && c != null) {
                    JPopupMenu popup = new JPopupMenu();
                    popup.add(new PopupAction("Properties", c));
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        }
    }

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");

    private Client client;
    private RepaintCallback rCallback;
    private JScrollPane scrollPane;
    private JTree tree;

    private Map<DefaultMutableTreeNode, Cell> cellMap = new HashMap<DefaultMutableTreeNode, Cell>();

    public ColumnTreePane(Client client) {
        this.client = client;
        scrollPane = new JScrollPane();
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

    public void showRows(String keyspaceName, String columnFamilyName, String startKey, String endKey, int rows) {
        DefaultMutableTreeNode columnFamilyNode =
            new DefaultMutableTreeNode(columnFamilyName);
        tree = new JTree(columnFamilyNode);
        tree.setRootVisible(true);
        tree.addMouseListener(new MousePopup());

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
                            cellMap.put(cellNode, c);
                        }
                    }
                } else {
                    for (String cName : k.getCells().keySet()) {
                        Cell c = k.getCells().get(cName);
                        DefaultMutableTreeNode cellNode =
                            new DefaultMutableTreeNode(c.getName() + "=" + c.getValue() + ", " + DATE_FORMAT.format(c.getDate()));
                        keyNode.add(cellNode);
                        cellMap.put(cellNode, c);
                    }
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "error: " + e.getMessage());
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
