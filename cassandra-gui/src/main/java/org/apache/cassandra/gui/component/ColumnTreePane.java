package org.apache.cassandra.gui.component;

import java.awt.Cursor;
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
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.apache.cassandra.client.Client;
import org.apache.cassandra.gui.control.callback.RepaintCallback;
import org.apache.cassandra.unit.Cell;
import org.apache.cassandra.unit.Key;
import org.apache.cassandra.unit.SColumn;
import org.apache.cassandra.unit.Unit;

public class ColumnTreePane extends JPanel {
    private static final long serialVersionUID = -4236268406209844637L;

    private class PopupAction extends AbstractAction {
        private static final long serialVersionUID = 4235052996425858520L;

        public static final int OPERATION_PROPERTIES = 0;
        public static final int OPERATION_REMOVE = 2;

        private int operation;
        private DefaultMutableTreeNode node;
        private Cell c;

        public PopupAction(String name,int operation, DefaultMutableTreeNode node, Cell c) {
            this.operation = operation;
            this.node = node;
            this.c = c;
            putValue(Action.NAME, name);
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            switch (operation) {
            case OPERATION_PROPERTIES:
                CellPropertiesDlg cpdlg = new CellPropertiesDlg(c.getName(), c.getValue());
                cpdlg.setVisible(true);
                break;
            case OPERATION_REMOVE:
                int status = JOptionPane.showConfirmDialog(null,
                                                           "Delete a column " + c.getName() + "?",
                                                           "confirm",
                                                           JOptionPane.YES_NO_OPTION,
                                                           JOptionPane.QUESTION_MESSAGE);
                if (status == JOptionPane.YES_OPTION) {
                    try {
                        Unit parent = c.getParent();
                        if (parent instanceof Key) {
                            Key k = (Key) parent;
                            client.removeColumn(keyspace, columnFamily, k.getName(), c.getName());
                        } else if (parent instanceof SColumn) {
                            SColumn s = (SColumn) parent;
                            Key k = (Key) s.getParent();
                            client.removeColumn(keyspace, columnFamily, k.getName(), s.getName(), c.getName());
                        }
                        DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
                        if (parentNode != null) {
                            parentNode.remove(node);
                            treeModel.reload(parentNode);
                        }
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(null, "error: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                break;
            }
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
                    popup.add(new PopupAction("properties", PopupAction.OPERATION_PROPERTIES, node, c));
                    popup.add(new PopupAction("remove", PopupAction.OPERATION_REMOVE, node, c));
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        }
    }

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");

    private Client client;

    private String keyspace;
    private String columnFamily;

    private RepaintCallback rCallback;
    private JScrollPane scrollPane;
    private JTree tree;
    private DefaultTreeModel treeModel;

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

    public void showRows(String keyspace, String columnFamily, String startKey, String endKey, int rows) {
        this.keyspace = keyspace;
        this.columnFamily = columnFamily;

        DefaultMutableTreeNode columnFamilyNode = new DefaultMutableTreeNode(columnFamily);
        treeModel = new DefaultTreeModel(columnFamilyNode);
        tree = new JTree(treeModel);
        tree.setRootVisible(true);
        tree.addMouseListener(new MousePopup());

        try {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            Map<String, Key> l =
                client.listKeyAndValues(keyspace, columnFamily, startKey, endKey, rows);
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
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
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
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
