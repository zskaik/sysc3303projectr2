// TFTPServer.java 
// This class is the server side of a simple TFTP server based on
// UDP/IP. The server receives a read or write packet from a client and
// sends back the appropriate response without any actual file transfer.
// One socket (69) is used to receive (it stays open) and another for each response. 

import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.filechooser.FileSystemView;

public class TFTPServer {

   // types of requests we can receive
   public static enum Request { READ, WRITE, ERROR};
   // responses for valid requests
   public static final byte[] readResp = {0, 3, 0, 1};
   public static final byte[] writeResp = {0, 4, 0, 0};
   public static final byte[] invalidResp = {0, 5};
  private int packetdata_length,errorno;
   // UDP datagram packets and sockets used to send / receive
   private DatagramPacket sendPacket, receivePacket;
   private DatagramSocket receiveSocket, sendSocket;
   
   public TFTPServer()
   {
      try {
         // Construct a datagram socket and bind it to port 69
         // on the local host machine. This socket will be used to
         // receive UDP Datagram packets.
         receiveSocket = new DatagramSocket(69);
      } catch (SocketException se) {
         se.printStackTrace();
         System.exit(1);
      }
      
 	 new Thread(){
   		
   		public void run()
   		{
	      		  Scanner scanner = new Scanner(System.in);String input ="";
	      	      System.out.println("Enter Q or q to terminate the Server.");
	     
	      	      for(;;)
	      	      {
	      	       input = scanner.next();
	      	      if(input.equalsIgnoreCase("q"))
	      	      {
	      	    	  System.out.println("Server has been terminated.");
	      	    	  System.exit(0);
	      	      }
	      	      
	      		}
   		}
   	}.start();
   }

   public void receiveTFTPRequest()
   {

      byte[] data,
             response = new byte[4],
             sending;
      
      Request req; // READ, WRITE or ERROR
      String filename, mode;
      int len, j=0, k=0;

      for(;;) { // loop forever
         // Construct a DatagramPacket for receiving packets up
         // to 100 bytes long (the length of the byte array).
          
         data = new byte[100];
         receivePacket = new DatagramPacket(data, data.length);

         System.out.println("Server: Waiting for request packet.");
         // Block until a datagram packet is received from receiveSocket.
         try {
            receiveSocket.receive(receivePacket);
         } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
         }

         // Process the received datagram.
         System.out.println("Server: Request Packet received:");
         System.out.println("From host: " + receivePacket.getAddress());
         System.out.println("Host port: " + receivePacket.getPort());
         System.out.println("Length: " + receivePacket.getLength());
         System.out.println("Containing:  " );

         // Get a reference to the data inside the received datagram.
         data = receivePacket.getData();
         
         // print the bytes
         for (byte b:data) {
            System.out.print(b + " ");
         }
         
         // Form a String from the byte array.
         String received = new String(data,0,receivePacket.getLength());
         System.out.println(received);

         // If it's a read, send back DATA (03) block 1
         // If it's a write, send back ACK (04) block 0
         // Otherwise, ignore it
         if (data[0]!=0) req = Request.ERROR; // bad
         else if (data[1]==1) req = Request.READ; // could be read
         else if (data[1]==2) req = Request.WRITE; // could be write
         else req = Request.ERROR; // bad
         
         len = receivePacket.getLength();

        /* if (req!=Request.ERROR) { // check for filename
             // search for next all 0 byte
             for(j=2;j<len;j++) {
                 if (data[j] == 0) break;
            }
            if (j==len) req=Request.ERROR; // didn't find a 0 byte
            // otherwise, extract filename
            filename = new String(data,2,j-2);
         }/*
 
        /* if(req!=Request.ERROR) { // check for mode
             // search for next all 0 byte
             for(k=j+1;k<len;k++) { 
                 if (data[k] == 0) break;
            }
            if (k==len) req=Request.ERROR; // didn't find a 0 byte
            mode = new String(data,j,k-j-1);
         }*/
         
        // if(k!=len-1) req=Request.ERROR; // other stuff at end of packet        
         
         // Create a response.
       
         new clientConnectionThread(receivePacket,req,receivePacket.getPort()).start();
         
         
         
      } 

   }
   
   /**
    * 
    * @author Ziad Skaik
    * @since 2014-05-30
    * @version 2.0
    *
    *This is the clientConnectionThread class which will be spawned by the Server listener as soon as 
    * a request packet is receieved. 
    */
   class clientConnectionThread extends Thread
   {
	   private DatagramSocket sendAndReceiveSocket; // the socket that we will use for File I/O
	   private Request request; // the type of request we have ( read or write )
	   private String s; 	// the String to hold the file name that is to be read/written from/to
	   private int sendPort; // the port we will send to ( Error Simulator OR Client depending on mode of operation)
	   /**
	    * 
	    * @param rp The DatagramPacket containing our request
	    * @param r The request type
	    * This is the constructor for our clientConnectionThread.
	    * It will create a new DatagramSocket for Sending and Receiving DATA/ACK 
	    * packets during the file transfer.
	    * Additionally, will initialize the request type as a field for access in 
	    * our subsequent run method and will initialize the String to containing our file name to
	    * be READ/WRITTEN to as another field for access when the thread is running.
	    */
	   public clientConnectionThread(DatagramPacket rp,Request r,int sendPort)
	   {
			   try {
				this.sendAndReceiveSocket = new DatagramSocket();
			} catch (SocketException e) {
				e.printStackTrace();
			}
			   
			   this.request = r;
			   this.sendPort=sendPort;
			   
			   if(this.sendPort!=70)
				sendServerThreadPort();
			   
			   
			   // get the data of our request packet
			   byte[] data = rp.getData();
			   String packetdata = "";
	  			try {
	  				s = "";
	  				packetdata = new String(data, "UTF-8");
	  			} catch (UnsupportedEncodingException e1) {
	  				e1.printStackTrace();
	  			}
	  	        //get the file name and store in String s
	  			//System.out.print(packetdata.substring(2, packetdata.length()));
	  			//System.exit(0);
	  			int length = packetdata.length();
	  	       s= packetdata.substring(2,length);
	  	       
	  	       System.out.println(s);
			     	        	
	   }
	   /**
	    * The run method for our clientConnectionThread.
	    * This thread will be responsible for handling the requests
	    * and doing the File I/O required
	    */
	   public void run()
	   {
		   // thread has begun
  	        	
  	        	
  	        	//if we are dealing with a read request
  	        		if (request==Request.READ) 
  	        		{
	  	        		 	InputStream reader = null;
	  	        			File file = new File("server_files\\" +s);
	  	        				
	  	        		
	  	        				
	  	        			if (!file.canRead()) {
	  	        				System.out.println("TRY AGAIN ");
	  	        				errorno =2;
	  	        				senderror(errorno);
	  	        				System.exit(1);
	  	        			}
	  	        			
	  	        			try {
	  	        				reader = new FileInputStream(file);
	  	        			} catch (FileNotFoundException e1) {
	  	  					
	  	        				errorno = 1;
	  	        				senderror (errorno);
	  	        				System.exit(1);
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
								if( (n=reader.read(data))==-1)
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
								// the size is 512 to reflect a 4 byte opcode + 512 bytes of file data = 516 bytes

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
							System.err.println("Failed to complete read request.");
							Thread.currentThread().interrupt();System.err.println("Thread was interrupted due to failed read");
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
	  	  	        	this.sendAndReceiveSocket.close();
  	        		}
  	        	//if we are dealing with a  write request
  	        		else if (request==Request.WRITE)
  	        		{
  	        			sendAck((byte)0); 
  	        			ArrayList<Byte> finalData = new ArrayList<Byte>();
  	        			 OutputStream os= null;
  	        			 byte[] data,data_noopcode;
  	        			 
  	        			 File file = new File("server_files\\"+s );
  	        			 
  	        		
  	        			 if(file.isFile()){
  	        				 errorno =6;
  	        				 senderror(errorno);
  	        				 System.exit(1);
  	        			  }
  	        			 
  	        			 
  	        			 
  	        			 
  	        			 
  	        			 
  	        			 try {
  	        				 os = new FileOutputStream(file);
  	        			} catch (FileNotFoundException e) {
  	        				e.printStackTrace();
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
  	        		    	 
  	        		    	/* int a =0;
  	        		    	 System.out.println("data_noopcode contains: ");
  	        		    	 for(byte b:data_noopcode){
  	        		    		 System.out.print(b+" ");
  	        		    		 a++;
  	        		    	 }*/
  	        		    	 
  	        		    	 for(byte b:data_noopcode)
  	        		    		 finalData.add(b);

  	        		    	 sendAck(data[3]++);
  	        		    	System.out.println("THE PACKETDATA_LENGTH VALUE IS" + packetdata_length);
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
  	        			 
  	        			 try {
  	        					os.write(finalData_barr,0, finalData_barr.length);
  	        			 } catch (IOException e) {
  	        				e.printStackTrace();
  	        				System.err.println("Failed to write to the OutputStream");
  	        			 }
  	        		     //
  	        		     System.out.println("Done. File write request has been completed");
  	        		     try {
  	        				os.close();
  	        			} catch (IOException e) {
  	        				e.printStackTrace();
  	        				System.err.println("ERROR! Failed to close the OutputStream.");
  	        			}
  	        		     sendAndReceiveSocket.close(); //Close the Socket used for the file transfer since we are done.
  	        			
  	        		} 
	
	   }//end run method
   
	   /**
	    * If in NORMAL mode, we will send the port of this thread to the Client so we can let it know
	    * who to communicate with.
	    */
	private void sendServerThreadPort()
	{
		byte[] data = {(byte) this.sendAndReceiveSocket.getPort()};
		DatagramPacket sendPacket = null;
		try {
			sendPacket = new DatagramPacket(data,data.length,InetAddress.getLocalHost(),sendPort);
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
		
		try {
			sendAndReceiveSocket.send(sendPacket);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
   /**
    * 
    * @param d the Byte array that contains our packet data 
    * (i.e file contents) that are being transferred
    * Sends a packet of file contents to the Error Simulator/Client
    * depending on mode of operation
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
			sendAndReceiveSocket.send(sendPacket);
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
   byte[] data  = new byte[516];
	   
	   DatagramPacket receivePacket = new DatagramPacket(data,data.length);
	   
	   try {
		System.out.println("RECEIVEADATAPACKET THREAD IS LISTENING ON:" + sendAndReceiveSocket.getPort());
		sendAndReceiveSocket.receive(receivePacket);
		packetdata_length= receivePacket.getLength();
	} catch (IOException e) {
		e.printStackTrace();
		System.err.println("ERROR! Failed to receive data packet.");
	}
	   
	   data = receivePacket.getData();
	
		System.out.println("Received a Data packet with block #:  " + data[3] );
		System.out.println("Containing: ");
		for(byte b:data)
		System.out.print(b+ " ");
		
		   byte[] trimarr = new byte[packetdata_length];
		     
			for(int j=0;j<packetdata_length;j++)
			{
				trimarr[j]=data[j];
			}
		  
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
			sendAndReceiveSocket.send(sendPacket);
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
		sendAndReceiveSocket.receive(receivePacket);
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
	    	   x = "ACCESS VIOLATION error";
	                break;
	       case 3:  
	    	   error [3] =3;
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
   
   
  /* private boolean enoughSpace(int fileSize)
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
	   
   	}	//end enoughSpace function*/
   
   
   }// end clientConnectionThread class

   /**
    * The main thread of execution for the Server side
    * This thread will create and initialize a new TFTPServer Object
    * and invoke the receiveTFTPRequest() ( i.e the listener) 
    * which will listen for file transfer requests
    * @param args
    */
   public static void main( String args[] )
   {
      TFTPServer c = new TFTPServer();
      c.receiveTFTPRequest();
   }
}

