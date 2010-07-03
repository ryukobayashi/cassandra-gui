package org.apache.cassandra.gui.component;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class ShowConfigDlg extends JDialog {
    private static final long serialVersionUID = 4532467169901940874L;

    public ShowConfigDlg(String value){
        final JTextArea text = new JTextArea();
        text.setText(value);

        JPanel showPanel = new JPanel(new GridLayout(1, 1));
        showPanel.add(text);

        JButton ok = new JButton("OK");
        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(ok);

        JScrollPane scrollPane = new JScrollPane(text);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        add(panel);

        pack();
        setModalityType(ModalityType.DOCUMENT_MODAL);
        setTitle("config file");
        setLocationRelativeTo(null);
        setModal(true);
    }
}
