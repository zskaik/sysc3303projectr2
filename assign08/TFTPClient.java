// TFTPClient.java 
// This class is the client side for a very simple assignment based on TFTP on
// UDP/IP. The client uses one port and sends a read or write request and gets 
// the appropriate response from the server.  No actual file transfer takes place.   

import java.io.*;
import java.net.*;
import java.util.*;
//import javax.swing.filechooser.FileSystemView;
/**
 * 
 * @author Ziad Skaik
 * @since 2014-05-30
 * @version 2.0
 */
public class TFTPClient
{

   private DatagramPacket sendPacket, receivePacket;
   private DatagramSocket  ftSocket; // sendSocket-> Sends requests to Error Simulator, ftSocket-> Handles file transfer
   private static String s;
   private static String fname;
   private int errorno,sendPort, packetdata_length=0;

   // we can run in normal (send directly to server) or test
   // (send to simulator) mode
   public static enum Mode { NORMAL, TEST};

   /**
    * This is the constructor for the TFTPClient
    * It is responsible for initializing our two Sockets:
    * sendSocket, which is used to send requests to the Server,
    * and ftSocket which is used in the duration of the file transfer to
    * send/receive Acknowledge packets, and send/receive Data packets.
    */
   public TFTPClient()
   {
	
      try {
         // Construct a datagram socket and bind it to any available
         // port on the local host machine. This socket will be used to
         // send and receive UDP Datagram packets.
      //   sendSocket = new DatagramSocket();
         // Create this socket ( File Transfer Socket) to be for use in  file transfer.
         ftSocket = new DatagramSocket();
      } catch (SocketException se) {   // Can't create the socket.
         se.printStackTrace();
         System.exit(1);
      }
   }
   /**
    * This method will send the read/write request to the Server(Port 69).
    * Additionally, the read() or write() methods will be called according to the 
    * request type made which will run until the file transfer has been completed. 
    */
   public void sendRequest()
   {
      byte[] msg = new byte[100], // message we send
             fn, // filename as an array of bytes
             md, // mode as an array of bytes
             data; // reply as array of bytes
      String mode; // filename and mode as Strings
      int j, len;
      Mode run = Mode.NORMAL; // change to NORMAL to send directly to server
      
      if (run==Mode.NORMAL) 
         sendPort = 69;	  // If this is selected, all communication will be with the Error Simulator.
      else
         sendPort = 70;   // If this is selected, all communication will be with the Error Simulator.
      
         System.out.println("Client: creating packet request.");
         
         // Set the first byte of the packet data to 0 as per opcode format requirements
        msg[0] = 0;
        // If the String that the user entered matches the letter "R" or "r" indicating a read request
        // we will set the second byte of the packet data to 1 as per opcode format requirements
        if(s.equalsIgnoreCase("R")) 
           msg[1]=1;
        // If the String that user entered matches the letter "W" or "w" indicating a write request
        // we will set the second byte of the packet data to 1 as per opcode format requirements
        else if(s.equalsIgnoreCase("W")) 
           msg[1]=2;
       
        // convert the user-entered String representing the file name that is
        // to be read/written from/to to bytes
        fn = fname.getBytes();
        
        // and copy into the msg
        System.arraycopy(fn,0,msg,2,fn.length);
        
        // now add a 0 byte
   //     msg[fn.length+2] = 0;

       /* // now add "octet" (or "netascii")
        mode = "octet";
        // convert to bytes
        md = mode.getBytes();
        
        // and copy into the msg
        System.arraycopy(md,0,msg,fn.length+3,md.length);*/
        
        len = fn.length+2; // length of the message

        // and end with another 0 byte 
      //  msg[len-1] = 0;
        
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
        try {
           sendPacket = new DatagramPacket(msg, len,
                                         InetAddress.getLocalHost(), sendPort);
        } catch (UnknownHostException e) {
           e.printStackTrace();
           System.exit(1);
        }

        System.out.println("Client: sending request packet ");
        if(run==Mode.NORMAL)
        System.out.println("To Server: " + sendPacket.getAddress());
        
        else
        {
        System.out.println("To Error Simulator: " + sendPacket.getAddress());	
        }
        System.out.println("Destination  port: " + sendPacket.getPort());
        System.out.println("Length: " + sendPacket.getLength());
        System.out.println("Containing: \n  ");
        data = sendPacket.getData();
        for (j=0;j<len;j++) 
        System.out.print(data[j]+" ");
        
        // Send the DatagramPacket containing the file transfer request to the server via the send/receive socket.
        try {
           ftSocket.send(sendPacket);
        } catch (IOException e) {
           e.printStackTrace();
           System.exit(1);
        }

        System.out.println("Client: Packet sent.");
        
        if(run==Mode.NORMAL)
        	getServerThreadPort();
       
        // We have sent the file transfer request to the Server, so we close the Socket.
   //    sendSocket.close(); 
        //If the String which the user entered matches the letter "R" or "r" indicating a read request
        // we will invoke the method read(sendPort) which will initiate the reading process on the client end.
        if(s.equalsIgnoreCase("R"))
        	read();

        //If the String which the user entered matches the letter "W" or "w" indicating a write request
        // we will invoke the method write(sendPort) which will initiate the writing process on the client end.
        else if(s.equalsIgnoreCase("W"))
        	write();
   }
   /**
    * If we are in NORMAL mode we need to be able to acquire the port of the Thread we are communicating with
    */
   private void getServerThreadPort()
   {
	   byte[] data = new byte[10];
	
	  DatagramPacket receivePacket = new DatagramPacket(data,data.length);
	  try {
		ftSocket.receive(receivePacket);
	} catch (IOException e) {
		e.printStackTrace();
	}
	 
	  sendPort = receivePacket.getPort();
	  
   }
   /**
    * This method will be responsible for reading the file from the Server
    */
   private void read()
   {
	 ArrayList<Byte> finalData = new ArrayList<Byte>();
	 OutputStream os= null;
	 byte[] data,data_noopcode;
	 
	 File file = new File("client_files\\" +fname);
	 
	 
	 try {
		 os = new FileOutputStream(file);
	} catch (FileNotFoundException e) {
		//e.printStackTrace();
		//errorno = 1;
		//senderror (errorno);
		System.err.println("ERROR! Failed to initialize OutputStream Object");
	}
	 int x=0;
	 for(;;)
     { 	 
    	 // receive the data packet with the opcode included and store in byte array "data"
    	 // initialize the data_noopcode array to the same array as data, discarding the opcode
    	 data = receiveDataPacket();
    	 
    	/* System.out.println("\nPRINITING OUT OPCODE\n");
    	 for (int i = 0 ; i < 4 ; i++)
    		 System.out.println(data[i]+ " ");*/
    	 
    	 data_noopcode = Arrays.copyOfRange(data, 4, packetdata_length);
    	 
    	 /*int a =0;
    	 System.out.println("data_noopcode contains: ");
    	 for(byte b:data_noopcode){
    		 System.out.print(b+" ");
    		 a++;
    	 }*/
    	 
    	 for(byte b:data_noopcode)
    		 finalData.add(b);

    	 sendAck(data[3]++);
    	System.out.println("THE PACKETDATA_LENGTH VALUE IS: " + packetdata_length);
    	 if(packetdata_length<512)
    	 {
     		  break;
    	 }
     }	// end of for loop
    
	 byte[] finalData_barr = new byte[finalData.size()];
	 int z=0;
	 	
	 for(byte b:finalData){
		 finalData_barr[z]=b;
		 z++;}
	 
	 
	 /*if(!enoughSpace(finalData_barr.length)){
		 System.out.println("DISK FULL ERROR");
		 System.exit(0);
	 }*/
	 
	 try {
			os.write(finalData_barr,0, finalData_barr.length);
	 } catch (IOException e) {
		e.printStackTrace();
		System.err.println("Failed to write to the OutputStream");
	 }
     //
     System.out.println("Done. File was successfully read");
     try {
		os.close();
	} catch (IOException e) {
		e.printStackTrace();
		System.err.println("ERROR! Failed to close the OutputStream.");
	}
     ftSocket.close(); //Close the Socket used for the file transfer since we are done.
     
   }
   /**
    * This method will be responsible for handling the writing of the file to the Server
    * Upon receiving an initial ACK0, the file to be transferred will be opened, 512 bytes will be read into a byte array 
    * and inserted into a DatagramPacket to be sent to the Error Simulator which will send 
    * the packet to the Server.
    * This process will repeat until the file is fully transferred. 
    * When the transfer is complete, the ftSocket will be closed.
    */
   
private void write()
   {
		receiveAck();
		InputStream reader = null;
			File file = new File("client_files\\" +fname);
 	  	    
			if (!file.canRead()) {
				
				errorno =2;
				senderror(errorno);
				
			}
    	try {
			reader = new FileInputStream(file);
		} catch (FileNotFoundException e1) {
			
			//e1.printStackTrace();
		}
    
    	//Create a new ArrayList of Bytes to store our data
    	//Create n integer to indicate how many bytes we have read from the file.
    	//Create and initialize dataop ( Data packet opcode array) to size 4 
    	//Create and initialize byte array data to a size of 512 bytes.
    Byte[] arr = null;int n,i=0;byte[] dataop = new byte[4];
    	// We will set the four bytes to the DATA1 opcode before we 
    	// read the file contents into the byte array.
    	dataop[0]=0;dataop[1]=3;dataop[2]=0;dataop[3]=1;
    
    	try {
    		// Loop while the number of bytes we have read from the file into the data byte array
    		// is not -1 ( i.e we successfully read bytes from the file ( file has content)). 
		for(;;)
		{ 
			
			ArrayList<Byte> bl= new ArrayList<Byte>();
			 byte[] data = new byte[512];

			 if((n=reader.read(data))==-1)
				 break;
			// add the opcode to the ArrayList
			for(byte b:dataop){
			bl.add(b);
			i++;}
			
			//add the rest of the file contents (data array) to the the ArrayList
			for(int k=0;k<n;k++){
			bl.add(data[k]);
			i++;}
			// this is an array of Bytes not bytes as
			// as the toArray method does not work with just bytes
			

			arr = bl.toArray(new Byte[i]);
			/*System.out.println("Printing the data before sending, the size of the Byte array is: " + arr.length + "Bytes");
			for(Byte B:arr)
				System.out.print(" " + B);*/
			//Now send the contents to the Error Simulator/Client depending on mode of operation
			sendDataPacket(arr);
			
			//receive an ACK packet
			  receiveAck();
			//Increment the block number
			dataop[3]++;
			i=0;
		}//end while loop
	} catch (IOException e) {
		
		e.printStackTrace();
		System.err.println("Failed to complete write request.");
		Thread.currentThread().interrupt();System.err.println("Thread was interrupted due to failed write");
	}
    	
    	if(arr.length==516){
    		Byte[] zeropacket = {0,3,0,dataop[3]++,0};
    		sendDataPacket(zeropacket);
    		receiveAck();
    	}
    	
    		
    	try {
		reader.close();
	} catch (IOException e) {
		e.printStackTrace();
		System.err.println("Failed to close the input stream.");
	}
    	System.out.println("File has been successully sent.");
    	ftSocket.close();
   }
   /**
    * 
    * @param d the Byte array that contains our packet data 
    * (i.e file contents) that are being transferred
    * Sends a packet of file contents to the Error Simulator/Server depending on 
    * mode of operation
    * 
    */
   private void sendDataPacket(Byte[] d)
   {
	   byte[] data = new byte[d.length];
	   int i;
	   // convert the array of Bytes to bytes so we can send the packet 
	   for(i=0;i<d.length;i++)
		   data[i]= d[i].byteValue();
	   DatagramPacket sendPacket=null;
	   try {
	
	// create a packet to send to the specified port ( Client /Error Simulator depending on mode of operation)
		 sendPacket = new DatagramPacket(data, data.length, InetAddress.getLocalHost(), this.sendPort);
	} catch (UnknownHostException e) {
		e.printStackTrace();
	}
	// print the packet data contents to be sent.
	   System.out.println("\nSending data packet to port : "+ this.sendPort+ " Containing: ");
	   for(byte b:data)
	   System.out.print(b + " ");
	   // send the packet to the specified port
	   try {
		   System.out.println("port id =" +sendPort);
		   System.out.println("data length =" +data.length);
		   System.out.println(new String(data));
		ftSocket.send(sendPacket);
	} catch (IOException e) {
		e.printStackTrace();
		System.err.println("Packet failed to send to port: " + this.sendPort); // failed to send the packet
	}
	   System.out.println("\nPacket sent to port: " + this.sendPort); // packet was successfully sent to the destination port
   }
   /**
    * 
    * @return the data byte array containing the data of the DatagramPacket we receive.
    * This packet will contain file data that is being transferred to 
    * to the Server.
    */
   
   
   private byte[] receiveDataPacket()
   {
	byte[] data= new byte[516]; 
	ArrayList<Byte> testData = new ArrayList<Byte>();
	
	
	
	System.out.println("Waiting for data packet");
	DatagramPacket receivePacket = null;
	try {
		receivePacket = new DatagramPacket(data,data.length,InetAddress.getLocalHost(),sendPort);
		
	} catch (UnknownHostException e) {
		e.printStackTrace();
		System.err.println("ERROR! Failed to create and intialize DatagramPacket for receiving next data packet");
		
	}
	// Receive the data packet.
	// This method call will block until a packet matching the receivePacket object is received on the ftSocket
	try {
		ftSocket.receive(receivePacket);
		packetdata_length= receivePacket.getLength();
		
	} catch (IOException e) {
		e.printStackTrace();
		System.err.println("ERROR! Failed to receive data packet.");
	}
	data = receivePacket.getData();
	
	 byte[] trimarr = new byte[packetdata_length];
     
		for(int j=0;j<packetdata_length;j++)
		{
			trimarr[j]=data[j];
		}
	  
  // System.out.println("THE PACKETDATA_LENGTH VALUE IS" + packetdata_length);

	
	// set the data of the receivePacket equal to the data byte array initialized previously
	
	// print a message to indicate that a data packet has been received, along with the corresponding block #
	System.out.println("Received Data packet with block #: " + data[3] );
	System.out.println("Containing: ");
	for(int h = 0;h<packetdata_length;h++)
		System.out.print(trimarr[h]+ " ");
	System.out.print("\n");
	
	
	
	///////////////////////
	 String received = new String(trimarr,0,receivePacket.getLength());
     System.out.println(received);
	
    
     return trimarr;
   }
   /**
    * 
    * @param ackCount the ACK # ( or block #) 
    * that will be inserted into the ACK packet that is to be sent.
    */
   private void sendAck(byte ackCount)
   {	
	  byte[] data = {0,4,0,ackCount};
	  DatagramPacket sendPacket = null;
	  try {
		 sendPacket = new DatagramPacket(data,data.length,InetAddress.getLocalHost(),sendPort);
	} catch (UnknownHostException e) {
		e.printStackTrace();
		System.err.println("ERROR! DatagramPacket failed to be created and initialized");
	}
	  //print what is to be sent
	  System.out.println("Sending ACK with block #: " + ackCount);
	  try {
		ftSocket.send(sendPacket);
	} catch (IOException e) {
		e.printStackTrace();
		System.err.println("ERROR! Datagram ACK packet failed to be sent");
	}
	  
	  System.out.println("ACK successfully sent");
   }
   /**
    * 
    * @return the data byte array containing the data of the DatagramPacket we receive.
    * Receives the ACK with corresponding ACK # ( or block #)
    */
   private byte[] receiveAck()
   {
	   byte[] data  = new byte[4];
	   DatagramPacket receivePacket=null;
	   try {
		 receivePacket = new DatagramPacket(data,data.length,InetAddress.getLocalHost(),sendPort);
	} catch (UnknownHostException e) {
		e.printStackTrace();
		System.err.println("ERROR! Failed to create and initialize DatagramPacket to receive ACKs");
	}
	   try {
		ftSocket.receive(receivePacket);
	} catch (IOException e) {
		e.printStackTrace();
		System.err.println("ERROR! Failed to receive ACK packet");
	}
	   
	   data = receivePacket.getData();
	// Print the ACK packet received.
		System.out.println("Received an ACK with block #: " + data[3] + "Containing\n: " );
		for(byte b:data)
		System.out.print(b+ " ");
		
	   return data;
   }
   
   /*private boolean enoughSpace(int fileSize)
   {
	   
	   FileSystemView fsv = FileSystemView.getFileSystemView();
       
       File[] drives = File.listRoots();
       if (drives != null && drives.length > 0) 
       {
    	   long totalSpace = 0;
           long freeSpace = 0;
           
    	   for (File aDrive : drives) {
               if(fsv.getSystemTypeDescription(aDrive).equals("Removable Disk")){
                   System.out.println("Drive Letter: " + aDrive);
                   System.out.println("\tType: " + fsv.getSystemTypeDescription(aDrive));
                   System.out.println("\tTotal space: " + aDrive.getTotalSpace());
                   System.out.println("\tFree space: " + aDrive.getFreeSpace());
                   System.out.println();
               
                   totalSpace = aDrive.getTotalSpace();
                   freeSpace = aDrive.getFreeSpace();
               }
           }
       
           if(freeSpace<fileSize)
        	   return false;
       
       }
	   
	   return true;
	   
   }*/
   
   private void senderror (int i ) {
	   
		
	   
		DatagramSocket sendErr=null;
		
		
		
		
		try {
			sendErr = new DatagramSocket();
		} catch (SocketException e1) {
			e1.printStackTrace();
		}
		   byte[] error = new byte[100], // message we send
		             erstring; // error string as bytes
		           error[0]= 0; 
		        	error[1]=5; 
		         	error [2]=0;
		       	String x = null;
		   
		       	DatagramPacket errPacket = null;
				try {
					errPacket = new DatagramPacket(error, error.length, InetAddress.getLocalHost(), sendPort);
				} catch (UnknownHostException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
		       	
		       	switch (i) {
	       case 0:  
	    	   error [3] =0;
	                break;
	       case 1:  
	    	   error [3] =1;
	    	   
	    	    x = "FILE NOT FOUND error";
	    	      		
	                break;
	       case 2:  
	    	   error [3] =2;
	    	   x = "ACCESS VIOLATION error ";
	                break;
	       case 3:  
	    	   error [3] =3;
	    	   x= "DISK FULL ERROR ";
	                break;
	       case 4:  
	    	   error [3] =4;
	                break;
	       case 5:  
	    	   error [3] =5;
	                break;
	       case 6:  
	    	   error [3] =6;
	                x = "FILE ALREADY EXISTS.";
	    	   
	    	   break;
	     
		default: 
	                break;   
		   }
		   
		   erstring = x.getBytes();
		   
		   System.arraycopy(erstring,0,error,3,erstring.length);
	       
	       // now add a 0 byte
	       error[erstring.length+4] = 0;
	       
	       try {
	           errPacket = new DatagramPacket(error, error.length,
	                                         InetAddress.getLocalHost(), sendPort);
	        } catch (UnknownHostException e) {
	           e.printStackTrace();
	           System.exit(1);
	        }
	       
	       System.out.println("Sending error packet with contents  #: " + erstring);
	       System.out.println("Sending error packet with contents  #: " + new String (errPacket.getData()));
	 	  try {
	 		 sendErr.send(errPacket);
	 	} catch (IOException e) {
	 		e.printStackTrace();
	 		System.err.println("ERROR! Datagram  packet failed to be sent");
	 	}
	 	  
	 	  System.out.println("Error successfully sent");
	    }
   /**
    * The main thread of execution for this program
    * This thread will create and initialize a new TFTPClient Object,
    * create a Scanner Object to read user input
    * which will ask the user to enter either "r"/"R" or "w"/"W" 
    * to indicate a read or write request(continues prompt until user correctly enters request String)
    * The user is also prompted to enter the file name that is being read/written from/to.
    * If the user has selected a write request, and has incorrectly entered a file name that 
    * does not exist on the file system, the user will be prompted to re-enter the file name.
    * 
    * Once the user input has been handled, the TFTPClient Object will invoke the sendRequest() method
    * to send a file read/write request to the Server/Error Simulator depending on the mode of operation.
    * @param args
    */
   public static void main(String args[])
   {
	   TFTPClient c = new TFTPClient();  
	   Scanner scanner = new Scanner(System.in);
	  
	  do
	  {
	   System.out.println("Read or Write? (R/W) ");
	   s = scanner.next();
		   
	  }while(!s.equalsIgnoreCase("R")&&!s.equalsIgnoreCase("W"));
	  
	  File file; boolean filexists = true ;
	   do
	   {
		   System.out.println("Enter file name");
		   fname = scanner.next();
		  
		   if(s.equalsIgnoreCase("W"))
		   {
		    	file = new File("client_files\\" +fname);
		    	filexists = file.isFile();
		    	//file.setReadOnly();
		    	//file.setWritable(true);
		    	// If the file name entered by the user doesn't exist
		    	if(!filexists)
		    	{
		    		System.err.println("File does not exist, please re-enter the file name");
		    	}
		   }
	   }while(!filexists);  // loop while the file name entered doesn't exist
	   // invoke the sendRequest method
      c.sendRequest();
   }//end main
}//end class

