package prg07_08;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Poller;

import java.util.Random;

public class client888 {
    public static void main(String[] args) {

        ZContext context = new ZContext();
        ZMQ.Socket subscriber = context.createSocket(SocketType.SUB);
        subscriber.subscribe("".getBytes(ZMQ.CHARSET));
        subscriber.connect("tcp://localhost:5556");
        ZMQ.Socket publisher = context.createSocket(SocketType.PUSH);
        publisher.connect("tcp://localhost:5557");

        String clientID = args[0];
        Random srandom = new Random(System.currentTimeMillis());
        Poller poller = context.createPoller(1);
        poller.register(subscriber, Poller.POLLIN);

        while (!Thread.currentThread().isInterrupted()) {
            poller.poll(100);
            if (poller.pollin(0)) { // 메세지가 온 게 있는지
                byte[] message = subscriber.recv();
                System.out.println(clientID + ": receive status => " + new String(message, ZMQ.CHARSET));
            } else {
                int rand = srandom.nextInt(99) + 1;
                if (rand < 10) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    String msg = "(" + clientID + ":ON)";
                    publisher.send(msg);
                    System.out.println(clientID + ": send status - activated");
                } else if (rand > 90) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    String msg = "(" + clientID + ":OFF)";
                    publisher.send(msg);
                    System.out.println(clientID + ": send status - deactivated");
                }
            }
        }

    }
}