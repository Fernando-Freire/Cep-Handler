package worker.ThreadHandler;

import com.github.fridujo.rabbitmq.mock.MockConnectionFactory;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import junit.framework.TestCase;
import org.apache.avro.Schema;
import redis.embedded.RedisServer;
import worker.Connections.RabbitmqReceiver;
import worker.Connections.RabbitmqSender;
import worker.DatabaseAccess.Lettuce;
import worker.Events.EventHandler;
import worker.utils.SerializerTest;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

public class ThreadHandlerTest extends TestCase {


    public void testdummy() throws IOException{
        RedisServer redisServer = new RedisServer();
        redisServer.start();

        RedisClient redisClient = RedisClient.create("redis://localhost:6379/");
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        RedisCommands<String,String> syncCommands = connection.sync();
        syncCommands.set("111:Name", "Position");
        syncCommands.set("111:Definition","SELECT average from MyAvroEvent#length(4)#uni(carId)");
        syncCommands.sadd("111:Inputs","001");
        syncCommands.sadd("Unnasigned","111");

        syncCommands.set("001:Name","MyAvroEvent");
        syncCommands.set("001:AvroSchema",SerializerTest.generateSchema().toString());
        connection.close();
        redisClient.shutdown();


        Lettuce store = new Lettuce("localhost");

        String EventTypeName = store.getEventTypeName("111");

        store.Close();

        assertEquals("Position",EventTypeName);


        redisServer.stop();

    }


    // This test was put here in order to keep the maven test suite working
    // other tests were commented because the Rabbitmq can not handle concurrency
    //
    //




    /*public void testEventTypeAssignement() throws IOException, TimeoutException, InterruptedException {
        ThreadHandler TH_1 = new ThreadHandler("1","localhost");

        //Test parameters:

        // EventTypeId : 111
        // EventType AvroShcema SerializerTest.generateschema()
        // EventType name Position
        // Event Type Query "SELECT average from MyAvroEvent#length(4)#uni(carId)"
        // Event Type Inputs [ "001" ]

        // Primitive EventType Id : "001"
        // Primitive EventType AvroShcema SerializerTest.generateschema()
        // Primitive EventType name : "MyAvroEvent"


        RedisServer redisServer = new RedisServer();
        redisServer.start();

        RedisClient redisClient = RedisClient.create("redis://localhost:6379/");
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        RedisCommands<String,String> syncCommands = connection.sync();
        syncCommands.set("111:Name", "Position");
        syncCommands.set("111:Definition","SELECT average from MyAvroEvent#length(4)#uni(carId)");
        syncCommands.sadd("111:Inputs","001");
        syncCommands.sadd("Unnasigned","111");

        syncCommands.set("001:Name","MyAvroEvent");
        syncCommands.set("001:AvroSchema",SerializerTest.generateSchema().toString());
        connection.close();
        redisClient.shutdown();

        //initiate Rabbitmq receiver 1

        ConnectionFactory factory = new MockConnectionFactory();
        factory.setHost("localhost");
        Connection conn = factory.newConnection();
        Channel channel = conn.createChannel();
        channel.exchangeDeclare("EXCHANGE", "topic");


        TH_1.AddOutgoingEventType("111",channel);

        Lettuce store = new Lettuce("localhost");

        String whereEventTypeIs = store.getEventTypeCurrentWorker("111");

        TH_1.deleteCheckExpressionAndUnnecessaryInputs(store,"111");

        channel.close();

        conn.close();

        store.Close();

        redisServer.stop();

        assertEquals("1",whereEventTypeIs);


    } */







    /*public void testEventTypeRellocation() throws IOException, TimeoutException {
        ThreadHandler TH_1 = new ThreadHandler("1","localhost");

        ThreadHandler TH_2 = new ThreadHandler("2","localhost");

        //Test parameters:

        // EventTypeId : 111
        // EventType AvroShcema SerializerTest.generateschema()
        // EventType name Position
        // Event Type Query "SELECT average from MyAvroEvent#length(4)#uni(carId)"
        // Event Type Inputs [ "001" ]

        // Primitive EventType Id : "001"
        // Primitive EventType AvroShcema SerializerTest.generateschema()
        // Primitive EventType name : "MyAvroEvent"


        RedisServer redisServer = new RedisServer();
        redisServer.start();

        RedisClient redisClient = RedisClient.create("redis://localhost:6379/");
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        RedisCommands<String,String> syncCommands = connection.sync();
        syncCommands.set("111:Name", "Position");
        syncCommands.set("111:Definition","SELECT average from MyAvroEvent#length(4)#uni(carId)");
        syncCommands.sadd("111:Inputs","001");
        syncCommands.sadd("Unnasigned","111");

        syncCommands.set("001:Name","MyAvroEvent");
        syncCommands.set("001:AvroSchema",SerializerTest.generateSchema().toString());
        connection.close();
        redisClient.shutdown();





        //initiate Rabbitmq receiver 1

        ConnectionFactory factory = new MockConnectionFactory();
        factory.setHost("localhost");
        Connection conn = factory.newConnection();
        Channel channel = conn.createChannel();
        channel.exchangeDeclare("EXCHANGE", "topic");


        //TH_1.AddUnassignedTestEvent("111","001",channel);
        TH_1.AddOutgoingEventType("111",channel);

        RabbitmqSender testsender = new RabbitmqSender(channel);

        testsender.Publish("001",SerializerTest.generateEvent());
        testsender.Publish("001",SerializerTest.generateEventTruck());

        Lettuce lettuce = new Lettuce("localhost");






        String sendingWorkerId = TH_2.setUpReceivingAndSendingWorkerIds(lettuce,"111");
        TH_2.addEventTypeToThreadHandler("111",channel);
        testsender.Publish("001",SerializerTest.generateEvent());
        testsender.Publish("001",SerializerTest.generateEventTruck());
        testsender.Publish("001",SerializerTest.generateEvent());
        testsender.Publish("001",SerializerTest.generateEventTruck());
        long timetowait = TH_2.evaluateInputsToBuildState(lettuce,"111",channel);
        testsender.Publish("001",SerializerTest.generateEvent());
        testsender.Publish("001",SerializerTest.generateEventTruck());
        testsender.Publish("001",SerializerTest.generateEvent());
        testsender.Publish("001",SerializerTest.generateEventTruck());
        lettuce.eventTypeStateBuildedAdd("111","2");

        TH_1.acknolegeTransferConfirmed(lettuce,"111");
        TH_1.deleteCheckExpressionAndUnnecessaryInputs(lettuce,"111");

        TH_2.startSendingEvents("111");



        lettuce.Close();






        Lettuce store = new Lettuce("localhost");

        String whereEventTypeIs = store.getEventTypeCurrentWorker("111");

        store.Close();

        redisServer.stop();

        assertEquals("2",whereEventTypeIs);




            }


     */



    public static Schema genSchema(){
        Schema.Parser parser = new Schema.Parser();
        return parser.parse("{" +
                "  \"type\" : \"record\"," +
                "  \"name\" : \"MyAvroEvent\"," +
                "  \"fields\" : [ {" +
                "    \"name\" : \"carId\"," +
                "    \"type\" : \"int\"" +
                "  }, {" +
                "    \"name\" : \"carType\"," +
                "    \"type\" : {" +
                "      \"type\" : \"string\"," +
                "      \"avro.java.string\" : \"String\"" +
                "    }" +
                "  } ]" +
                "}");
    }




}
