package prg03_04;

import java.util.Random;

import org.zeromq.SocketType;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;

public class lec_05_prg_03_pub_sub_basic_server {
    public static void main(String[] args) throws Exception
    {
        System.out.println("Publising updates at weather server...");

        try (ZContext context = new ZContext()) {
            ZMQ.Socket publisher = context.createSocket(SocketType.PUB);
            publisher.bind("tcp://*:5556");

            Random srandom = new Random(System.currentTimeMillis());
            while (!Thread.currentThread().isInterrupted()) {
                //  Get values that will fool the boss
                int zipcode, temperature, relhumidity;
                zipcode = 1 + srandom.nextInt(99999);
                temperature = srandom.nextInt(215) - 80;
                relhumidity = srandom.nextInt(50) + 10;

                //  Send message to all subscribers
                String update = String.format(
                        "%d %d %d", zipcode, temperature, relhumidity
                );
                publisher.send(update, 0);
            }
        }
    }
}
