import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by 1703053 on 26/02/2018.
 */
public class Controller {

    public static void main(String[] args) throws IOException{
        int portNumber = 443;
        ServerSocket serverSocket = new ServerSocket(portNumber);
        while(true){
            Socket clientSocket = serverSocket.accept();
            new EchoServer(clientSocket);
        }
    }
}
