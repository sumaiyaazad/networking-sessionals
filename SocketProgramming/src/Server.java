import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {
    public static ArrayList<Integer> userArray= new ArrayList<>();
    public static ArrayList<Integer> currentUserArray= new ArrayList<>();
    public static int bufferSize = 100;
    public static int currentBufferSize = 0;
    public static int minSize = 100;
    public static int maxSize = 400;
    public static ArrayList<Thread> threadArrayList=new ArrayList<>();
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        ServerSocket serverSocket = new ServerSocket(6666);
        while(true) {
            System.out.println("Waiting for connection...");
            Socket socket = serverSocket.accept();
            System.out.println("Connection established");
            Thread serviceProvider = new ServiceProvider(socket);
            threadArrayList.add(serviceProvider);
            serviceProvider.start();
        }

    }
}
