import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap; // https://stackoverflow.com/questions/2836267/concurrenthashmap-in-java
import java.util.Vector;
import java.util.ArrayList;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;


class tcpechoserver {
    public static void main(String args[]) {
        Scanner scan = new Scanner(System.in);
        // map to store all connected client ips and threads
        ConcurrentHashMap<SocketAddress, String> clientMap = new ConcurrentHashMap<SocketAddress, String>();
        try {
            if (args.length == 1) {
                // not logs file
                inputCheck(args[0], ".", "none");
                System.out.println("You are connected on port: " + args[0] + ", Searching from the directory the server runs from, and logs are printed to the server.");
            } else if (args.length == 2) {
                // we have a dirroot to look from
                inputCheck(args[0], args[1], "none");
                System.out.println("You are connected on port: " + args[0] + ", Searching from directory: " + args[1] + ", and logs are printed to the server.");
            } else if (args.length == 3) {
                // given a log file to write to
                inputCheck(args[0], args[1], args[2]);
                System.out.println("You are connected on port: " + args[0] + ", Searching from directory: " + args[1] + ", and logging to the file: " + args[2] + ".");
            } else {
                // invlaid user input
                System.out.println("You have the wrong number of agruments. Use: 'java <program-name> <port-num> <path-to-root-dir> <logfile>'.");
                System.exit(0);
            }

            // valid input
            int port = Integer.parseInt(args[0]);
            ServerSocketChannel c = ServerSocketChannel.open();
            c.bind(new InetSocketAddress(port));
            while (true) {
                SocketChannel sc = c.accept();
                System.out.println("Client Connected: " + sc.getRemoteAddress());
                TcpServerThread t = new TcpServerThread(sc);
                t.start();
                if (!clientMap.containsKey(sc.getRemoteAddress())) {
                    clientMap.putIfAbsent(sc.getRemoteAddress(), t.getName());
                }
                System.out.println(clientMap.toString());
            }
        } catch (IOException e) {
            System.out.println("Got an Exception");
        }
    }

    public static ArrayList<Boolean> inputCheck(String port, String dir, String logfile) {
        // checks for user input and
        return null;
    }
}



















class TcpServerThread extends Thread {
    SocketChannel sc;
    private boolean running = true;
    private ArrayList<String> data;

    TcpServerThread(SocketChannel channel) {
        sc = channel;
    }

    public void run() {

        // main method ?
        try {
            while (running) {
                ByteBuffer buffer = ByteBuffer.allocate(4096);
                sc.read(buffer);
                buffer.flip();
                byte[] a = new byte[buffer.remaining()];
                buffer.get(a);
                String message = new String(a);
                //System.out.println(message);
                // call parse
                data = parseRequest(message);
                System.out.println(data);
                createResponse(data);
            }
        } catch (IOException e) {
            // print error
            System.out.println("Got an IO Exception");
        }

    }

    public ArrayList<String> parseRequest(String a) {
        // parse the data into a list contain all words sent in seperated by space
        Scanner line = new Scanner(a);
        ArrayList<String> data = new ArrayList<String>();

        while (line.hasNext()) {
            // System.out.print(line.next() + " \n");
            data.add( line.next());
        }
        return data;
    }

    public byte[] fileToBytes(File file){
        try{
            byte[] bytesArray = new byte[(int) file.length()];
            FileInputStream fis = new FileInputStream(file);
            fis.read(bytesArray); //read file into bytes[]
            fis.close();
            return bytesArray;
        }
        catch(Exception e){
            System.out.println("Error Loading file");
        }
        return null;
    }

    public byte[] createResponse(ArrayList<String> info) {
        // create the response string
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

	String filename = info.get(1).substring(1, info.get(1).length());
	//System.out.println("FILENAME REQUESTED: " + filename);

	if(info.get(0).equals("GET")){

		if(info.get(1).equals("/") || info.get(1).equals("/index.html")){
			String homeResponse = "HTTP/1.1 200 OK\r\n" +
				"Date: " + dateFormat.format(calendar.getTime()) + "\r\n" +
				"Last-Modified: Thu, 05 Apr 2018 19:15:56 GMT\r\n" +
				"Content-Length:" + (int)(fileToBytes(new File("index.html"))).length + "\r\n" +
				"Content-Type: text/html\r\n" +
				//"Connection: Closed\r\n" +
				"\r\n";
			send(sc, homeResponse, "index.html");
		}
		else if(fileExists(filename)){
			String valResponse = "HTTP/1.1 200 OK\r\n" +
				"Date: " + dateFormat.format(calendar.getTime()) + "\r\n" +
				"Last-Modified: Thu, 05 Apr 2018 19:15:56 GMT\r\n" +
				"Content-Length:" + (int)(fileToBytes(new File(filename))).length + "\r\n" +
				"Content-Type: text/html\r\n" +
				//"Connection: Closed\r\n" +
				"\r\n";
			send(sc, valResponse, filename);

		}
		else{
			String notFoundResponse = "HTTP/1.1 404 NOT FOUND\r\n" +
				"Date: " + dateFormat.format(calendar.getTime()) + "\r\n" +
				"Last-Modified: " + dateFormat.format(calendar.getTime()) + "\r\n" +
				"Content-Length:" + (int)(fileToBytes(new File("notfound.html"))).length + "\r\n" +
				"Content-Type: text/html\r\n" +
				//"Connection: Closed\r\n" +
				"\r\n";
			send(sc, notFoundResponse, "notfound.html");		
		

		}
	}

	else{
		String notSupportedResponse = "HTTP/1.1 501 NOT IMPLEMENTED\r\n" +
				"Date: " + dateFormat.format(calendar.getTime()) + "\r\n" +
				"Last-Modified: Thu, 05 Apr 2018 19:15:56 GMT\r\n" +
				"Content-Length:" + (int)(fileToBytes(new File("notsupported.html"))).length + "\r\n" +
				"Content-Type: text/html\r\n" +
				//"Connection: Closed\r\n" +
				"\r\n";
			send(sc, notSupportedResponse, "notsupported.html");
	}
        return null;

    }

    public void send(SocketChannel socket, String info, String filename) {
        ByteBuffer buf = ByteBuffer.allocate((int)(info.getBytes().length) + (int)fileToBytes(new File(filename)).length);
        buf.put(info.getBytes());
        buf.put(fileToBytes(new File(filename)));
        buf.flip();
        //ByteBuffer buf = ByteBuffer.wrap(info.getBytes());
        //buf.rewind();

        try {
            socket.write(buf);



        } catch (IOException e) {
            // print error
            System.out.println("Got an IO Exception HERE 5");
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public Boolean fileExists(String fileName) {
        File myFile = new File(fileName);
        if (myFile.exists() && !myFile.isDirectory()) {
            return true;
        }
        return false;
    }

    public double fileSize(String fileName) {
        File myFile = new File(fileName);
        if (myFile.exists() && !myFile.isDirectory()) {
            //System.out.println("Size of file: " + fileSize);
            double count = myFile.length();
            System.out.println("size of file: " + count);
            return count;
        } else
            System.out.println("The file does not exist!");
        return 0;
    }
}
