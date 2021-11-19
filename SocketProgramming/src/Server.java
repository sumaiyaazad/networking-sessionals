import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Server extends Thread{
    public static ArrayList<Integer> userArray= new ArrayList<>();
    public static ArrayList<Integer> currentUserArray= new ArrayList<>();
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        ServerSocket welcomeSocket = new ServerSocket(6666);
        while(true) {
            System.out.println("Waiting for connection...");
            Socket socket = welcomeSocket.accept();
            System.out.println("Connection established");
            Thread serviceProvider = new ServiceProvider(socket);
            serviceProvider.start();
        }

    }
}
