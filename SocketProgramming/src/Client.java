import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        Socket socket = new Socket("localhost", 6666);
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        Scanner s = new Scanner(System.in);
        System.out.println("Connection established");
        System.out.println("Remote port: " + socket.getPort());
        System.out.println("Local port: " + socket.getLocalPort());
        System.out.println("Remote IP: " + socket.getInetAddress());
        System.out.println("Local IP: " + socket.getLocalAddress());
        
        while(true) {
            String serverMessage = (String) in.readObject();
            System.out.println(serverMessage);
            if(serverMessage.contains("denied") || serverMessage.contains("logout")){
                break;
            }
            String userCommand = s.nextLine();
            out.writeObject(userCommand);
        }
    }


}
