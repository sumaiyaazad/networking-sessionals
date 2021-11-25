import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class Server {
    public static ArrayList<Integer> userArray= new ArrayList<>();
    public static ArrayList<Integer> currentUserArray= new ArrayList<>();
    public static int bufferSize = 65500;
    public static int currentBufferSize = 0;
    public static int minSize = 10000;
    public static int maxSize = 60000;
    public static ArrayList<ServiceProvider> threadArrayList=new ArrayList<>();
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        ServerSocket serverSocket = new ServerSocket(6666);
        while(true) {
            System.out.println("Waiting for connection...");
            Socket socket = serverSocket.accept();
            System.out.println("Connection established");
            ServiceProvider serviceProvider = new ServiceProvider(socket);
            threadArrayList.add(serviceProvider);
            serviceProvider.start();
        }

    }
}
