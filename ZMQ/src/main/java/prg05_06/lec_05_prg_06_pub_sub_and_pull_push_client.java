package prg05_06;

import org.zeromq.SocketType;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZContext;

import java.util.Random;

public class lec_05_prg_06_pub_sub_and_pull_push_client {
    public static void main(String[] args) {
        ZContext context = new ZContext();
        ZMQ.Socket subscriber = context.createSocket(SocketType.SUB);
        subscriber.subscribe("".getBytes(ZMQ.CHARSET));
        subscriber.connect("tcp://localhost:5556");
        ZMQ.Socket publisher = context.createSocket(SocketType.PUSH);
        publisher.connect("tcp://localhost:5557");

        Random srandom = new Random(System.currentTimeMillis());
        Poller poller = context.createPoller(1);
        poller.register(subscriber, Poller.POLLIN);
        while (!Thread.currentThread().isInterrupted()) {
            poller.poll(100);
            if (poller.pollin(0)) { // 메세지가 온 게 있는지
                byte[] message = subscriber.recv();
                System.out.println("I: receive message " + new String(message, ZMQ.CHARSET));
            } else {
                int rand = srandom.nextInt(99) + 1;
                if (rand < 10) {
                    publisher.send((String.format("%d", rand)).getBytes(ZMQ.CHARSET));
                    System.out.println("I: sending message " + rand);
                }
            }
        }
    }
}
