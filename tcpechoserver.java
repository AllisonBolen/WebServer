import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap; // https://stackoverflow.com/questions/2836267/concurrenthashmap-in-java
import java.util.Vector;

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
                System.out.println("Got from client: " + message);
                if (message.equals("Quit")) {
                    sc.close();
                    System.out.println("Client has disconnected");
                    running = false;
                } else if (message.contains("killuser")) {
                    try {
                        Scanner scanner = new Scanner(message);
                        String command = scanner.next();
                        String name = scanner.next();
                        String password = scanner.next();
                        if (password.equals("football") && command.equals("killuser")) {
                            String m1 = "Admin logged in successfully. Killing user: " + name;
                            ByteBuffer buf = ByteBuffer.wrap(m1.getBytes());
                            sc.write(buf);
                        } else if (!command.equals("killuser")) {
                            String m2 = "Incorrect admin command format: use COMMAND VARIABLE PASSWORD format";
                            ByteBuffer buf = ByteBuffer.wrap(m2.getBytes());
                            sc.write(buf);
                        } else {
                            String m3 = "Incorrect admin password.";
                            ByteBuffer buf = ByteBuffer.wrap(m3.getBytes());
                            sc.write(buf);
                        }
                    } catch (Exception e) {
                        String m4 = "Incorrect admin command format: use COMMAND VARIABLE PASSWORD format";
                        ByteBuffer buf = ByteBuffer.wrap(m4.getBytes());
                        sc.write(buf);
                    }

                } else if (message.contains("List connections")) {
                    String m5 = "Here are the connections";
                    ByteBuffer buf = ByteBuffer.wrap(m5.getBytes());
                    sc.write(buf);
                } else if (message.contains("Broadcast")) {
                    String m6 = "Here are the connections";
                    ByteBuffer buf = ByteBuffer.wrap(m6.getBytes());
                    sc.write(buf);
                } else if (message.contains("SendTo")) {
                    try {
                        Scanner scanner = new Scanner(message);
                        String sendto = scanner.next();
                        String name = scanner.next();
                        String data = scanner.next();
                        while (scanner.hasNext()) {
                            data = data + " " + scanner.next();
                        }
                        //error checking
                        // if name isnt here check that at some point
                        // check proper command trigger statment in index one of input if not send error
                        if (!sendto.equals("SendTo")) {
                            String m7 = "Incorrect 'SendTo' command format. Use: 'SendTo USER MESSAGE'.";
                            ByteBuffer buf = ByteBuffer.wrap(m7.getBytes());
                            sc.write(buf);
                        }
                    } catch (Exception e) {
                        String m8 = "Incorrect 'SendTo...' command format. Use 'SendTo USER MESSAGE'. Your message is blank.";
                        ByteBuffer buf = ByteBuffer.wrap(m8.getBytes());
                        sc.write(buf);
                    }

                } else {
                    buffer.rewind();
                    sc.write(buffer);
                }
            }
        } catch (IOException e) {
            // print error
            System.out.println("Got an IO Exception");
        }

    }
}
