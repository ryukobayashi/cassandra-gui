package org.apache.cassandra.gui.component.panel;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import org.apache.cassandra.client.Client;
import org.apache.cassandra.gui.component.dialog.KeyRangeDlg;
import org.apache.cassandra.gui.control.callback.PropertiesCallback;
import org.apache.cassandra.gui.control.callback.RepaintCallback;
import org.apache.cassandra.gui.control.callback.SelectedColumnFamilyCallback;
import org.apache.cassandra.thrift.NotFoundException;
import org.apache.thrift.TException;

public class KeyspaceTreePanel extends JPanel implements TreeSelectionListener {
    private static final long serialVersionUID = 5481365703729222288L;

    private class PopupAction extends AbstractAction {
        private static final long serialVersionUID = 4235052996425858520L;

        public static final int ROWS_1000 = 1000;

        private boolean keyRange;

        public PopupAction(String name, boolean keyRange) {
            this.keyRange = keyRange;
            putValue(Action.NAME, name);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (lastSelectedKeysapce == null ||
                lastSelectedColumnFamily == null) {
                return;
            }

            String startKey = "";
            String endKey = "";

            if (keyRange) {
                KeyRangeDlg krd = new KeyRangeDlg();
                krd.setVisible(true);
                if (krd.isCancel()) {
                    return;
                }

                startKey = krd.getStartKey();
                endKey = krd.getEndKey();
            }

            cCallback.callback(lastSelectedKeysapce,
                               lastSelectedColumnFamily,
                               startKey,
                               endKey,
                               ROWS_1000);
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
                if (node != null && node.getChildCount() == 0) {
                    String columnFamily = (String) node.getUserObject();
                    lastSelectedKeysapce = keyspaceMap.get(columnFamily);
                    lastSelectedColumnFamily = columnFamily;

                    JPopupMenu popup = new JPopupMenu();
                    popup.add(new PopupAction("show 1000 rows", false));
                    popup.add(new PopupAction("key range rows", true));
                    popup.show(e.getComponent(), e.getX(), e.getY());
                } else {
                    lastSelectedKeysapce = null;
                    lastSelectedColumnFamily = null;
                }
            }
        }
    }

    private static final int TREE_CLUSTER = 1;
    private static final int TREE_KEYSPACE = 2;
    private static final int TREE_COLUMN_FAMILY = 3;

    private PropertiesCallback propertiesCallback; 
    private SelectedColumnFamilyCallback cCallback;
    private RepaintCallback rCallback;

    private Map<String, String> keyspaceMap = new HashMap<String, String>();
    private JScrollPane scrollPane;
    private String lastSelectedKeysapce;
    private String lastSelectedColumnFamily;
    private JTree tree;

    public KeyspaceTreePanel(Client client) {
        try {
            DefaultMutableTreeNode clusterNode =
                new DefaultMutableTreeNode(client.describeClusterName());
            tree = new JTree(clusterNode);
            tree.setRootVisible(true);
            tree.addMouseListener(new MousePopup());
            tree.addTreeSelectionListener(this);

            List<String> ks = new ArrayList<String>(client.getKeyspaces());
            Collections.sort(ks);
            for (String keyspace : ks) {
                DefaultMutableTreeNode keyspaceNode = new DefaultMutableTreeNode(keyspace);
                clusterNode.add(keyspaceNode);
                try {
                    Set<String> cfs = client.getColumnFamilys(keyspace);
                    for (String columnFamily : cfs) {
                        keyspaceNode.add(new DefaultMutableTreeNode(columnFamily));
                        keyspaceMap.put(columnFamily, keyspace);
                    }
                } catch (NotFoundException e) {
                    JOptionPane.showMessageDialog(null, "error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (TException e) {
            JOptionPane.showMessageDialog(null, "error: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        scrollPane = new JScrollPane();
        scrollPane.getViewport().setView(tree);
        add(scrollPane);
        repaint();
    }

    @Override
    public void valueChanged(TreeSelectionEvent e) {
        String keyspace;
        String columnFamily;

        switch (e.getPath().getPathCount()) {
        case TREE_CLUSTER:
            propertiesCallback.clusterCallback();
            break;
        case TREE_KEYSPACE:
            keyspace = e.getPath().getPath()[TREE_KEYSPACE - 1].toString();
            propertiesCallback.keyspaceCallback(keyspace);
            break;
        case TREE_COLUMN_FAMILY:
            keyspace = e.getPath().getPath()[TREE_KEYSPACE - 1].toString();
            columnFamily = e.getPath().getPath()[TREE_COLUMN_FAMILY - 1].toString();
            propertiesCallback.columnFamilyCallback(keyspace, columnFamily);
            break;
        }
    }

    @Override
    public void repaint() {
        if (scrollPane != null && rCallback != null) {
            Dimension d = rCallback.callback();
            scrollPane.setPreferredSize(new Dimension(d.width - 10,
                                                      d.height - 10));
            scrollPane.repaint();
        }
        super.repaint();
    }

    /**
     * @param propertiesCallback the propertiesCallback to set
     */
    public void setPropertiesCallback(PropertiesCallback propertiesCallback) {
        this.propertiesCallback = propertiesCallback;
    }

    /**
     * @param cCallback the cCallback to set
     */
    public void setcCallback(SelectedColumnFamilyCallback cCallback) {
        this.cCallback = cCallback;
    }

    /**
     * @param rCallback the rCallback to set
     */
    public void setrCallback(RepaintCallback rCallback) {
        this.rCallback = rCallback;
    }
}
