package worker.DatabaseAccess;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;

import junit.framework.TestCase;
import redis.embedded.RedisServer;
import worker.utils.SerializerTest;

import java.io.IOException;


public class LettuceTest extends TestCase {

    public void testGetEventTypeName() throws IOException {
        RedisServer redisServer = new RedisServer();
        redisServer.start();


        RedisClient redisClient = RedisClient.create("redis://localhost:6379/");
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        RedisCommands<String,String> syncCommands = connection.sync();
        syncCommands.set("111:Name","MyAvroEvent");
        connection.close();
        redisClient.shutdown();

        Lettuce lettuce = new Lettuce("localhost");
        String name = lettuce.getEventTypeName("111");

        assertEquals("MyAvroEvent",name);

        lettuce.Close();

        redisServer.stop();
    }

    public void testGetEventTypeDefinition() throws IOException {
        RedisServer redisServer = new RedisServer();
        redisServer.start();


        RedisClient redisClient = RedisClient.create("redis://localhost:6379/");
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        RedisCommands<String,String> syncCommands = connection.sync();
        syncCommands.set("111:Definition","SELECT * FROM Location");
        connection.close();
        redisClient.shutdown();

        Lettuce lettuce = new Lettuce("localhost");

        assertEquals("SELECT * FROM Location",lettuce.getEventTypeDefinition("111"));

        lettuce.Close();

        redisServer.stop();
    }

    public void testGetAvroSchema() throws IOException {
        RedisServer redisServer = new RedisServer();
        redisServer.start();


        RedisClient redisClient = RedisClient.create("redis://localhost:6379/");
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        RedisCommands<String,String> syncCommands = connection.sync();
        syncCommands.set("111:AvroSchema", SerializerTest.generateSchema().toString());
        connection.close();
        redisClient.shutdown();

        Lettuce lettuce = new Lettuce("localhost");

        assertEquals(SerializerTest.generateSchema(),lettuce.getAvroSchema("111"));

        lettuce.Close();

        redisServer.stop();
    }

    public void testSetAvroSchema() throws IOException {
        RedisServer redisServer = new RedisServer();
        redisServer.start();

        Lettuce lettuce = new Lettuce("localhost");

        lettuce.setAvroSchema("111",SerializerTest.generateSchema());

        assertEquals(SerializerTest.generateSchema(),lettuce.getAvroSchema("111"));

        lettuce.Close();

        redisServer.stop();
    }

    public void testGetEventTypeCurrentWorker() throws IOException {
        RedisServer redisServer = new RedisServer();
        redisServer.start();


        RedisClient redisClient = RedisClient.create("redis://localhost:6379/");
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        RedisCommands<String,String> syncCommands = connection.sync();
        syncCommands.set("111:WorkerId", "113");
        connection.close();
        redisClient.shutdown();

        Lettuce lettuce = new Lettuce("localhost");

        assertEquals("113",lettuce.getEventTypeCurrentWorker("111"));

        lettuce.Close();

        redisServer.stop();
    }

    public void testSetEventTypeCurrentWorker() throws IOException {
        RedisServer redisServer = new RedisServer();
        redisServer.start();



        Lettuce lettuce = new Lettuce("localhost");

        lettuce.setEventTypeCurrentWorker("111","133");

        String workerid = lettuce.getEventTypeCurrentWorker("111");

        assertEquals("133",workerid);

        lettuce.Close();

        redisServer.stop();
    }

    public void testEventTypeCurrentWorker() throws IOException {
        RedisServer redisServer = new RedisServer();
        redisServer.start();



        Lettuce lettuce = new Lettuce("localhost");

        lettuce.setEventTypeCurrentWorker("111","133");


        assertTrue(lettuce.EventTypeCurrentWorker("111","133"));
        assertFalse(lettuce.EventTypeCurrentWorker("111","113"));

        lettuce.Close();

        redisServer.stop();
    }

    public void testgetEventTypeInputs() throws IOException {
        RedisServer redisServer = new RedisServer();
        redisServer.start();


        RedisClient redisClient = RedisClient.create("redis://localhost:6379/");
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        RedisCommands<String,String> syncCommands = connection.sync();
        syncCommands.sadd("111:Inputs", "0372");
        connection.close();
        redisClient.shutdown();

        Lettuce lettuce = new Lettuce("localhost");

        assertTrue(lettuce.getEventTypeInputs("111").contains("0372"));
        assertFalse(lettuce.getEventTypeInputs("111").contains("0371"));

        lettuce.Close();

        redisServer.stop();
    }

    public void testeventTypeStateBuilded() throws IOException {
        RedisServer redisServer = new RedisServer();
        redisServer.start();


        RedisClient redisClient = RedisClient.create("redis://localhost:6379/");
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        RedisCommands<String,String> syncCommands = connection.sync();
        syncCommands.sadd("111:StateBuilded","12");
        connection.close();
        redisClient.shutdown();

        Lettuce lettuce = new Lettuce("localhost");

        assertTrue(lettuce.eventTypeStateBuilded("111","12"));
        assertFalse(lettuce.eventTypeStateBuilded("111","14"));

        lettuce.Close();

        redisServer.stop();
    }

    public void testeventTypeStateBuildedAdd() throws IOException {
        RedisServer redisServer = new RedisServer();
        redisServer.start();



        Lettuce lettuce = new Lettuce("localhost");

        lettuce.eventTypeStateBuildedAdd("111","12");



        assertTrue(lettuce.eventTypeStateBuilded("111","12"));
        assertFalse(lettuce.eventTypeStateBuilded("111","14"));

        lettuce.Close();

        redisServer.stop();
    }

    public void testeventTypeStateBuildedDel() throws IOException {
        RedisServer redisServer = new RedisServer();
        redisServer.start();



        Lettuce lettuce = new Lettuce("localhost");

        lettuce.eventTypeStateBuildedAdd("111","12");


        lettuce.eventTypeStateBuildedDel("111","12");

        assertFalse(lettuce.eventTypeStateBuilded("111","12"));


        lettuce.Close();

        redisServer.stop();
    }



    /*public void testgetUnnasignedEventTypes() throws IOException {
        RedisServer redisServer = new RedisServer();
        redisServer.start();


        RedisClient redisClient = RedisClient.create("redis://localhost:6379/");
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        RedisCommands<String,String> syncCommands = connection.sync();
        syncCommands.sadd("Unnasigned","111");
        connection.close();
        redisClient.shutdown();

        Lettuce lettuce = new Lettuce("localhost");


        assertTrue(lettuce.getUnnasignedEventTypes().contains("111"));
        assertFalse(lettuce.getUnnasignedEventTypes().contains("113"));



        lettuce.Close();

        redisServer.stop();
    }*/

    /*public void testgetUnnasignedEventType() throws IOException {
        RedisServer redisServer = new RedisServer();
        redisServer.start();


        RedisClient redisClient = RedisClient.create("redis://localhost:6379/");
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        RedisCommands<String,String> syncCommands = connection.sync();
        syncCommands.zadd("Unnasigned",1,"111");
        connection.close();
        redisClient.shutdown();

        Lettuce lettuce = new Lettuce("localhost");

        assertTrue(lettuce.hasUnnasignedEvents());
        String result = lettuce.getUnnasignedEventType();
        System.out.println("RESULT: "+result);
        assertEquals("111",result);

        lettuce.Close();

        redisServer.stop();
    }

     */

    /*public void testhasEventTypeBeenPickedUpForRellocation() throws IOException {
        RedisServer redisServer = new RedisServer();
        redisServer.start();


        RedisClient redisClient = RedisClient.create("redis://localhost:6379/");
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        RedisCommands<String,String> syncCommands = connection.sync();
        syncCommands.zadd("RellocationStack",14,"111");
        connection.close();
        redisClient.shutdown();

        Lettuce lettuce = new Lettuce("localhost");

        assertTrue(lettuce.hasEventTypeBeenPickedUpForRellocation("111"));
        assertFalse(lettuce.hasEventTypeBeenPickedUpForRellocation("311"));

        lettuce.Close();

        redisServer.stop();
    }

    public void testpushEventTypeForRellocation() throws IOException {
        RedisServer redisServer = new RedisServer();
        redisServer.start();


        Lettuce lettuce = new Lettuce("localhost");

        lettuce.pushEventTypeForRellocation("111",15.0);

        lettuce.pushEventTypeForRellocation("112",45.0);

        assertTrue(lettuce.hasEventTypeBeenPickedUpForRellocation("111"));
        assertFalse(lettuce.hasEventTypeBeenPickedUpForRellocation("311"));

        lettuce.Close();

        redisServer.stop();
    }*/

    public void testgetEventTypeReceivingWorker() throws IOException {
        RedisServer redisServer = new RedisServer();
        redisServer.start();


        RedisClient redisClient = RedisClient.create("redis://localhost:6379/");
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        RedisCommands<String,String> syncCommands = connection.sync();
        syncCommands.set("111:ReceveingWorkerId", "113");
        connection.close();
        redisClient.shutdown();

        Lettuce lettuce = new Lettuce("localhost");

        assertEquals("113",lettuce.getEventTypeReceiveingWorker("111"));

        lettuce.Close();

        redisServer.stop();
    }

    public void testsetEventTypeReceveingWorker() throws IOException {
        RedisServer redisServer = new RedisServer();
        redisServer.start();



        Lettuce lettuce = new Lettuce("localhost");

        lettuce.setEventTypeReceveingWorker("111","133");

        String workerid = lettuce.getEventTypeReceiveingWorker("111");

        assertEquals("133",workerid);

        lettuce.Close();

        redisServer.stop();
    }

    public void testremoveEventTypeReceveingWorker() throws IOException {
        RedisServer redisServer = new RedisServer();
        redisServer.start();



        Lettuce lettuce = new Lettuce("localhost");

        lettuce.setEventTypeReceveingWorker("111","133");
        lettuce.removeEventTypeReceveingWorker("111");

        lettuce.Close();


        RedisClient redisClient = RedisClient.create("redis://localhost:6379/");
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        RedisCommands<String,String> syncCommands = connection.sync();

        assertFalse(syncCommands.exists("111:ReceveingWorkerId")>0 );

        connection.close();
        redisClient.shutdown();





        redisServer.stop();
    }

    /*public void testpopEventTypeForAcceptingRellocation() throws IOException {
        RedisServer redisServer = new RedisServer();
        redisServer.start();

        //Test ok, but embbedded-redis does not have support for zpopmax

        Lettuce lettuce = new Lettuce("localhost");

        lettuce.pushEventTypeForRellocation("111",15.0);

        lettuce.pushEventTypeForRellocation("112",45.0);

        String uuidPoped = lettuce.popEventTypeForAcceptingRellocation();


        assertEquals("112",uuidPoped);

        lettuce.Close();

        redisServer.stop();
    }*/

}
