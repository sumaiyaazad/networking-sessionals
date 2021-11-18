import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Date;

public class ServiceProvider extends Thread {
    Socket socket;
    ObjectOutputStream out;
    ObjectInputStream in;


    public ServiceProvider(Socket clientSocket) throws IOException {
        socket = clientSocket;
        out = new ObjectOutputStream(this.socket.getOutputStream());
        in = new ObjectInputStream(this.socket.getInputStream());
    }

    public void run() {
        try {
            boolean loginFlag = login();
            while (loginFlag) {
                System.out.println((String) in.readObject());
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private boolean login() throws IOException, ClassNotFoundException {

        //reads user id and checks whether a current user is trying to establish a connection
        //if inactive user tries to establish a connection, then add that user to currentUserArray
        //if anew user tries to establish a connection add that user to currentUserArray and userArray

        out.writeObject("Login ID :");
        Integer userId = Integer.parseInt((String) in.readObject());
        if (Server.currentUserArray.contains(userId)) {
            out.writeObject("Login denied, "+userId + " active");
            socket.close();
            return false;
        } else if (Server.userArray.contains(userId)) {
            out.writeObject("Welcome again "+userId);
            Server.currentUserArray.add(userId);
            return true;
        } else {
            out.writeObject("Welcome "+userId);
            Server.currentUserArray.add(userId);
            Server.userArray.add(userId);
            return true;
        }

    }
}
