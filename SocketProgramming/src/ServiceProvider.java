import java.io.*;
import java.net.Socket;
import java.util.Date;

public class ServiceProvider extends Thread {
    Socket socket;
    ObjectOutputStream out;
    ObjectInputStream in;
    Integer id=0;


    public ServiceProvider(Socket clientSocket) throws IOException {
        socket = clientSocket;
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(this.socket.getInputStream());
    }

    public void run() {
        try {
            boolean loginFlag = login();
            while (loginFlag) {
                String userCommand = (String) in.readObject();
                if (userCommand.equalsIgnoreCase("logout")) {
                    logout();
                } else if (userCommand.equalsIgnoreCase("lookup-student-list")) {
                    lookupStudentList();
                } else if (userCommand.equalsIgnoreCase("lookup-own-files")) {
                    lookupOwnFiles();
                } else if (userCommand.equalsIgnoreCase("lookup-other-files")) {
                    lookupOtherFiles();
                }else if (userCommand.contains("upload")) {
                    int fileSize = Integer.parseInt(userCommand.split("-")[3]);
                    if(Server.currentBufferSize+fileSize>Server.bufferSize){
                        out.writeObject("maximum buffer size overflow");
                        continue;
                    }
                    Server.currentBufferSize += fileSize;
                    String fileType = userCommand.split("-")[1];
                    String fileName = userCommand.split("-")[2];
                    String fileId = "src\\files\\"+id+"\\"+fileType+"\\"+fileName;
                    int selectedChunkSize=100;
//                    int selectedChunkSize = ThreadLocalRandom.current().nextInt(Server.minSize, Server.maxSize + 1);
                    out.writeObject("parameter-"+fileName+"-"+fileSize+"-"+selectedChunkSize);
                    uploadFile(fileId, fileSize, selectedChunkSize);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void uploadFile(String fileId, int fileSize, int selectedChunkSize) throws IOException, ClassNotFoundException {
        FileOutputStream fos = new FileOutputStream(fileId);
        byte[] bytes = new byte[(int) selectedChunkSize];
        int numberOfChunk = (fileSize%selectedChunkSize == 0)? (fileSize/selectedChunkSize) : (fileSize/selectedChunkSize+1);
        System.out.println("fileId : "+fileId+" fileSize : "+fileSize+" selectedChunkSize : "+selectedChunkSize+" noOfChunk : "+numberOfChunk);
        int count=0;
        while(count<numberOfChunk){
            count+=1;
            System.out.println("received chunk no : "+count);
            out.writeObject("successfully received chunk no : "+count);
            socket.getInputStream().read(bytes);
            fos.write(bytes);
            System.out.println("wrote chunk no : "+count+" to file");
        }
        fos.close();
        String completionMessage = (String) in.readObject();
        System.out.println("client message : "+completionMessage);
        if(!completionMessage.contains("complete") || count != numberOfChunk){
            // delete that file from server
            File file = new File(fileId);
            file.delete();
            System.out.println("transmission failure, file deleted");
            out.writeObject("transmission cancelled");
        }else{
            out.writeObject("transmission completed successfully");
        }
        Server.currentBufferSize -= fileSize;
    }

    private void lookupOtherFiles() throws IOException {
        String otherPublicList ;
        if(Server.userArray.size()==0){
            otherPublicList = "none uploaded public file \n";
        }else{
            otherPublicList = "other public file list : \n";
        }
        for (int i = 0; i < Server.userArray.size(); i++) {
            if (Server.userArray.get(i).equals(id)) {
                continue;
            }

            File folder = new File("src\\files\\" + Server.userArray.get(i) + "\\public");
            String[] listOfPublicFiles = folder.list();
            if(listOfPublicFiles.length!=0){
                otherPublicList += Server.userArray.get(i)+" public file list : \n";
            }
            for (int j = 0; j < listOfPublicFiles.length; j++) {
                otherPublicList += listOfPublicFiles[j] + "\n";
            }
        }
        out.writeObject(otherPublicList);
    }

    private void lookupOwnFiles() throws IOException {
        String fileList;
        File publicFolder = new File("src\\files\\" + id + "\\public");
        File privateFolder = new File("src\\files\\" + id + "\\private");
        String[] listOfPublicFiles = publicFolder.list();
        String[] listOfPrivateFiles = privateFolder.list();
        if(listOfPrivateFiles.length==0 && listOfPublicFiles.length==0){
            fileList = "you haven't uploaded any file\n";
        }else{
            fileList = "own file list : \n";
        }

        for (int i = 0; i < listOfPublicFiles.length; i++) {
            fileList += listOfPublicFiles[i] + " --> public \n";
        }
        for (int i = 0; i < listOfPrivateFiles.length; i++) {
            fileList += listOfPrivateFiles[i] + " --> private \n";
        }
        out.writeObject(fileList);
    }

    private void lookupStudentList() throws IOException {
        String studentList = "student list : \n";
        studentList += id + "\n";
        for (int i = 0; i < Server.userArray.size(); i++) {
            if (Server.userArray.get(i) == id) {
                continue;
            }
            if (Server.currentUserArray.contains(Server.userArray.get(i))) {
                studentList += Server.userArray.get(i) + " --> online \n";
            } else {
                studentList += Server.userArray.get(i) + " --> offline \n";
            }
        }
        out.writeObject(studentList);
    }

    private void logout() throws IOException {
        Server.currentUserArray.remove(id);
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
            out.writeObject("login denied, " + userId + " active");
            socket.close();
            return false;
        } else if (Server.userArray.contains(userId)) {
            out.writeObject("welcome again " + userId);
            Server.currentUserArray.add(userId);
            id = userId;
            return true;
        } else {
            out.writeObject("welcome " + userId);
            //add create new folder functionality
            Server.currentUserArray.add(userId);
            Server.userArray.add(userId);
            id = userId;
            new File("src\\files\\" + sUserId).mkdir();
            new File("src\\files\\" + sUserId + "\\public").mkdir();
            new File("src\\files\\" + sUserId + "\\private").mkdir();
            return true;
        }

    }
}
