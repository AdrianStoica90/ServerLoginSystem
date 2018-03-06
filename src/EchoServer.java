import java.net.*;
import java.io.*;
import java.sql.*;
import java.util.Base64;

import org.sqlite.JDBC;

public class EchoServer extends Thread {
    private static String loggedUser = "";
    Socket clientSocket;
    PrintWriter out;
    BufferedReader in;


    public EchoServer(Socket openSocket) throws IOException{
        clientSocket = openSocket;
        out = new PrintWriter(clientSocket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        this.start();
    }

    public void run(){
        /*
        if (args.length != 1) {
            System.err.println("Usage: java EchoServer <port number>");
            System.exit(1);
        }
        
        int portNumber = Integer.parseInt(args[0]);
        */
        int portNumber = 80;

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
                                System.out.println("Failsed");
                                out.println("Failed");
                            }
                        } else if (inputLine.contains("requestSaltFromUsername")) {
                            out.println(requestSaltFromUserName(inputLine.substring(inputLine.indexOf("(") + 1, inputLine.indexOf(")"))));
                        } else if (inputLine.contains("requestLogin")) {
                            if (requestLogin(inputLine)) {
                                out.println("Success");
                            } else out.println("Failed");
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Exception caught when trying to listen on port "
                        + portNumber + " or listening for a connection");
                System.out.println(e.getMessage());
                b = false;
                try{
                    in.close();
                    out.close();
                    clientSocket.close();
                    this.stop();
                }catch(IOException eTwo){
                    eTwo.printStackTrace();
                }
            }
        }
    }

    private static boolean requestLogin(String inputLine) {
        boolean returnStatement = false;

        String username, password, input, pepperedPass;
        pepperedPass = "";
        input = inputLine.substring(inputLine.indexOf('(') + 1, inputLine.indexOf(')'));
        String[] args = input.split(",");
        username = args[0];
        if (!(loggedUser.equalsIgnoreCase(username))) {
            password = args[1];
            try {
                byte[] pepPass = Password.getSaltedHash(password.toCharArray());
                pepperedPass = Base64.getEncoder().encodeToString(pepPass);
            } catch (Exception e) {

            }
            Connection c = null;
            if (!(pepperedPass.equals(""))) {
                try {
                    Class.forName("org.sqlite.JDBC");
                    c = DriverManager.getConnection("jdbc:sqlite:database.db");
                    Statement s = c.createStatement();
                    String sql = "SELECT * FROM Users";
                    ResultSet rs = s.executeQuery(sql);
                    while (rs.next()) {
                        if (rs.getString("username").equalsIgnoreCase(username) && rs.getString("password").equals(pepperedPass)) {
                            returnStatement = true;
                            loggedUser = username;
                            break;
                        }
                    }
                } catch (Exception e) {

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

    private static boolean checkRegistration(String userName) {
        Connection conn = null;
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection("jdbc:sqlite:database.db");
            Statement stmn = conn.createStatement();
            String sql = "SELECT * FROM Users";
            ResultSet rs = stmn.executeQuery(sql);
            while (rs.next()) {
                if (rs.getString("userName").equalsIgnoreCase(userName)) {
                    return false;
                }
            }
            stmn.close();
        } catch (Exception e) {

        } finally {
            try {
                if (conn != null) {
                    conn.close();
                }
            } catch (Exception e) {

            }
        }


        return true;
    }

    private static String requestSaltFromUserName(String username) {
        String salt = "";
        Connection c = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:database.db");
            Statement s = c.createStatement();
            String sql = "SELECT * FROM Users";
            ResultSet rs = s.executeQuery(sql);
            while (rs.next()) {
                String current = rs.getString("userName");
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

            }
        }
        return salt;
    }

    private static boolean requestUserAccount(String inputLine) {
        String input, username, password, salt, pepperedPassAsString;
        pepperedPassAsString = "";
        byte[] pepperedPass;
        input = inputLine.substring(inputLine.indexOf('(') + 1, inputLine.indexOf(')'));
        String[] str = input.split(",");
        username = str[0];
        password = str[1];
        try {
            pepperedPass = Password.getSaltedHash(password.toCharArray());
            pepperedPassAsString = Base64.getEncoder().encodeToString(pepperedPass);
        } catch (Exception e) {

        }
        salt = str[2];

        if (checkRegistration(username) && !(pepperedPassAsString.equals(""))) {
            Connection conn = null;
            String sql = "";
            try {
                Class.forName("org.sqlite.JDBC");
                conn = DriverManager.getConnection("jdbc:sqlite:database.db");
                Statement stmn = conn.createStatement();
                sql = "INSERT INTO Users (userName, password, salt)" +
                        "Values('" + username + "', '" + pepperedPassAsString + "', '" + salt + "');";
                stmn.execute(sql);
                stmn.close();
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
}
