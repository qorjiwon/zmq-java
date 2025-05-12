package prg05_06;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class lec_05_prg_05_pub_sub_and_pull_push_server {
    public static void main(String[] args) {
        ZContext ctx = new ZContext();
        ZMQ.Socket publisher = ctx.createSocket(SocketType.PUB);
        publisher.bind("tcp://*:5556");
        ZMQ.Socket collector = ctx.createSocket(SocketType.PULL);
        collector.bind("tcp://*:5557");

        while (true) {
            byte[] message = collector.recv();
            System.out.println("I: publishing update " + new String(message, ZMQ.CHARSET));
            publisher.send(message);
        }
    }
}
