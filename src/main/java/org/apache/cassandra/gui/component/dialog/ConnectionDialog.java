package org.apache.cassandra.gui.component.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.cassandra.client.Client;

public class ConnectionDialog extends JDialog {
	
    private static final long serialVersionUID = 8707158056959280058L;

    private class EnterAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            enterAction();
        }
    }

    private JButton ok = new JButton("OK");
    private JTextField hostText = new JTextField();
    private JTextField thriftPortText = new JTextField();
    private JTextField jmxPortText = new JTextField();
    private boolean wasCancelled = false;

    public ConnectionDialog(JFrame owner) {
        super(owner);

        hostText.setText("localhost");
        thriftPortText.setText(String.valueOf(Client.DEFAULT_THRIFT_PORT));
        jmxPortText.setText(String.valueOf(Client.DEFAULT_JMX_PORT));

        hostText.addActionListener(new EnterAction());
        thriftPortText.addActionListener(new EnterAction());
        jmxPortText.addActionListener(new EnterAction());

        JPanel inputPanel = new JPanel(new GridLayout(3, 2));
        inputPanel.add(new JLabel("host:"));
        inputPanel.add(hostText);
        inputPanel.add(new JLabel("thrift port:"));
        inputPanel.add(thriftPortText);
        inputPanel.add(new JLabel("jmx port:"));
        inputPanel.add(jmxPortText);

        ok.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enterAction();
            }
        });
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
            	wasCancelled = true;
                setVisible(false);
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(ok);
        buttonPanel.add(cancel);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(new JLabel("Connection settings"), BorderLayout.NORTH);
        panel.add(inputPanel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        add(panel);

        pack();
        setModalityType(ModalityType.DOCUMENT_MODAL);
        setTitle("Connect");
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void enterAction() {
        if (hostText.getText().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Enter host name.");
            return;
        }
        setVisible(false);
    }

    public String getHost() {
    	return hostText.getText();
    }
    
    public int getThriftPort() {
    	return Integer.valueOf(thriftPortText.getText());
    }

    public int getJmxPort() {
    	return Integer.valueOf(jmxPortText.getText());
    }

	public boolean wasCancelled() {
		return wasCancelled;
	}

}
