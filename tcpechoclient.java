import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.Scanner;

class tcpechoclient {
    public static void main(String args[]) {
        Scanner scan = new Scanner(System.in);
        try {
            System.out.println("What is the server's IP address?");
            String IP = scan.nextLine();
            System.out.println("What is the server's port?");
            int portNum = scan.nextInt();
            SocketChannel sc = SocketChannel.open();
            sc.connect(new InetSocketAddress(IP, portNum));
            System.out.println("Connected to Chat Server");
            TcpClientThread t = new TcpClientThread(sc);
            t.start();
            while (true) {
                Console cons = System.console();
                String m = cons.readLine("Enter your message: ");
                if (m.equals("Quit")) {
                    ByteBuffer buf = ByteBuffer.wrap(m.getBytes());
                    sc.write(buf);
                    sc.close();
                    System.exit(0);
                } else {
                    ByteBuffer buf = ByteBuffer.wrap(m.getBytes());
                    sc.write(buf);
                }
            }
        } catch (IOException e) {
            System.out.println("Got an Exception");
        }
    }
}

class TcpClientThread extends Thread {
    SocketChannel sc;

    TcpClientThread(SocketChannel channel) {
        sc = channel;
    }

    public void run() {
        // main method ?
        try {
            while (true) {
                ByteBuffer buf2 = ByteBuffer.allocate(5000);
                sc.read(buf2);
                buf2.flip();
                byte[] a = new byte[buf2.remaining()];
                buf2.get(a);
                String message = new String(a);
                System.out.println("\nGot from server: " + message);
                //sc.close();
            }
        } catch (IOException e) {
            // print error
            System.out.println("Got an IO Exception");
        }

    }
}
