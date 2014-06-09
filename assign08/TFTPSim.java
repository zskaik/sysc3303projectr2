// TFTPSim.java 
// This class is the beginnings of an error simulator for a simple TFTP server 
// based on UDP/IP. The simulator receives a read or write packet from a client and
// passes it on to the server.  Upon receiving a response, it passes it on to the 
// client.
// One socket (68) is used to receive from the client, and another to send/receive
// from the server.  A new socket is used for each communication back to the client.   
/**
 * @author Ziad Skaik
 * @since 2014-05-30
 * @version 2.0
 */
import java.io.*;
import java.net.*;
import java.util.*;

public class TFTPSim {
   
   // UDP datagram packets and sockets used to send / receive
   private DatagramPacket sendPacket, receivePacket;
   private DatagramSocket  sendReceiveSocket;
   private int sendPort;// the port to send to
   public TFTPSim()
   {
      try {
         // Construct a datagram socket and bind it to port 70
         // on the local host machine. 
         // This socket will be used to send and receive UDP Datagram packets to/from the server.
         sendReceiveSocket = new DatagramSocket(70);
      } catch (SocketException se) {
         se.printStackTrace();
         System.exit(0);
      }
      	 new Thread(){
      		
      		public void run()
      		{
	      		  Scanner scanner = new Scanner(System.in);String input ="";
	      	      System.out.println("Enter Q or q to terminate the Error Simuator");
	      	  	System.out.println("Warning! If Error Simulator is terminated while in TEST MODE, the system will fail inevitably!");
	      	  	
	      	      for(;;)
	      	      {
	      	       input = scanner.next();
	      	      if(input.equalsIgnoreCase("q"))
	      	      {
	      	    	System.out.println("Error Simulator has been terminated.");
	      	    	  System.exit(0);
	      	    	  
	      	      }
	      	      
	      		}
      		}
      	}.start();
        new connectionThread().start();	//spawn a new connection thread to start passing packets 
        
   }
   /**
    * 
    * @author Ziad Skaik
    * @since 2014-05-30
    * @version 2.0
    * This is the connectionThread class to handle the passing of DATA and ACK packets
    * from Client to Server and vice versa for the duration of the file transfer
    */
   class connectionThread extends Thread
   {
	   public connectionThread()
	   {
		   //
	   }
	   public void run()
	   {

		      byte[] data, sending;
		      
		      int clientPort, j=0;

		         data = new byte[516];
		         DatagramPacket receivePacket = new DatagramPacket(data, data.length);

		         System.out.println("Simulator: Waiting for packet.");
		         // Block until a datagram packet is received from receiveSocket.
		         try {
		            sendReceiveSocket.receive(receivePacket);
		         } catch (IOException e) {
		            e.printStackTrace();
		            System.exit(1);
		         }

		         // Process the received datagram.
		         System.out.println("\nSimulator: Request Packet received:");
		         System.out.println("From host: " + receivePacket.getAddress());
		         clientPort = receivePacket.getPort();
		         System.out.println("Host port: " + clientPort);
		         System.out.println("Length: " + receivePacket.getLength());
		         System.out.println("Containing: " );

		         // Get a reference to the data inside the received datagram.
		         data = receivePacket.getData();
		         
		         // print the bytes
		         for (byte b:data) 
		         System.out.print(b+" ");
		         

		         // Form a String from the byte array.
		         String received = new String(data,0,receivePacket.getLength());
		         System.out.println(received);
		         
		         // Now pass it on to the server (to port 69)
		         // Construct a datagram packet that is to be sent to a specified port
		         // on a specified host.
		         // The arguments are:
		         //  msg - the message contained in the packet (the byte array)
		         //  the length we care about - k+1
		         //  InetAddress.getLocalHost() - the Internet address of the
		         //     destination host.
		         //     In this example, we want the destination to be the same as
		         //     the source (i.e., we want to run the client and server on the
		         //     same computer). InetAddress.getLocalHost() returns the Internet
		         //     address of the local host.
		         //  69 - the destination port number on the destination host.

		         sendPacket = new DatagramPacket(data, receivePacket.getLength(),
		                                        receivePacket.getAddress(), 69);
		        
		         System.out.println("\nSimulator: sending packet.");
		         System.out.println("To host: " + sendPacket.getAddress());
		         System.out.println("Destination host port: " + sendPacket.getPort());
		         System.out.println("Length: " + sendPacket.getLength());
		         System.out.println("Containing: ");
		         data = sendPacket.getData();
		         for (byte b:data) 
		         System.out.print(b + " ");
		         

		         // Send the datagram packet to the server via the send/receive socket.

		         try {
		            sendReceiveSocket.send(sendPacket);
		            
		         } catch (IOException e) {
		            e.printStackTrace();
		            System.exit(1);
		         }
		         
		         // The Algorithm for forwarding packets
		      	  System.out.println("port id =" +sendReceiveSocket.getPort());
		         DatagramPacket p = receive();  // receive a packet ( from server )
		         int serverTPort = p.getPort(); // server thread port
		         
		     for(;;)	//loop forever
		     {
		    	 
		    	 //if the packet we received is not from the client
		    	 // then it must be from the server and as such
		    	 // the packet will be forwarded to the client
		    	 if(p.getPort()!=clientPort)
		    	 send(p,clientPort);
		    	//otherwise if the packet we received is from the client
		    	// then we will send the packet to the server and use our server thread port variable
		    	 else
		    	 {
		    		 send(p,serverTPort);
		    	 }
		    	 p= receive(); // received a packet ( from either client or server ) 
		    	 
		     }// end for loop
	   }
	   
   }
   /**
    * Receives packet
    * @return the DatagramPacket received
    */
   private DatagramPacket receive()
   {

       byte[] data = new byte[516];
       receivePacket = new DatagramPacket(data, data.length);

       System.out.println("\nSimulator: Waiting for packet.");
       try {
          // Block until a datagram is received via sendReceiveSocket.
    	  System.out.println("port id =" +sendReceiveSocket.getLocalPort());
          sendReceiveSocket.receive(receivePacket);
       } catch(IOException e) {
          e.printStackTrace();
          System.exit(1);
       }

       // Process the received datagram.
       System.out.println("Simulator: Packet received:");
       System.out.println("From host: " + receivePacket.getAddress());
       System.out.println("Host port: " + receivePacket.getPort());
       System.out.println("Length: " + receivePacket.getLength());
       System.out.println("Containing: ");

       // Get a reference to the data inside the received datagram.
       data = receivePacket.getData();
       for (byte b:data) 
          System.out.print(b+ " ");
       
       return receivePacket;
	   
   }
   /**
    * Sends the packet to the specified port
    * @param p the DatagramPacket that is passed 
    * @param sendPort the port that is being sent to
    */
   private void send(DatagramPacket p,int sendPort)
   {
	   	byte[] data = p.getData(); //senddata = new byte[p.getLength()];
	   	
	 /*  	for(int j=0;j<senddata.length;j++)
	   	senddata[j]=data[j];*/
	   	
       sendPacket = new DatagramPacket(data,data.length,
                             p.getAddress(), sendPort);

       System.out.println( "\nSimulator: Sending packet:");
       System.out.println("To host: " + sendPacket.getAddress());
       System.out.println("Destination host port: " + sendPacket.getPort());
       System.out.println("Length: " + sendPacket.getLength());
       System.out.println("Containing: ");
       
       for (byte b:data) 
        System.out.print(b + " ");
       

       // Send the datagram packet to the client via a new socket.


       try {
          sendReceiveSocket.send(sendPacket);
       } catch (IOException e) {
          e.printStackTrace();
          System.exit(1);
       }

       System.out.println("\nSimulator: packet sent using port: " + sendReceiveSocket.getLocalPort());
       System.out.println();

   }

   /**
    * The main thread of execution for the Simulator
    * @param args
    */
   public static void main( String args[] )
   {
      TFTPSim s = new TFTPSim();
      
   
   }
}

