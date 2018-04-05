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
        // map to store all connected client ips and threads
        ConcurrentHashMap<SocketAddress, String> clientMap = new ConcurrentHashMap<SocketAddress, String>();
        try {
            if (args.length == 1) {
                // not logs file
                inputCheck(args[0], ".", "none");
            } else if (args.length == 2) {
                // we have a dirroot to look from
                inputCheck(args[0], args[1], "none");
            } else if (args.length == 3) {
                // given a log file to write to
                inputCheck(args[0], args[1], args[2]);
            } else {
                // invlaid user input
                System.out.println("You have the wrong number of agruments. Use: 'java <program-name> <port-num> <path-to-root-dir> <logfile>'.");
            }

            // valid input
            int port = Integer.parseInt(args[0]);
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
                // call parse
                data = parseRequest(message);
                // do stuff depending on data
                send(sc, "testing");
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
        System.out.print("this is the data we were sent:\n");

        while (line.hasNext()) {
            System.out.print(line.next() + " \n");
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
