package org.apache.cassandra.gui.control;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class CellPropertiesDlg extends JDialog {
    private static final long serialVersionUID = -7378362468372008181L;

    public CellPropertiesDlg(String name, String value){
        final JTextField nameText = new JTextField(name);
        final JTextField valueText = new JTextField(value);

        nameText.setEditable(false);
        valueText.setEditable(false);

        JPanel propertiesPane = new JPanel(new GridLayout(2, 2));
        propertiesPane.add(new JLabel("name:"));
        propertiesPane.add(nameText);
        propertiesPane.add(new JLabel("value:"));
        propertiesPane.add(valueText);

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
        panel.add(propertiesPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        add(panel);

        pack();
        setModalityType(ModalityType.DOCUMENT_MODAL);
        setTitle("Properties");
        setLocationRelativeTo(null);
        setModal(true);
    }
}
