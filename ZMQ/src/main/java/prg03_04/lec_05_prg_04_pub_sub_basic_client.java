package prg03_04;

import java.util.StringTokenizer;

import org.zeromq.SocketType;
import org.zeromq.ZMQ;
import org.zeromq.ZContext;

public class lec_05_prg_04_pub_sub_basic_client {
    public static void main(String[] args)
    {
        try (ZContext context = new ZContext()) {
            System.out.println("Collecting updates from weather server...");
            ZMQ.Socket subscriber = context.createSocket(SocketType.SUB);
            subscriber.connect("tcp://localhost:5556");

            String filter = (args.length > 0) ? args[0] : "10001 ";
            subscriber.subscribe(filter.getBytes(ZMQ.CHARSET));

            int update_nbr;
            double total_temp = 0;
            for (update_nbr = 0; update_nbr < 20; update_nbr++) {
                String string = subscriber.recvStr(0).trim();

                StringTokenizer sscanf = new StringTokenizer(string, " ");
                int zipcode = Integer.valueOf(sscanf.nextToken());
                int temperature = Integer.valueOf(sscanf.nextToken());
                int relhumidity = Integer.valueOf(sscanf.nextToken());

                total_temp += temperature;
                System.out.printf("Receive temperature for zipcode '%d' was %d F\n", zipcode, temperature);
            }

            System.out.println(
                    String.format("Average temperature for zipcode '%s' was %.2f F", filter, (total_temp / update_nbr))
            );
        }
    }
}
