package org.apache.cassandra.gui.control;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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

import org.apache.cassandra.client.Client;
import org.apache.cassandra.gui.control.callback.SelectedColumnFamilyCallback;
import org.apache.cassandra.thrift.NotFoundException;
import org.apache.thrift.TException;

public class KeyspaceTreePanel extends JPanel {
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
                if (krd.isCancel()) {
                    return;
                }

                startKey = krd.getStartKey();
                endKey = krd.getEndKey();
            }

            callback.callback(lastSelectedKeysapce,
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

    private SelectedColumnFamilyCallback callback;

    private Map<String, String> keyspaceMap = new HashMap<String, String>();
    private JScrollPane scrollPane;
    private String lastSelectedKeysapce;
    private String lastSelectedColumnFamily;
    private JTree tree;

    public KeyspaceTreePanel(Client client) {
        try {
            DefaultMutableTreeNode clusterNode =
                new DefaultMutableTreeNode(client.getClusterName());
            tree = new JTree(clusterNode);
            tree.setRootVisible(true);
            tree.addMouseListener(new MousePopup());

            List<String> ks = new ArrayList<String>(client.getKeyspaces());
            Collections.sort(ks);
            for (String keyspace : ks) {
                DefaultMutableTreeNode keyspaceNode = new DefaultMutableTreeNode(keyspace);
                clusterNode.add(keyspaceNode);
                try {
                    List<String> cfs = client.getColumnFamilys(keyspace);
                    Collections.sort(cfs);
                    for (String columnFamily : cfs) {
                        keyspaceNode.add(new DefaultMutableTreeNode(columnFamily));
                        keyspaceMap.put(columnFamily, keyspace);
                    }
                } catch (NotFoundException e) {
                    JOptionPane.showConfirmDialog(null, "error");
                    e.printStackTrace();
                }
            }
        } catch (TException e) {
            JOptionPane.showConfirmDialog(null, "error");
            e.printStackTrace();
            return;
        }

        scrollPane = new JScrollPane();
        scrollPane.getViewport().setView(tree);
        add(scrollPane);
        repaint();
    }

    @Override
    public void repaint() {
        if (scrollPane != null) {
            int height =
                getParent() == null || getParent().getHeight() == 0 ?
                        615 : getParent().getHeight() - 15;
            scrollPane.setPreferredSize(new Dimension(180, height));
            scrollPane.repaint();
        }

        super.repaint();
    }

    /**
     * @param callback the callback to set
     */
    public void setCallback(SelectedColumnFamilyCallback callback) {
        this.callback = callback;
    }
}
