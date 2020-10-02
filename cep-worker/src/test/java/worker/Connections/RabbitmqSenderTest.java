package worker.Connections;

import com.github.fridujo.rabbitmq.mock.MockConnectionFactory;
import com.rabbitmq.client.*;
import junit.framework.TestCase;
import worker.utils.Serializer;
import worker.utils.SerializerTest;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RabbitmqSenderTest extends TestCase{

    

    public void testPublish() throws IOException, TimeoutException {

        ConnectionFactory factory = new MockConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        String queueName = channel.queueDeclare().getQueue();

        RabbitmqSender sender = new RabbitmqSender(channel);

        channel.queueBind(queueName, "EXCHANGE", "MyAvroEvent");

        sender.Publish("MyAvroEvent",SerializerTest.generateEvent());

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {
                assertEquals(SerializerTest.generateEvent(),Serializer.AvroDeserialize(body,SerializerTest.generateSchema()));
            }
        };
        channel.basicConsume(queueName, true, consumer);
        channel.close();
        connection.close();

    }

}
