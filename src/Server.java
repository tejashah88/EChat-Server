import java.awt.BorderLayout;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;

/**
 * Class that manages connections to clients
 * @author Tejas Shah
 * @version 11/7/14
 */
public class Server {
	public ArrayList<ClientObject> clients;
	private static final String DIVIDER = "~";
	EChatServerGUI sg;
	
	ServerSocket serverSocket;
	//String addr;
	int port;
	int UID = 0;
	boolean isRunning = true;
	
	SimpleDateFormat sdf;
	
	JFrame tempProgress;
	JProgressBar progressBar;
	int barValue = 0;
	
	/**
	 * Constructor for TCP Server
	 * @param addr
	 * @param port the port number to host
	 * @param sg an instance of EChatServerGUI
	 */
	public Server(/*String addr,*/ int port, EChatServerGUI sg) {
		//this.addr = addr;
		this.port = port;
		this.sg = sg;
		this.sdf = new SimpleDateFormat("MM/dd/yy HH:mm:ss");
		this.clients = new ArrayList<ClientObject>();
		
		this.tempProgress = new JFrame("");
        this.tempProgress.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.tempProgress.setSize(300, 100);
        this.tempProgress.setVisible(false);
	}
	
	/**
	 * Starts the server
	 */
	public void start() {
		try {
			this.serverSocket = new ServerSocket(port);//, 10, InetAddress.getByName("www.echat.duckdns.org").getHostAddress());
		} catch (IOException e) {
			this.error("Unable to start up chat server!");
			this.stop();
		}
		
		while (this.isRunning) {
			try {
				ClientObject tempClient = new ClientObject(serverSocket.accept());  //wait for a client to connect to this server
				this.clients.add(tempClient); //if has gotten a connection, save it!!!
				tempClient.start();
			} catch (IOException e) {
				this.display("Lost connection with unknown client!");
				this.isRunning = false;
			}
		}
		
		this.stop();
	}
	
	/**
	 * Disconnects all clients from the server and cleans up any remaining stuff
	 * 
	 */
	public void stop() {
		this.isRunning = false;
		for (ClientObject client : clients) {
			client.shutdown();  //disconnect all clients from the server
		}
		
		try {
			if(this.serverSocket != null)
				this.serverSocket.close();  //close the socket to prevent any incoming connections to the server
		} catch (IOException e) {}
		
		System.exit(0);
	}
	
	/**
	 * Preps the progress bar for use during a long operation.
	 * @param title the title of the progress bar
	 */
	public void initBar(String title) {
		sg.setEnabled(false);
		this.tempProgress.setTitle(title);
		
		this.progressBar = new JProgressBar();
		this.progressBar.setValue(0);
	    this.progressBar.setStringPainted(true);
	    this.progressBar.setBorder(BorderFactory.createTitledBorder(title));
	    
	    this.tempProgress.add(this.progressBar, BorderLayout.NORTH);
	    this.tempProgress.setVisible(true);
	}
	
	/**
	 * Increments the progress bar by the specified value.
	 * @param n the value to add to the progress
	 */
	public void addBarValue(int n) {
		this.barValue += n;
		
		if (this.barValue >= 100) {
			this.barValue = 100;
		}
		
		this.progressBar.setValue(this.barValue);
	}
	
	/**
	 * Decrements the progress bar by the specified value.
	 * @param n the value to subtract from the progress
	 */
	public void subBarValue(int n) {
		this.barValue -= n;
		
		if (this.barValue <= 0) {
			this.barValue = 0;
		}
		
		this.progressBar.setValue(this.barValue);
	}
	
	/**
	 * Deallocates resources from the progress bar.
	 */
	public void deInitBar() {
		if (this.progressBar != null) {
			this.tempProgress.remove(this.progressBar);
		}
		
		this.progressBar = null;
		this.tempProgress.setVisible(false);
		
		sg.setEnabled(true);
	}
	
	/**
	 * Prints an object to the console.
	 * @param o whatever you want to print onto the GUI's console
	 */
	public void display(Object o) {
		sg.console.append(this.getDate() + ": " + o + "\n");
		sg.console.setCaretPosition(sg.console.getText().length());
	}
	
	/**
	 * Shows an error as a message dialog
	 * @param o whatever you want to show as a cause of the error
	 */
	public void error(Object o) {
		JOptionPane.showMessageDialog(null, o);
	}
	
	/**
	 * Notifies every user in the room except for the user specified. Useful for notifying others about logout events.
	 * @param o the msg to send
	 * @param user the user to not notify
	 */
	public void notifyOthers(Object o, String user) {
		this.display(o);
		for (int x = 0; x < this.clients.size(); x++) {
			if (!this.clients.get(x).equals(user)) {
				this.clients.get(x).sendToClient(new Message(MessageType.TEXT, o.toString()));
			}
		}
	}
	
	public void broadcast(Object o) {
		this.display(o);
		for (int x = 0; x < this.clients.size(); x++) {
			this.clients.get(x).sendToClient(new Message(MessageType.TEXT, o.toString()));
		}
	}
	
	/**
	 * Returns the current date and time when the method was called
	 * @return current date and time
	 */
	public String getDate() {
		return this.sdf.format(new Date());
	}
	
	/**
	 * This class represents clients connected to this server
	 * @author Tejas Shah
	 * @version 11/7/14
	 */
	public class ClientObject extends Thread {
		Socket socket;
		ObjectInputStream inFromClient;
		ObjectOutputStream outToClient;
		
		String user, pass;
		int id;
		boolean isRunning = true;
		
		/**
		 * Constructor for the ClientObject class.
		 * @param socket
		 */
		public ClientObject(Socket socket) {
			this.socket = socket;
			this.id = UID;
			UID++;
			
			try {
				this.outToClient = new ObjectOutputStream(this.socket.getOutputStream());
				this.inFromClient = new ObjectInputStream(this.socket.getInputStream());
				
				Message loginMsg = (Message) inFromClient.readObject();
				if (loginMsg.getMsgType().equals(MessageType.LOGIN)) {
					String text = loginMsg.getText();
					String[] loginStuff = text.split(DIVIDER);
					
					if (loginStuff.length == 2) {
						this.user = loginStuff[0];
						this.pass = loginStuff[1];
					} else {
						this.shutdown();
					}
				} else {
					this.shutdown();
				}
			} catch (IOException | ClassNotFoundException e) {
				Server.this.display("Unable to communicate with client!");
				this.shutdown();
			}
			
			Server.this.broadcast(this.user + " has joined the server!");
		}
		
		/**
		 * The main loop of the ClientObject
		 */
		@Override
		public void run() {
			while(this.isRunning) {
				try {
					Message response = (Message) inFromClient.readObject();
					
					switch (response.getMsgType()) {
						case LOGOUT:
							Server.this.notifyOthers(user + " has disconnected from server!", user);
							this.isRunning = false;
							break;
							
						case TEXT:
							Server.this.broadcast(this.user + ": " + response.getText());
							break;
						
						case COMMAND:
							switch (response.getText().substring(1))
							{
								case "who":
									/*String[] names = new String[clients.size()];
									int index = 0;
									for (ClientObject c : clients) {
										names[index] = c.user;
									}
									this.sendToClient(new Message(MessageType.TEXT, Arrays.toString(names)));*/
							}
							break;
							
						default:
							break;
					}
				} catch (ClassNotFoundException | IOException e) {
					Server.this.notifyOthers(user + " has disconnected from server!", user);
					this.isRunning = false;
				}
			}
			
			this.shutdown();
		}
		
		/**
		 * Closes the connection to the client that the ClientObject was holding
		 */
		public void shutdown() {
			try {
				if (this.outToClient != null)
					this.outToClient.close();
			} catch (IOException e) {}
			
			try {
				if (this.inFromClient != null)
					this.inFromClient.close();
			} catch (IOException e) {}
			
			try {
				if(this.socket != null)
					this.socket.close();
			} catch (IOException e) {}
		}
		
		/**
		 * Sends a text to a client. It will try for 20 times for giving up.
		 * @param text Whatever to send to the client.
		 */
		public void sendToClient(Message msg) {
			int tries = 0;
			while (tries <= 20) {
				try {
					this.outToClient.writeObject(msg);
					return;
				} catch (IOException e) {
					tries++;
				}
			}
			
			this.shutdown();
		}
	}
}