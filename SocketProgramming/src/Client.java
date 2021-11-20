import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static Socket socket;
    private static ObjectOutputStream out;
    private static ObjectInputStream in;

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        socket = new Socket("localhost", 6666);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        Scanner s = new Scanner(System.in);
        System.out.println("Connection established");
        System.out.println("Remote port: " + socket.getPort());
        System.out.println("Local port: " + socket.getLocalPort());
        System.out.println("Remote IP: " + socket.getInetAddress());
        System.out.println("Local IP: " + socket.getLocalAddress());
        
        while(true) {
            String serverMessage = (String) in.readObject();
            System.out.println("server message : "+serverMessage);
            if(serverMessage.contains("denied") || serverMessage.contains("logout")){
                break;
            }else if(serverMessage.contains("parameter")){
                String fileName = serverMessage.split("-")[1];
                Integer fileSize = Integer.parseInt(serverMessage.split("-")[2]);
                Integer selectedChunkSize = Integer.parseInt(serverMessage.split("-")[3]);
                uploadfile(fileName, fileSize, selectedChunkSize);
            }
            String userCommand = s.nextLine();
            out.writeObject(userCommand);
        }
    }

    private static void uploadfile(String fileName, Integer fileSize, Integer selectedChunkSize) throws IOException, ClassNotFoundException {
        FileInputStream fin = new FileInputStream(new File("src\\"+fileName));
        byte[] bytes = new byte[(int) selectedChunkSize];
        System.out.println("fileName : "+fileName+" fileSize : "+fileSize+" selectedChunkSize : "+selectedChunkSize);
        int count;
        String confirmationMessage;
        while((count=fin.read(bytes))>0){
            socket.getOutputStream().write(bytes);
            System.out.println("sent a chunk of size : "+count);
            confirmationMessage = (String) in.readObject();
            System.out.println("server message : "+confirmationMessage);
            if(!confirmationMessage.contains("successful")){
                out.writeObject("unsuccessful");
                break;
            }
        }
        fin.close();
        out.writeObject("transmission complete");
        confirmationMessage = (String) in.readObject();
        System.out.println("server message : "+confirmationMessage);
    }

}
