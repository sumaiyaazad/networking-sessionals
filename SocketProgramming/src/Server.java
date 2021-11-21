import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {
    public static ArrayList<Integer> userArray= new ArrayList<>();
    public static ArrayList<Integer> currentUserArray= new ArrayList<>();
    public static int bufferSize = 1000;
    public static int currentBufferSize = 0;
    public static int minSize = 100;
    public static int maxSize = 400;
    public static ArrayList<BrowsingThread> threadArrayList=new ArrayList<>();
    private static Socket socket;
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        ServerSocket serverSocket = new ServerSocket(6666);
        while(true) {
            System.out.println("Waiting for connection...");
            socket = serverSocket.accept();
            System.out.println("Connection established");
            BrowsingThread browsing = new BrowsingThread(socket);
            threadArrayList.add(browsing);
            browsing.start();
        }

    }

}
