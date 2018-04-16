import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.Scanner;
import java.util.concurrent.*; // https://stackoverflow.com/questions/2836267/concurrenthashmap-in-java
import java.util.Vector;
import java.util.ArrayList;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.TimeZone;

import java.util.Date;
import java.text.DateFormat;


import java.util.regex.*;


class tcpechoserver {
    public static void main(String args[]) {
        Scanner scan = new Scanner(System.in);
        // map to store all connected client ips and threads
        ConcurrentHashMap<SocketAddress, String> clientMap = new ConcurrentHashMap<SocketAddress, String>();
        ArrayList<String> inputData = new ArrayList<String>();
        String inPort, inDir, inLog;
        inPort = "";
        inDir = ".";
        inLog = "none";
        for (String input : args) {

            if (input.substring(0, input.indexOf("=")).equals("-port")) {// we have been given a port value
                inPort = input.substring(input.indexOf("=") + 1);
            } else {
                inPort = "";
            }
            if (input.substring(0, input.indexOf("=")).equals("-docroot")) {// we have been given a port value
                inDir = input.substring(input.indexOf("=") + 1);
            } else {
                inDir = ".";
            }
            if (input.substring(0, input.indexOf("=")).equals("-logfile")) {// we have been given a port value
                inLog = input.substring(input.indexOf("=") + 1);
            } else {
                inLog = "none";
            }
        }
        inputData = inputCheck(inPort, inDir, inLog);
        System.out.println("You will be using the settings: -port=" + inputData.get(0) + ", -docroot=" + inputData.get(1) + ", logFile=" + inputData.get(2) + ".");

        try {
            // valid input
            int port = Integer.parseInt(inputData.get(0));
            ServerSocketChannel c = ServerSocketChannel.open();
            PrinterThread printer = new PrinterThread(inputData.get(2));
            printer.start();
            c.bind(new InetSocketAddress(port));
            while (true) {
                SocketChannel sc = c.accept();
                System.out.println("Client Connected: " + sc.getRemoteAddress());
                TcpServerThread t = new TcpServerThread(sc, inputData.get(1), printer);
                t.start();
                if (!clientMap.containsKey(sc.getRemoteAddress())) {
                    clientMap.putIfAbsent(sc.getRemoteAddress(), t.getName());
                }
                System.out.println(clientMap.toString());
            }
        } catch (IOException e) {
            System.out.println("Got an Exception 4: " + e);
        }
    }

    public static Boolean directoryExists(String fileName) {
        File myFile = new File(fileName);
        if (myFile.exists() && myFile.isDirectory()) {
            return true;
        }
        return false;
    }

    public static Boolean fileExists(String fileName) {
        File myFile = new File(fileName);
        if (myFile.exists() && !myFile.isDirectory()) {
            return true;
        }
        return false;
    }

    public static ArrayList<String> inputCheck(String p, String dir, String logFile) {
        // checks for user input and
        ArrayList<String> data = new ArrayList<String>();

        // check port value
        if (!p.equals("")) {
            try {
                int port = Integer.parseInt(p);
            } catch (Exception e) {
                System.out.println("Your -port is invalid");
                System.exit(0);
            }
            int port = Integer.parseInt(p);
            if (port < 1000 || port > 65535) {
                System.out.println("Your -port is invalid");
                System.exit(0);
            } else {
                data.add(p);
            }
        } else {
            data.add("8080");
        }


        if (!dir.equals(".")) {
            if (directoryExists(dir)) {
                data.add(dir);
            } else {
                System.out.println("Your -docroot root is invalid");
                System.exit(0);
            }
        } else {
            data.add("./");// default
        }

        if (!logFile.equals("none")) {
            if (fileExists(logFile)) {
                System.out.println("Your -logfile is invalid, a file by that name already exists.");
                System.exit(0);
            } else {
                data.add(logFile);
            }
        } else {
            data.add("CommandLine");
        }

        return data;
    }
}

//______________________________________________________________________
class TcpServerThread extends Thread {
    SocketChannel sc;
    String dir;
    PrinterThread printer;
    private boolean running = true;
    //private ArrayList<String> data = new ArrayList<String>();

    TcpServerThread(SocketChannel channel, String d, PrinterThread p) {
        sc = channel;
        dir = d;
        printer = p;
    }

    public void run() {
        ArrayList<String> data = new ArrayList<String>();
        // main method ?
        try {
            while (running) {
                sc.configureBlocking(false);
                ByteBuffer buffer = ByteBuffer.allocate(4096);
                long endTimeMillis = System.currentTimeMillis() + 20000;
                long startTimeMillis = System.currentTimeMillis();
                while (buffer.position() == 0) {
                    sc.read(buffer);
                    startTimeMillis = startTimeMillis + (System.currentTimeMillis() - startTimeMillis);
                    if (startTimeMillis >= endTimeMillis) {
                        System.out.println("Time out! Closed connection: "+ sc.getRemoteAddress()+ ", " + this.getName());
                        sc.close();
                        setRunning(false);
                        break;
                    }
                }
                if (running) {
                    buffer.flip();
                    byte[] a = new byte[buffer.remaining()];
                    buffer.get(a);
                    String message = new String(a);
                    // call parse
                    data = parseRequest(message);
                    if (data.size() != 0) {
                        printer.addToQueue(message);
                        createResponse(data);
                    } else {
                        // got an empty request
                    }
                }

            }
        } catch (IOException e) {
            // print error
            System.out.println("Got an IO Exception: " + e);
        }

    }

    public ArrayList<String> parseRequest(String a) {
        // parse the data into a list contain all words sent in seperated by space
        Scanner line = new Scanner(a);
        ArrayList<String> requestData = new ArrayList<String>();
        while (line.hasNext()) {
            // System.out.print(line.next() + " \n");
            requestData.add(line.next());
        }
        return requestData;
    }

    public byte[] fileToBytes(File file) {
        try {
            byte[] bytesArray = new byte[(int) file.length()];
            FileInputStream fis = new FileInputStream(file);
            fis.read(bytesArray); //read file into bytes[]
            fis.close();
            return bytesArray;
        } catch (Exception e) {
            System.out.println("Error Loading file");
        }
        return null;
    }

    public void createResponse(ArrayList<String> info) {
        // create the response string
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

        String filename = info.get(1).substring(1, info.get(1).length());
        //boolean modifiedSince = modifiedSince(info, filename);
        Boolean dotdot = Pattern.matches("([\\S|\\s|]/\\.\\.[\\S|\\s|])", filename);
        System.out.println(dir + filename);
        filename = dir + filename;

        if (info.get(0).equals("GET") && info.contains("keep-alive")) {
            if (dotdot) {
                //System.out.println("dotdot");
                String notFoundResponse = "HTTP/1.1 404 NOT FOUND\r\n" +
                        "Date: " + dateFormat.format(calendar.getTime()) + "\r\n" +
                        "Last-Modified: " + dateFormat.format(calendar.getTime()) + "\r\n" +
                        "Content-Length:" + (int) (fileToBytes(new File("notfound.html"))).length + "\r\n" +
                        "Content-Type: text/html\r\n" +
                        //"Connection: Closed\r\n" +
                        "\r\n";
                send(sc, notFoundResponse, "notfound.html");
            } else if (info.get(1).equals("/") || info.get(1).equals("/index.html")) {
                if (modifiedSince(info, "index.html")) {
                    String homeResponse = "HTTP/1.1 200 OK\r\n" +
                            "Date: " + dateFormat.format(calendar.getTime()) + "\r\n" +
                            "Last-Modified: " + getLastModified(dir + "index.html") + "\r\n" +
                            "Content-Length: " + (int) (fileToBytes(new File(dir + "index.html"))).length + "\r\n" +
                            "Content-Type: text/html\r\n" +
                            "Connection: Keep-Alive\r\n" +
                            "Keep-Alive: timeout=20, max=100\r\n" +
                            "\r\n";
                    send(sc, homeResponse, dir + "index.html");
                } else {
                    String homeResponse = "HTTP/1.1 304 NOT MODIFIED\r\n" +
                            "Date: " + dateFormat.format(calendar.getTime()) + "\r\n" +
                            "Last-Modified: " + getLastModified(dir + "index.html") + "\r\n" +
                            "Content-Length: " + (int) (fileToBytes(new File(dir + "index.html"))).length + "\r\n" +
                            "Content-Type: text/html\r\n" +
                            "Connection: Keep-Alive\r\n" +
                            "Keep-Alive: timeout=20, max=100\r\n" +
                            "\r\n";
                    send(sc, homeResponse, dir + "index.html");
                }
            } else if (fileExists(filename) && (info.get(1).contains(".html") || info.get(1).contains(".HTML"))) {
                if (modifiedSince(info, filename)) {
                    String valResponse = "HTTP/1.1 200 OK\r\n" +
                            "Date: " + dateFormat.format(calendar.getTime()) + "\r\n" +
                            "Last-Modified: " + getLastModified(filename) + "\r\n" +
                            "Content-Length: " + (int) (fileToBytes(new File(filename))).length + "\r\n" +
                            "Content-Type: text/html\r\n" +
                            "Connection: Keep-Alive\r\n" +
                            "Keep-Alive: timeout=20, max=100\r\n" +
                            "\r\n";
                    send(sc, valResponse, filename);
                } else {
                    String valResponse = "HTTP/1.1 304 NOT MODIFIED\r\n" +
                            "Date: " + dateFormat.format(calendar.getTime()) + "\r\n" +
                            "Last-Modified: " + getLastModified(filename) + "\r\n" +
                            "Content-Length :" + (int) (fileToBytes(new File(filename))).length + "\r\n" +
                            "Content-Type: text/html\r\n" +
                            "Connection: Keep-Alive\r\n" +
                            "Keep-Alive: timeout=20, max=100\r\n" +
                            "\r\n";
                    send(sc, valResponse, filename);
                }
            } else if (fileExists(filename) && (info.get(1).contains(".txt") || info.get(1).contains(".TXT"))) {
                if (modifiedSince(info, filename)) {
                    String valResponse = "HTTP/1.1 200 OK\r\n" +
                            "Date: " + dateFormat.format(calendar.getTime()) + "\r\n" +
                            "Last-Modified: " + getLastModified(filename) + "\r\n" +
                            "Content-Length: " + (int) (fileToBytes(new File(filename))).length + "\r\n" +
                            "Content-Type: text/plain\r\n" +
                            "Connection: Keep-Alive\r\n" +
                            "Keep-Alive: timeout=20, max=100\r\n" +
                            "\r\n";
                    send(sc, valResponse, filename);
                } else {
                    String valResponse = "HTTP/1.1 304 NOT MODIFIED\r\n" +
                            "Date: " + dateFormat.format(calendar.getTime()) + "\r\n" +
                            "Last-Modified: " + getLastModified(filename) + "\r\n" +
                            "Content-Length: " + (int) (fileToBytes(new File(filename))).length + "\r\n" +
                            "Content-Type: text/plain\r\n" +
                            "Connection: Keep-Alive\r\n" +
                            "Keep-Alive: timeout=20, max=100\r\n" +
                            "\r\n";
                    send(sc, valResponse, filename);
                }
            } else if (fileExists(filename) && (info.get(1).contains(".jpeg") || info.get(1).contains(".JPEG") || info.get(1).contains(".jpg") || info.get(1).contains(".JPG"))) {
                if (modifiedSince(info, filename)) {
                    String valResponse = "HTTP/1.1 200 OK\r\n" +
                            "Date: " + dateFormat.format(calendar.getTime()) + "\r\n" +
                            "Last-Modified: " + getLastModified(filename) + "\r\n" +
                            "Content-Length: " + (int) (fileToBytes(new File(filename))).length + "\r\n" +
                            "Content-Type: image/jpeg\r\n" +
                            "Connection: Keep-Alive\r\n" +
                            "Keep-Alive: timeout=20, max=100\r\n" +
                            "\r\n";
                    send(sc, valResponse, filename);
                } else {
                    String valResponse = "HTTP/1.1 304 NOT MODIFIED\r\n" +
                            "Date: " + dateFormat.format(calendar.getTime()) + "\r\n" +
                            "Last-Modified: " + getLastModified(filename) + "\r\n" +
                            "Content-Length: " + (int) (fileToBytes(new File(filename))).length + "\r\n" +
                            "Content-Type: image/jpeg\r\n" +
                            "Connection: Keep-Alive\r\n" +
                            "Keep-Alive: timeout=20, max=100\r\n" +
                            "\r\n";
                    send(sc, valResponse, filename);
                }
            } else if (fileExists(filename) && (info.get(1).contains(".pdf") || info.get(1).contains(".PDF"))) {
                if (modifiedSince(info, filename)) {
                    String valResponse = "HTTP/1.1 200 OK\r\n" +
                            "Date: " + dateFormat.format(calendar.getTime()) + "\r\n" +
                            "Last-Modified: " + getLastModified(filename) + "\r\n" +
                            "Content-Length: " + (int) (fileToBytes(new File(filename))).length + "\r\n" +
                            "Content-Type: application/pdf\r\n" +
                            "Connection: Keep-Alive\r\n" +
                            "Keep-Alive: timeout=20, max=100\r\n" +
                            "\r\n";
                    send(sc, valResponse, filename);
                } else {
                    String valResponse = "HTTP/1.1 304 NOT MODIFIED\r\n" +
                            "Date: " + dateFormat.format(calendar.getTime()) + "\r\n" +
                            "Last-Modified: " + getLastModified(filename) + "\r\n" +
                            "Content-Length: " + (int) (fileToBytes(new File(filename))).length + "\r\n" +
                            "Content-Type: application/pdf\r\n" +
                            "Connection: Keep-Alive\r\n" +
                            "Keep-Alive: timeout=20, max=100\r\n" +
                            "\r\n";
                    send(sc, valResponse, filename);
                }
            } else {
                String notFoundResponse = "HTTP/1.1 404 NOT FOUND\r\n" +
                        "Date: " + dateFormat.format(calendar.getTime()) + "\r\n" +
                        "Last-Modified: " + getLastModified("notfound.html") + "\r\n" +
                        "Content-Length: " + (int) (fileToBytes(new File("notfound.html"))).length + "\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Connection: Keep-Alive\r\n" +
                        "Keep-Alive: timeout=20, max=100\r\n" +
                        "\r\n";
                send(sc, notFoundResponse, "notfound.html");


            }
        } else if (info.get(0).equals("GET") && info.contains("close")) {

            if (info.get(1).equals("/") || info.get(1).equals("/index.html")) {
                if (modifiedSince(info, dir + "index.html")) {
                    String homeResponse = "HTTP/1.1 200 OK\r\n" +
                            "Date: " + dateFormat.format(calendar.getTime()) + "\r\n" +
                            "Last-Modified: " + getLastModified(dir + "index.html") + "\r\n" +
                            "Content-Length: " + (int) (fileToBytes(new File(dir + "index.html"))).length + "\r\n" +
                            "Content-Type: text/html\r\n" +
                            "Connection: close\r\n" +
                            //"Keep-Alive:timeout=20, max=100\r\n" +
                            "\r\n";
                    send(sc, homeResponse, dir + "index.html");
                } else {
                    String homeResponse = "HTTP/1.1 304 NOT MODIFIED\r\n" +
                            "Date: " + dateFormat.format(calendar.getTime()) + "\r\n" +
                            "Last-Modified: " + getLastModified(dir + "index.html") + "\r\n" +
                            "Content-Length: " + (int) (fileToBytes(new File(dir + "index.html"))).length + "\r\n" +
                            "Content-Type: text/html\r\n" +
                            "Connection: close\r\n" +
                            //"Keep-Alive:timeout=20, max=100\r\n" +
                            "\r\n";
                    send(sc, homeResponse, dir + "index.html");
                }
            } else if (fileExists(filename) && (info.get(1).contains(".html") || info.get(1).contains(".HTML"))) {
                if (modifiedSince(info, filename)) {
                    String valResponse = "HTTP/1.1 200 OK\r\n" +
                            "Date: " + dateFormat.format(calendar.getTime()) + "\r\n" +
                            "Last-Modified: " + getLastModified(filename) + "\r\n" +
                            "Content-Length: " + (int) (fileToBytes(new File(filename))).length + "\r\n" +
                            "Content-Type: text/html\r\n" +
                            "Connection: close\r\n" +
                            //"Keep-Alive:timeout=20, max=100\r\n" +
                            "\r\n";
                    send(sc, valResponse, filename);
                } else {
                    String valResponse = "HTTP/1.1 304 NOT MODIFIED\r\n" +
                            "Date: " + dateFormat.format(calendar.getTime()) + "\r\n" +
                            "Last-Modified: " + getLastModified(filename) + "\r\n" +
                            "Content-Length: " + (int) (fileToBytes(new File(filename))).length + "\r\n" +
                            "Content-Type: text/html\r\n" +
                            "Connection: close\r\n" +
                            //Keep-Alive:timeout=20, max=100\r\n" +
                            "\r\n";
                    send(sc, valResponse, filename);
                }
            } else if (fileExists(filename) && (info.get(1).contains(".txt") || info.get(1).contains(".TXT"))) {
                if (modifiedSince(info, filename)) {
                    String valResponse = "HTTP/1.1 200 OK\r\n" +
                            "Date: " + dateFormat.format(calendar.getTime()) + "\r\n" +
                            "Last-Modified: " + getLastModified(filename) + "\r\n" +
                            "Content-Length: " + (int) (fileToBytes(new File(filename))).length + "\r\n" +
                            "Content-Type: text/plain\r\n" +
                            "Connection: close\r\n" +
                            //"Keep-Alive:timeout=20, max=100\r\n" +
                            "\r\n";
                    send(sc, valResponse, filename);
                } else {
                    String valResponse = "HTTP/1.1 304 NOT MODIFIED\r\n" +
                            "Date: " + dateFormat.format(calendar.getTime()) + "\r\n" +
                            "Last-Modified: " + getLastModified(filename) + "\r\n" +
                            "Content-Length: " + (int) (fileToBytes(new File(filename))).length + "\r\n" +
                            "Content-Type: text/plain\r\n" +
                            "Connection: close\r\n" +
                            //"Keep-Alive:timeout=20, max=100\r\n" +
                            "\r\n";
                    send(sc, valResponse, filename);
                }
            } else if (fileExists(filename) && (info.get(1).contains(".jpeg") || info.get(1).contains(".JPEG") || info.get(1).contains(".jpg") || info.get(1).contains(".JPG"))) {
                if (modifiedSince(info, filename)) {
                    String valResponse = "HTTP/1.1 200 OK\r\n" +
                            "Date: " + dateFormat.format(calendar.getTime()) + "\r\n" +
                            "Last-Modified: " + getLastModified(filename) + "\r\n" +
                            "Content-Length: " + (int) (fileToBytes(new File(filename))).length + "\r\n" +
                            "Content-Type: image/jpeg\r\n" +
                            "Connection: close\r\n" +
                            //"Keep-Alive:timeout=20, max=100\r\n" +
                            "\r\n";
                    send(sc, valResponse, filename);
                } else {
                    String valResponse = "HTTP/1.1 304 NOT MODIFIED\r\n" +
                            "Date: " + dateFormat.format(calendar.getTime()) + "\r\n" +
                            "Last-Modified: " + getLastModified(filename) + "\r\n" +
                            "Content-Length: " + (int) (fileToBytes(new File(filename))).length + "\r\n" +
                            "Content-Type: image/jpeg\r\n" +
                            "Connection: close\r\n" +
                            //"Keep-Alive:timeout=20, max=100\r\n" +
                            "\r\n";
                    send(sc, valResponse, filename);
                }
            } else if (fileExists(filename) && (info.get(1).contains(".pdf") || info.get(1).contains(".PDF"))) {
                if (modifiedSince(info, filename)) {
                    String valResponse = "HTTP/1.1 200 OK\r\n" +
                            "Date: " + dateFormat.format(calendar.getTime()) + "\r\n" +
                            "Last-Modified: " + getLastModified(filename) + "\r\n" +
                            "Content-Length: " + (int) (fileToBytes(new File(filename))).length + "\r\n" +
                            "Content-Type: application/pdf\r\n" +
                            "Connection: close\r\n" +
                            //"Keep-Alive:timeout=20, max=100\r\n" +
                            "\r\n";
                    send(sc, valResponse, filename);
                } else {
                    String valResponse = "HTTP/1.1 304 NOT MODIFIED\r\n" +
                            "Date: " + dateFormat.format(calendar.getTime()) + "\r\n" +
                            "Last-Modified: " + getLastModified(filename) + "\r\n" +
                            "Content-Length: " + (int) (fileToBytes(new File(filename))).length + "\r\n" +
                            "Content-Type: application/pdf\r\n" +
                            "Connection: close\r\n" +
                            //"Keep-Alive:timeout=20, max=100\r\n" +
                            "\r\n";
                    send(sc, valResponse, filename);
                }
            } else {
                String notFoundResponse = "HTTP/1.1 404 NOT FOUND\r\n" +
                        "Date: " + dateFormat.format(calendar.getTime()) + "\r\n" +
                        "Last-Modified: " + getLastModified("notfound.html") + "\r\n" +
                        "Content-Length: " + (int) (fileToBytes(new File("notfound.html"))).length + "\r\n" +
                        "Content-Type: text/html\r\n" +
                        "Connection: close\r\n" +
                        //"Keep-Alive:timeout=20, max=100\r\n" +
                        "\r\n";
                send(sc, notFoundResponse, "notfound.html");
            }
        } else {
            String notSupportedResponse = "HTTP/1.1 501 NOT IMPLEMENTED\r\n" +
                    "Date: " + dateFormat.format(calendar.getTime()) + "\r\n" +
                    "Last-Modified: " + getLastModified("notsupported.html") + "\r\n" +
                    "Content-Length: " + (int) (fileToBytes(new File("notsupported.html"))).length + "\r\n" +
                    "Content-Type: text/html\r\n" +
                    "Connection:Keep-Alive\r\n" +
                    "Keep-Alive:timeout=20, max=100\r\n" +
                    "\r\n";
            send(sc, notSupportedResponse, "notsupported.html");
        }
        if(info.contains("Connection: ")&& info.contains("close")){
            try {
                System.out.println("Closed connection: " + sc.getRemoteAddress() + ", " + this.getName());
                sc.close();
                setRunning(false);
            }catch (Exception e){
                System.out.print("Something broke.");
            }
        }
    }

    public void send(SocketChannel socket, String info, String filename) {
        //tem.out.println("the filename is: " + filename);
        printer.addToQueue(info);
        ByteBuffer buf = ByteBuffer.allocate((int) (info.getBytes().length) + (int) fileToBytes(new File(filename)).length);
        buf.put(info.getBytes());
        buf.put(fileToBytes(new File(filename)));
        buf.flip();
        //ByteBuffer buf = ByteBuffer.wrap(info.getBytes());
        //buf.rewind();

        try {
            socket.write(buf);
        } catch (IOException e) {
            // print error
            System.out.println("Got an IO Exception HERE 5: " + e);
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
            //System.out.println("size of file: " + count);
            return count;
        } else
            System.out.println("The file does not exist!");
        return 0;
    }

    public String getLastModified(String filename) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        File file = new File(filename);
        String fileDate = dateFormat.format(new Date(file.lastModified()));
        return fileDate;
    }

    public boolean modifiedSince(ArrayList<String> info, String filename) {
        boolean modified = false;
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        int index = info.indexOf("If-Modified-Since:");

        if (index == -1)
            modified = true;

        else {

            String request = info.get(index + 1) + " " + info.get(index + 2) +
                    " " + info.get(index + 3) + " " + info.get(index + 4) + " " +
                    info.get(index + 5) + " " + info.get(index + 6);
            String lastMod = getLastModified(filename);

            try {

                if (dateFormat.parse(request).before(dateFormat.parse(lastMod)))
                    modified = true;

            } catch (Exception e) {
                System.out.println("Got an error in the ModifiedSince method");
            }
        }

        return modified;
    }
}

//__________________________________________________________
class PrinterThread extends Thread {
    BlockingQueue<String> queue = new LinkedBlockingQueue<String>();
    String log;
    private boolean running = true;

    PrinterThread(String logFile) {
        //System.out.println("WE Made the printer.");
        log = logFile;
    }

    public void setQueue(BlockingQueue<String> queue) {
        this.queue = queue;
    }

    public void addToQueue(String data) {
        //System.out.println("Add to the queue printer.");
        try {
            queue.put(data);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    public void run() {
        try {
            while (running) {
                String info = queue.poll();
                if (info != null) {
                    //System.out.println("Pull form the printer.");
                    if (log.equals("CommandLine")) {
                        System.out.print(info);
                    } else {
                        fileOutStream(log, info);
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Something broke: " + e);
        }
    }

    public void fileOutStream(String fileName, String str) {
        try (FileWriter fw = new FileWriter(fileName, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(str);
        } catch (IOException e) {
            System.out.println("Something broke: " + e);
        }
    }

}