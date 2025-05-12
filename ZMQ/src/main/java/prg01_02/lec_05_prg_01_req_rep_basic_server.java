package prg01_02;

import org.zeromq.SocketType;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;

public class lec_05_prg_01_req_rep_basic_server {
    public static void main(String[] args) throws Exception
    {
        try (ZContext context = new ZContext()) {
            ZMQ.Socket socket = context.createSocket(SocketType.REP);
            socket.bind("tcp://*:1207");

            while (true) {
                byte[] reply = socket.recv(0);
                System.out.println(
                        "Received request : " + new String(reply, ZMQ.CHARSET)
                );

                Thread.sleep(1000); //  Do some 'work'

                socket.send("world".getBytes(ZMQ.CHARSET), 0);
            }
        }
    }
}