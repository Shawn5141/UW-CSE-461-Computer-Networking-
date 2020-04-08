package Lab1;
import java.net.*;
import java.io.*;
public class client{
    public static void main(String[] args)throws IOException {
        Socket s = new Socket("localhost", 4999);
        PrintWriter pr =new PrintWriter(s.getOutputStream());
        //https://docs.oracle.com/javase/8/docs/api/java/io/PrintWriter.html
        pr.println("it is working");
        pr.flush();

        InputStreamReader in =new InputStreamReader(s.getInputStream());
        //BufferReader def : https://www.geeksforgeeks.org/java-io-bufferedreader-class-java/
        BufferedReader bf =new BufferedReader(in);
        String str =bf.readLine();
        System.out.println("server : "+str);



    }
}