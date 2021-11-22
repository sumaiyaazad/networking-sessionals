import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private static Socket socket;
    private static ObjectOutputStream out;
    private static ObjectInputStream in;
    private static Scanner s = new Scanner(System.in);

    public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
        socket = new Socket("localhost", 6666);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());

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
            } else if(serverMessage.contains("download")){
                String myId = serverMessage.split("-")[1];
                String fileName = serverMessage.split("-")[2];
                Integer fileSize= Integer.parseInt(serverMessage.split("-")[3]);
                downloadFile(myId, fileName, fileSize);
            } else if(serverMessage.contains("view-message")){
                viewMessage();
            }
            String userCommand = s.nextLine();
            out.writeObject(userCommand);
        }
    }

    private static void viewMessage() throws IOException, ClassNotFoundException {
        String unreadMessage = (String) in.readObject();
        System.out.println("server message : "+unreadMessage);
    }

    private static void downloadFile(String myId, String fileName, Integer fileSize) throws IOException, ClassNotFoundException {
        String serverMessage = (String) in.readObject();
        System.out.println("server message : "+serverMessage);
        if(serverMessage.contains("exist")){
            System.out.println("file download failure, try again later");
            return;
        }
        byte[] bytes = new byte[(int) Server.bufferSize];
//        System.out.println("do you want to download the folder in public or private directory?");
//        String type = s.nextLine();
        FileOutputStream fos = new FileOutputStream("src\\files\\"+myId+"\\public\\"+fileName);
        int numOfChunk = fileSize%Server.bufferSize == 0 ? (fileSize/Server.bufferSize) : (fileSize/Server.bufferSize+1);
        int count=0;
        while(count<numOfChunk){
            count+=1;
            socket.getInputStream().read(bytes);
            fos.write(bytes);
        }
        serverMessage = (String) in.readObject();
        System.out.println("server message : "+serverMessage);
        fos.close();
    }

    private static void uploadfile(String fileName, Integer fileSize, Integer selectedChunkSize) throws IOException, ClassNotFoundException {
                FileInputStream fin = new FileInputStream(new File("src\\" + fileName));
                byte[] bytes = new byte[(int) selectedChunkSize];
                System.out.println("fileName : " + fileName + " fileSize : " + fileSize + " selectedChunkSize : " + selectedChunkSize);
                int count;
                String confirmationMessage = "";
                boolean error = false;
                while ((count = fin.read(bytes)) > 0) {
                    socket.getOutputStream().write(bytes);
                    System.out.println("sent a chunk of size : " + count);
                    socket.setSoTimeout(30000);
                    confirmationMessage = (String) in.readObject();
                    System.out.println("server message : " + confirmationMessage);
                if (!confirmationMessage.contains("successful")) {
                    error = true;
                    break;
                }
                }
                fin.close();
            if (error) {
                System.out.println("server did not send confirmation");
                out.writeObject("unsuccessful");
            } else {
                out.writeObject("transmission complete");
                confirmationMessage = (String) in.readObject();
                System.out.println("server message : " + confirmationMessage);
            }

    }
}
