import java.net.*;
import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.Base64;

public class EchoServer extends Thread {
    private String socketLoggedUser = "";
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private static ArrayList<String> loggedInUsers = new ArrayList<>();


    EchoServer(Socket openSocket) throws IOException {
        clientSocket = openSocket;
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        this.start();
    }

    public void run(){

        boolean b = true;
        while (b) {
            try {
                String inputLine;

                while (b) {
                    out.flush();
                    if ((inputLine = in.readLine()) != null) {
                        System.out.println(inputLine);
                        if (inputLine.contains("requestUsernameCheck")) {
                            boolean available = checkRegistration(inputLine.substring(inputLine.indexOf("(" + 1), inputLine.indexOf(")")));
                            out.println("available - " + available);
                        } else if (inputLine.contains("requestUserAccount")) {
                            System.out.println("requestUserAccount command summoned.");
                            boolean accountCreationSuccessful = requestUserAccount(inputLine);
                            if (accountCreationSuccessful) {
                                out.println("Account created successfully.");
                            } else {
                                System.out.println("Failed: Account creation unsuccessful!");
                                out.println("Failed: Account creation unsuccessful!");
                            }
                        } else if (inputLine.contains("requestSaltFromUsername")) {
                            out.println(requestSaltFromUserName(inputLine.substring(inputLine.indexOf("(") + 1, inputLine.indexOf(")"))));
                        } else if (inputLine.contains("requestLogin")) {
                            if (requestLogin(inputLine)) {
                                out.println("Success");
                            } else {
                                System.out.println("Failed: User unable to log in!");
                                out.println("Failed: User unable to log in!");
                            }
                        } else if (inputLine.contains("requestLogout")) {
                            requestLogout();
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
                b = false;
                requestLogout();
                try{
                    in.close();
                    out.close();
                    clientSocket.close();
                }catch(IOException eTwo){
                    eTwo.printStackTrace();
                }
            }

        }

    }

    private boolean requestLogin(String inputLine) {
        boolean returnStatement = false;

        String email, password, input, pepperedPass;
        pepperedPass = "";
        input = inputLine.substring(inputLine.indexOf('(') + 1, inputLine.indexOf(')'));
        String[] args = input.split(",");
        email = args[0];
        boolean isAlreadyLoggedIn = false;
        for (String s : loggedInUsers) {
            if (email.equalsIgnoreCase(s)) {
                isAlreadyLoggedIn = true;
            }
        }
        if (!(isAlreadyLoggedIn)) {
            password = args[1];
            try {
                byte[] pepPass = Password.getSaltedHash(password.toCharArray());
                pepperedPass = Base64.getEncoder().encodeToString(pepPass);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Connection c = null;
            if (!(pepperedPass.equals(""))) {
                try {
                    Class.forName("org.sqlite.JDBC");
                    c = DriverManager.getConnection("jdbc:sqlite:database.db");
                    PreparedStatement s = c.prepareStatement("SELECT Email,password FROM Customer\n" +
                                    "WHERE (Email = ?) AND (password = ?);");
                    s.setString(1, email);
                    s.setString(2, pepperedPass);
                    ResultSet rs = s.executeQuery();
                    while (rs.next()) {
                        if (rs.getString("Email").equalsIgnoreCase(email) && rs.getString("password").equals(pepperedPass)) {
                            returnStatement = true;
                            socketLoggedUser = email;
                            loggedInUsers.add(email);
                            break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (c != null) {
                            c.close();
                        }
                    } catch (Exception e) {

                    }
                }
            }
        }
        return returnStatement;
    }

    private boolean checkRegistration(String userName) {
        Connection conn = null;
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:database.db");
            Statement stmn = conn.createStatement();
            String sql = "SELECT Email FROM Customer";
            ResultSet rs = stmn.executeQuery(sql);
            while (rs.next()) {
                if (rs.getString("Email").equalsIgnoreCase(userName)) {
                    return false;
                }
            }
            stmn.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    private String requestSaltFromUserName(String username) {
        String salt = "";
        Connection c = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:database.db");
            Statement s = c.createStatement();
            String sql = "SELECT * FROM Customer";
            ResultSet rs = s.executeQuery(sql);
            while (rs.next()) {
                String current = rs.getString("email");
                if (current.equals(username)) salt = rs.getString("salt");
            }
            s.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (c != null) {
                    c.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return salt;
    }

    private boolean requestUserAccount(String inputLine) {
        String input, email, password, salt, pepperedPassAsString, cabinNo, fullName;
        pepperedPassAsString = "";
        byte[] pepperedPass;
        input = inputLine.substring(inputLine.indexOf('(') + 1, inputLine.indexOf(')'));
        String[] str = input.split(",");
        email = str[0];
        password = str[1];
        try {
            pepperedPass = Password.getSaltedHash(password.toCharArray());
            pepperedPassAsString = Base64.getEncoder().encodeToString(pepperedPass);
        } catch (Exception e) {
            e.printStackTrace();
        }
        cabinNo = str[2];
        fullName = str[4];
        salt = str[3];


        if (checkRegistration(email) && !(pepperedPassAsString.equals(""))) {
            Connection conn = null;
            try {
                Class.forName("org.sqlite.JDBC");
                conn = DriverManager.getConnection("jdbc:sqlite:database.db");
                PreparedStatement stmn = conn.prepareStatement("INSERT INTO Customer (Name, Email, CabinNo, Password, Salt)" +
                        "Values(?,?,?,?,?);");
                stmn.setString(1, fullName);
                stmn.setString(2, email);
                stmn.setString(3, cabinNo);
                stmn.setString(4, pepperedPassAsString);
                stmn.setString(5, salt);
                stmn.execute();
            } catch (Exception e) {
                return false;
            } finally {
                try {
                    if (conn != null) {
                        conn.close();
                    }
                } catch (Exception e) {
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }

    private void requestLogout() {
        loggedInUsers.remove(socketLoggedUser);
        socketLoggedUser = "";
    }
}
