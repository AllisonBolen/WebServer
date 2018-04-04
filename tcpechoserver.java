import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap; // https://stackoverflow.com/questions/2836267/concurrenthashmap-in-java
import java.util.Vector;
import java.util.ArrayList;

class tcpechoserver {
    public static void main(String args[]) {
        Scanner scan = new Scanner(System.in);
        // list to store relevent code for the socket
        //Vector<long> clientInfo = new Vector<long>();
        // map to store all connected client ips and threads
        ConcurrentHashMap<SocketAddress, String> clientMap = new ConcurrentHashMap<SocketAddress, String>();
        try {
            System.out.println("Enter a port for the server to run on: ");
            int port = scan.nextInt();
            ServerSocketChannel c = ServerSocketChannel.open();
            c.bind(new InetSocketAddress(port));
            int count = 0;
            while (true) {
                SocketChannel sc = c.accept();
                System.out.println("Client Connected: " + sc.getRemoteAddress());
                TcpServerThread t = new TcpServerThread(sc);
                t.start();
                if (!clientMap.containsKey(sc.getRemoteAddress())) {
                    clientMap.putIfAbsent(sc.getRemoteAddress(), t.getName());
                }
                System.out.println(clientMap.toString());
                count++;
            }
        } catch (IOException e) {
            System.out.println("Got an Exception");
        }
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
                // call parse
                data = parseRequest(message);
                // do stuff depending on data
//                System.out.println("Got from client: " + message);
                //send(sc, "testing");
            }
        } catch (IOException e) {
            // print error
            System.out.println("Got an IO Exception");
        }

    }

    public ArrayList<String> parseRequest(String a) {
        // parse the dat into a list contain all words sent in seperated by space
        Scanner line = new Scanner(a);
        ArrayList<String> data = new ArrayList<String>();
        System.out.print("this is the data we were sent:\n");

        while (line.hasNext()) {
//            Scanner space = new Scanner(line.next()).useDelimiter("\\s");
//            while (space.hasNext()) {
//                data.add(space.next());
            System.out.print(line.next()+ " \n" );
//            }
        }
        return null;
    }

    public byte[] createResponse(ArrayList<String> info) {
        // create the response string
        return null;

    }

    public void send(SocketChannel socket, String info) {

        ByteBuffer buf = ByteBuffer.wrap(info.getBytes());
        buf.rewind();

        try {
            socket.write(buf);
        } catch (IOException e) {
            // print error
            System.out.println("Got an IO Exception HERE 5");
        }
    }

}
