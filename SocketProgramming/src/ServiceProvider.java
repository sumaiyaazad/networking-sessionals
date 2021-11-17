import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Date;

public class ServiceProvider extends Thread {
    Socket socket;
    ObjectOutputStream out;
    ObjectInputStream in;


    public ServiceProvider(Socket socket) throws IOException {
        this.socket = socket;
        this.out = new ObjectOutputStream(this.socket.getOutputStream());
        this.in = new ObjectInputStream(this.socket.getInputStream());
    }

    public void run()
    {
        try {
//            boolean loginFlag = login();
            out.writeObject("Login ID :");
            System.out.println((String) in.readObject());

//            while (true)
//            {
//                Thread.sleep(1000);
//                Date date = new Date();
//                out.writeObject(date.toString());
//            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

//    private boolean login() throws IOException, ClassNotFoundException {
//
//        return true;
//    }
}
