package worker.Subscriber;

import com.github.fridujo.rabbitmq.mock.MockConnectionFactory;
import com.rabbitmq.client.*;
import junit.framework.TestCase;

import org.apache.avro.generic.GenericData;
import worker.Connections.RabbitmqSender;
import worker.utils.Serializer;
import worker.utils.SerializerTest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;


public class SubscriberTest extends TestCase{

    public void testUpdate() throws IOException, TimeoutException {
        GenericData.Record event = SerializerTest.generateEvent();
        Object [] evt = new Object[2];
        evt[0] = "Van";
        evt[1] = 1; //matching attribute fields of event

        Map<String,Object> evet = new HashMap<>();
        evet.put("carType", "Van");
        evet.put("carId",1);





        ConnectionFactory factory = new MockConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        String queueName = channel.queueDeclare().getQueue();
        RabbitmqSender sender = new RabbitmqSender(channel);


        Subscriber subs = new Subscriber("1",sender,SerializerTest.generateSchema());
        channel.queueBind(queueName, "EXCHANGE", "1");


        subs.update(evet);



        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {
                assertEquals(SerializerTest.generateEvent(), Serializer.AvroDeserialize(body,SerializerTest.generateSchema()));
            }
        };
        channel.basicConsume(queueName, true, consumer);
        channel.close();
        connection.close();



    }


}
