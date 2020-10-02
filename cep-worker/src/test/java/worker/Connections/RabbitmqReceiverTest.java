package worker.Connections;

import com.github.fridujo.rabbitmq.mock.MockConnectionFactory;
import com.rabbitmq.client.*;
import junit.framework.TestCase;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import worker.Events.EventHandler;
import worker.utils.Serializer;
import worker.utils.SerializerTest;


public class RabbitmqReceiverTest extends TestCase{


    public void testReceive() throws IOException, TimeoutException{

        ConnectionFactory factory = new MockConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare("EXCHANGE", "topic");
        RabbitmqReceiver receiver;
        receiver = new RabbitmqReceiver(SerializerTest.generateSchema(),"EXHANGE","1","MyAvroEvent",channel,new EventHandler());

        byte [] evnt = Serializer.AvroSerialize(SerializerTest.generateEvent());

        Consumer consumer = new DefaultConsumer(channel) {

            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
                GenericData.Record evt = Serializer.generateEvent();
                Schema schema = Serializer.generateSchema();
                assertEquals(evt,Serializer.AvroDeserialize(body,schema));

            }

        };
        channel.basicPublish("EXCHANGE", "1", null, evnt);
        //System.out.print(event.toString()+"*******");
        //assertEquals(SerializerTest.generateEvent(),receiver.Reroute());
        receiver.Reroute(consumer);
    }
}
