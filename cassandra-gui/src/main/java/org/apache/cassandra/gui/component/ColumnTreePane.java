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
        private Unit unit;

        public PopupAction(String name,int operation, DefaultMutableTreeNode node, Unit unit) {
            this.operation = operation;
            this.node = node;
            this.unit = unit;
            putValue(Action.NAME, name);
        }

        @Override
        public void actionPerformed(ActionEvent ae) {
            switch (operation) {
            case OPERATION_PROPERTIES:
                if (unit instanceof Cell) {
                    Cell c = (Cell) unit;
                    CellPropertiesDlg cpdlg = new CellPropertiesDlg(c.getName(), c.getValue());
                    cpdlg.setVisible(true);
                }
                break;
            case OPERATION_REMOVE:
                int status = JOptionPane.showConfirmDialog(null,
                                                           "Delete a column " + getName() + "?",
                                                           "confirm",
                                                           JOptionPane.YES_NO_OPTION,
                                                           JOptionPane.QUESTION_MESSAGE);
                if (status == JOptionPane.YES_OPTION) {
                    try {
                        if (unit instanceof Key) {
                            Key k = (Key) unit;
                            client.removeKey(keyspace, columnFamily, k.getName());

                            node.removeAllChildren();
                            treeModel.reload(node);
                        } else if (unit instanceof SColumn) {
                            SColumn s = (SColumn) unit;
                            Key k = (Key) s.getParent();
                            client.removeSuperColumn(keyspace, columnFamily, k.getName(), s.getName());
                            k.getSColumns().remove(s.getName());

                            removeNode((DefaultMutableTreeNode) node.getParent(), node);
                        } else {
                            Cell c = (Cell) unit;
                            Unit parent = c.getParent();
                            if (parent instanceof Key) {
                                Key k = (Key) parent;
                                client.removeColumn(keyspace, columnFamily, k.getName(), c.getName());
                                k.getCells().remove(c.getName());

                                removeNode((DefaultMutableTreeNode) node.getParent(), node);
                            } else if (parent instanceof SColumn) {
                                SColumn s = (SColumn) parent;
                                Key k = (Key) s.getParent();
                                client.removeColumn(keyspace, columnFamily, k.getName(), s.getName(), c.getName());
                                s.getCells().remove(c.getName());

                                DefaultMutableTreeNode parentNode = (DefaultMutableTreeNode) node.getParent();
                                removeNode(parentNode, node);

                                if (s.getCells().isEmpty()) {
                                    k.getSColumns().remove(s.getName());
                                    removeNode((DefaultMutableTreeNode) parentNode.getParent(), parentNode);
                                }
                            }
                        }
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(null, "error: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                break;
            }
        }

        private void removeNode(DefaultMutableTreeNode parentNode,
                                DefaultMutableTreeNode node) {
            if (parentNode != null && node != null) {
                node.removeFromParent();
                treeModel.reload(parentNode);
            }
        }

        private String getName() {
            if (unit instanceof Key) {
                return ((Key) unit).getName();
            } else if (unit instanceof SColumn) {
                return ((SColumn) unit).getName();
            }

            return ((Cell) unit).getName();
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
                Unit u = unitMap.get(node);
                if (node != null && u != null) {
                    JPopupMenu popup = new JPopupMenu();
                    if (u instanceof Key) {
                        popup.add(new PopupAction("remove", PopupAction.OPERATION_REMOVE, node, u));
                        popup.show(e.getComponent(), e.getX(), e.getY());
                    } else if (u instanceof SColumn) {
                        popup.add(new PopupAction("remove", PopupAction.OPERATION_REMOVE, node, u));
                        popup.show(e.getComponent(), e.getX(), e.getY());
                    } else if (u instanceof Cell) {
                        popup.add(new PopupAction("properties", PopupAction.OPERATION_PROPERTIES, node, u));
                        popup.add(new PopupAction("remove", PopupAction.OPERATION_REMOVE, node, u));
                        popup.show(e.getComponent(), e.getX(), e.getY());
                    }
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

    private Map<DefaultMutableTreeNode, Unit> unitMap = new HashMap<DefaultMutableTreeNode, Unit>();

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
                unitMap.put(keyNode, k);
                if (k.isSuperColumn()) {
                    for (String sName : k.getSColumns().keySet()) {
                        SColumn sc = k.getSColumns().get(sName);
                        DefaultMutableTreeNode scNode = new DefaultMutableTreeNode(sc.getName());
                        keyNode.add(scNode);
                        unitMap.put(scNode, sc);
                        for (String cName : sc.getCells().keySet()) {
                            Cell c = sc.getCells().get(cName);
                            DefaultMutableTreeNode cellNode =
                                new DefaultMutableTreeNode(c.getName() + "=" + c.getValue() + ", " + DATE_FORMAT.format(c.getDate()));
                            scNode.add(cellNode);
                            unitMap.put(cellNode, c);
                        }
                    }
                } else {
                    for (String cName : k.getCells().keySet()) {
                        Cell c = k.getCells().get(cName);
                        DefaultMutableTreeNode cellNode =
                            new DefaultMutableTreeNode(c.getName() + "=" + c.getValue() + ", " + DATE_FORMAT.format(c.getDate()));
                        keyNode.add(cellNode);
                        unitMap.put(cellNode, c);
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
