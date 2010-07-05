package org.apache.cassandra.gui.component;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.apache.cassandra.RingNode;
import org.apache.cassandra.client.Client;
import org.apache.cassandra.dht.Range;

import edu.uci.ics.jung.graph.ArchetypeVertex;
import edu.uci.ics.jung.graph.Vertex;
import edu.uci.ics.jung.graph.decorators.StringLabeller;
import edu.uci.ics.jung.graph.decorators.VertexPaintFunction;
import edu.uci.ics.jung.graph.decorators.VertexStringer;
import edu.uci.ics.jung.graph.impl.UndirectedSparseEdge;
import edu.uci.ics.jung.graph.impl.UndirectedSparseGraph;
import edu.uci.ics.jung.graph.impl.UndirectedSparseVertex;
import edu.uci.ics.jung.visualization.Layout;
import edu.uci.ics.jung.visualization.PluggableRenderer;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import edu.uci.ics.jung.visualization.contrib.CircleLayout;

public class RingDlg extends JDialog {
    private static final long serialVersionUID = 1543749033698969116L;

    private static final int NODE_STATUS_UP = 1;
    private static final int NODE_STATUS_DOWN = 2;
    private static final int NODE_STATUS_UNKNOWN = 3;

    private Client client;

    public RingDlg(Client client) {
        this.client = client;

        JScrollPane scrollPane = new JScrollPane(setupControls());
        
        JButton ok = new JButton("OK");
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(ok);
    
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
    
        add(panel);
    
        pack();
        setModalityType(ModalityType.DOCUMENT_MODAL);
        setTitle("Ring");
        setLocationRelativeTo(null);
        setModal(true);
    }

    private VisualizationViewer setupControls() {
        final RingNode ringNode = client.listRing();

        final Map<Range, List<String>> rangeMap = ringNode.getRangeMap();
        final List<Range> ranges = ringNode.getRanges();
        final Set<String> liveNodes = ringNode.getLiveNodes();
        final Set<String> deadNodes = ringNode.getDeadNodes();
        final Map<String, String> loadMap = ringNode.getLoadMap();

        final Map<Vertex, Integer> statusMap = new HashMap<Vertex, Integer>();

        final UndirectedSparseGraph graph = new UndirectedSparseGraph();
        final StringLabeller stringLabeller = StringLabeller.getLabeller(graph);

        final Vertex[] vertices = new Vertex[rangeMap.size()];

        int count = 0;
        for (Range range : ranges) {
            List<String> endpoints = rangeMap.get(range);
            String primaryEndpoint = endpoints.get(0);
            String load = loadMap.containsKey(primaryEndpoint) ? loadMap.get(primaryEndpoint) : "?";
            String label = "<html>" +
                           "Address: " + primaryEndpoint + "<br/>" +
                           "Load: " + load + "<br/>" +
                           "Range: " + range.right.toString() +
                           "</html>";

            Vertex v = graph.addVertex(new UndirectedSparseVertex());
            vertices[count] = v;
            try {
                stringLabeller.setLabel(v, label);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null, "error: " + e.getMessage());
                e.printStackTrace();
            }

            statusMap.put(v,
                            liveNodes.contains(primaryEndpoint) ? NODE_STATUS_UP
                                    : deadNodes.contains(primaryEndpoint) ? NODE_STATUS_DOWN
                                    : NODE_STATUS_UNKNOWN);

            count++;
        }

        for (int i = 0; i < vertices.length; i++) {
            int index = 0;
            if (i+1 != vertices.length) {
                index = i + 1;
            }
            graph.addEdge(new UndirectedSparseEdge(vertices[i], vertices[index]));
        }

        final Layout layout = new CircleLayout(graph);
        final PluggableRenderer renderer = new PluggableRenderer();

        renderer.setVertexStringer(new VertexStringer() {
            @Override
            public String getLabel(ArchetypeVertex v) {
                return stringLabeller.getLabel(v);
            }
        });

        renderer.setVertexPaintFunction(new VertexPaintFunction() {
            @Override
            public Paint getFillPaint(Vertex v) {
                Color c = Color.YELLOW;
                switch (statusMap.get(v)) {
                case NODE_STATUS_UP:
                    c = Color.GREEN;
                    break;
                case NODE_STATUS_DOWN:
                    c = Color.RED;
                    break;
                }

                return c;
            }
            
            @Override
            public Paint getDrawPaint(Vertex v) {
                Color c = Color.YELLOW;
                switch (statusMap.get(v)) {
                case NODE_STATUS_UP:
                    c = Color.GREEN;
                    break;
                case NODE_STATUS_DOWN:
                    c = Color.RED;
                    break;
                }

                return c;
            }
        });

        final VisualizationViewer viewer = new VisualizationViewer(layout, renderer);

        return viewer;
    }
}
