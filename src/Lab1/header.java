package Lab1;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.*;
import java.nio.*;
//From piazza
//So client send psecret 0 on the header with "hello world" on the payload
// then server send back psecret 0 on the header and secretA on the payload
// (random number generated, lets say secretA = 200), then client send back psecret = 200
// on its header?

public class header{
    ByteBuffer byteBuffer=ByteBuffer.allocate(12);
    public header(int payload_len,int psecret,int step, int studentID){
        byteBuffer.putInt(payload_len);

        byteBuffer.putInt(psecret);
        byteBuffer.putShort((short)step);
        byteBuffer.putShort((short)studentID);
        System.out.println("Header "+Arrays.toString(byteBuffer.array()));
    }
}