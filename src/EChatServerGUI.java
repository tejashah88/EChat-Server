import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;

/**
 * This class is used for actually running the EChat client
 * 
 * @author Tejas Shah
 * @version 11/10/14
 */
public class EChatServerGUI extends JFrame implements ActionListener, WindowListener {
	private static final long serialVersionUID = 4905485210886599452L;
	
	//declare d shit
	
	static easyGUI gui = new easyGUI();
	JTextArea console;
	JTextField txtCmd;
	JButton btnSendText;
	JScrollPane consoleScroll;
	static JPanel panel;
	GridBagConstraints gbc;
	Insets insets;
	
	static String text;
	//private boolean hasRecievedText = false;
	//private static boolean isReadyToSend = false;
	
	static Server server;
	
	public EChatServerGUI() {
		//declare gui components
		
		console = new JTextArea(20, 40);
		console.setEditable(false);
		console.setMinimumSize(new Dimension(200,100));
		//console.setRows(console.getRows());
		
		txtCmd = new JTextField(40);
		
		btnSendText = new JButton("Send to all on server");
		
		consoleScroll = new JScrollPane(console, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
										ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		
		console.setLineWrap(true);
		
		gui.registerActionEvents(this, btnSendText);
		gui.registerActionEvents(this, txtCmd);
		//panel = gui.addToPanel(consoleScroll, btnSendText, txtCmd);
		panel = new JPanel(new GridBagLayout()); 
		gbc = new GridBagConstraints();
		
		/* config for grid bag constants */
		{
			gbc.fill = GridBagConstraints.BOTH;
			gbc.weightx = 0.5;
			gbc.gridx = 0;
			gbc.gridy = 0;
			gbc.gridwidth = 2;
			gbc.anchor = GridBagConstraints.FIRST_LINE_START;
			panel.add(consoleScroll, gbc);
			
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 0.5;
			gbc.gridx = 0;
			gbc.gridy = 1;
			gbc.gridwidth = 1;
			panel.add(txtCmd, gbc);
			
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.weightx = 0.5;
			gbc.gridx = 1;
			gbc.gridy = 1;
			gbc.gridwidth = 1;
			panel.add(btnSendText, gbc);
		}
		
		this.add(panel);
	}

	public static void main(String args[]) throws IOException {
		//initialize d server
		
		EChatServerGUI sg = new EChatServerGUI();
		gui.initGUI(sg, "EChat Server", 650, 395);
		sg.setMinimumSize(new Dimension(650, 395));
		//gui.enableBetterGUI();
		
		server = new Server(8888, sg);
		server.start();
		
		server.stop();
	}
	
	//detect when d comand is fired

	@Override
	public void actionPerformed(ActionEvent event) {
		if (event.getSource().equals(btnSendText) || event.getSource().equals(txtCmd)) {
			text = txtCmd.getText();
			if (text.startsWith("/")) {
				switch (text.substring(1)) {
					case "shutdown":
					case "stop":
						server.stop();
						break;
				}
			} else {
				switch(text) {
					case "":
						break;
					default:
						server.display("SERVER: " + text);
						for (int x = 0; x < server.clients.size(); x++) {
							server.clients.get(x).sendToClient(new Message(MessageType.TEXT, "SERVER: " + text));
						}
						break;
					}
			}
			
			txtCmd.setText("");
		}
	}
	
	@Override
	public void windowClosing(WindowEvent arg0) {
		server.stop();
	}
	
	@Override
	public void windowActivated(WindowEvent arg0) {}
	@Override
	public void windowClosed(WindowEvent arg0) {}
	@Override
	public void windowDeactivated(WindowEvent arg0) {}
	@Override
	public void windowDeiconified(WindowEvent arg0) {}
	@Override
	public void windowIconified(WindowEvent arg0) {}
	@Override
	public void windowOpened(WindowEvent arg0) {}
}