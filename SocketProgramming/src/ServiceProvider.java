import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Date;

public class ServiceProvider extends Thread {
    Socket socket;
    ObjectOutputStream out;
    ObjectInputStream in;
    Integer id;


    public ServiceProvider(Socket clientSocket) throws IOException {
        socket = clientSocket;
        out = new ObjectOutputStream(this.socket.getOutputStream());
        in = new ObjectInputStream(this.socket.getInputStream());
    }

    public void run() {
        try {
            boolean loginFlag = login();
            while (loginFlag) {
                String userCommand = (String) in.readObject();
                if(userCommand.equalsIgnoreCase("logout")){
                    logout();
                }else if(userCommand.equalsIgnoreCase("lookup-student-list")){
                    lookupStudentList();
                }else if(userCommand.equalsIgnoreCase("lookup-own-files")){
                    lookupOwnFiles();
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void lookupOwnFiles() throws IOException {
        String studentList = "student list : \n";
        for(int i=0;i<Server.userArray.size();i++){
            if(Server.userArray.get(i) != id){
                studentList += Server.userArray.get(i)+"\n";
            }
        }
        out.writeObject(studentList);
    }

    private void lookupStudentList() throws IOException {
        String studentList = "student list : \n";
        for(int i=0;i<Server.userArray.size();i++){
            if(Server.userArray.get(i) != id){
                if(Server.currentUserArray.contains(Server.userArray.get(i))){
                    studentList += Server.userArray.get(i)+" --> online \n";
                }else{
                    studentList += Server.userArray.get(i)+"\n";
                }
            }
        }
        out.writeObject(studentList);
    }

    private void logout() throws IOException {
        Server.currentUserArray.remove(Server.currentUserArray.indexOf(id));
        out.writeObject("logout successful");
        socket.close();
    }

    private boolean login() throws IOException, ClassNotFoundException {

        //reads user id and checks whether a current user is trying to establish a connection
        //if inactive user tries to establish a connection, then add that user to currentUserArray
        //if anew user tries to establish a connection add that user to currentUserArray and userArray

        out.writeObject("login ID :");
        String sUserId = (String) in.readObject();
        Integer userId = Integer.parseInt(sUserId);
        if (Server.currentUserArray.contains(userId)) {
            out.writeObject("login denied, "+userId + " active");
            socket.close();
            return false;
        } else if (Server.userArray.contains(userId)) {
            out.writeObject("welcome again "+userId);
            Server.currentUserArray.add(userId);
            id=userId;
            return true;
        } else {
            out.writeObject("welcome "+userId);
            //add create new folder functionality
            Server.currentUserArray.add(userId);
            id=userId;
            new File("src\\files\\"+sUserId).mkdir();
            new File("src\\files\\"+sUserId+"\\public").mkdir();
            new File("src\\files\\"+sUserId+"\\private").mkdir();
            Server.userArray.add(id);
            return true;
        }

    }
}
