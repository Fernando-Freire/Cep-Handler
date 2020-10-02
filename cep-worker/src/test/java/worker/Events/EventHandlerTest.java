package worker.Events;

import com.github.fridujo.rabbitmq.mock.MockConnectionFactory;
import com.rabbitmq.client.*;
import junit.framework.TestCase;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.reflect.AvroSchema;
import worker.Connections.RabbitmqSender;
import worker.utils.Serializer;
import worker.utils.SerializerTest;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class EventHandlerTest extends TestCase{
    EventHandler eventHandler;

    public void testHandle() throws IOException, TimeoutException {
        ConnectionFactory factory = new MockConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        String queueName = channel.queueDeclare().getQueue();

        RabbitmqSender sender  = new RabbitmqSender(channel);
        eventHandler = new EventHandler();
        String query1 = "SELECT * FROM MyAvroEvent7 as VanLocation WHERE carType = \"Van\" ";
        String query2 = "SELECT carType,carId FROM MyAvroEvent7 as Location WHERE carId = 7 ";
        String fields = "carId int, carType String";

        eventHandler.addInputStream("MyAvroEvent7",SerializerTest.generateSchema());
        eventHandler.addCheckExpression("2","VanLocation",query1,sender);
        eventHandler.addCheckExpression("3","Location",query2, sender);

        channel.queueBind(queueName, "EXCHANGE", "1");

        eventHandler.handle(SerializerTest.generateEvent(),"MyAvroEvent7");

        eventHandler.handle(SerializerTest.generateEventTruck(),"MyAvroEvent7");

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {
                assertEquals(SerializerTest.generateEvent(), Serializer.AvroDeserialize(body,SerializerTest.generateSchema()));
            }
        };
        channel.basicConsume(queueName, true, consumer);
        eventHandler.deleteCheckExpression("3");
        eventHandler.deleteCheckExpression("2");
        eventHandler.deleteInputStream("MyAvroEvent7");
        channel.close();
        connection.close();
        System.gc();
    }

    public void testBusEventHandle() throws IOException, TimeoutException {
        ConnectionFactory factory = new MockConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        String queueName = channel.queueDeclare().getQueue();

        RabbitmqSender sender  = new RabbitmqSender(channel);
        eventHandler = new EventHandler();
        String query1 = "SELECT * FROM bus234 WHERE cl > 0 ";

        eventHandler.addInputStream("bus234",generateBusEventSchema());
        eventHandler.addCheckExpression("B2","SaintMichel",query1,sender);

        channel.queueBind(queueName, "EXCHANGE", "1");

        eventHandler.handle(genBusEvt1(),"bus234");

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {
                assertEquals(SerializerTest.generateEvent(), Serializer.AvroDeserialize(body,SerializerTest.generateSchema()));
            }
        };
        channel.basicConsume(queueName, true, consumer);
        eventHandler.deleteCheckExpression("B2");
        eventHandler.deleteInputStream("bus234");
        channel.close();
        connection.close();
        System.gc();
    }

    public void testAggregation() throws IOException, TimeoutException {
        ConnectionFactory factory = new MockConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        String queueName = channel.queueDeclare().getQueue();

        RabbitmqSender sender  = new RabbitmqSender(channel);
        eventHandler = new EventHandler();
        String query = "SELECT average from MyAvroEvent#length(4)#uni(carId)";

        eventHandler.addInputStream("MyAvroEvent",SerializerTest.generateSchema());
        eventHandler.addCheckExpression("2","media",query,sender);

        channel.queueBind(queueName, "EXCHANGE", "1");

        eventHandler.handle(SerializerTest.generateEvent(),"MyAvroEvent");
        eventHandler.handle(SerializerTest.generateEventTruck(),"MyAvroEvent");
        eventHandler.handle(SerializerTest.generateEvent(),"MyAvroEvent");
        eventHandler.handle(SerializerTest.generateEventTruck(),"MyAvroEvent");

        Schema schema = eventHandler.getSchema("2");
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {

                assertEquals(schema.getFields(),Serializer.AvroDeserialize(body,schema).getSchema().getFields());
            }
        };
        channel.basicConsume(queueName, true, consumer);
        eventHandler.deleteCheckExpression("2");
        eventHandler.deleteInputStream("MyAvroEvent");
        channel.close();
        connection.close();
        System.gc();
    }

    public void testJoin() throws IOException, TimeoutException {
        ConnectionFactory factory = new MockConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        String queueName = channel.queueDeclare().getQueue();

        RabbitmqSender sender  = new RabbitmqSender(channel);
        EventHandler eventHandler = new EventHandler();
        String query = "SELECT m.py as pyy, m.px as pxx from MyAvroEvent3#length(1) as m,bus2345#length(1) as b WHERE b.py < m.py+0.0222 AND b.py > m.py-0.0222 AND b.px < m.px+0.02 AND b.px > m.px-0.02";
        String query2 = "SELECT m.py as pyy, m.px as pxx, distance(m.py, m.px, b.py, b.px) as d from MyAvroEvent3#length(1) as m,bus2345#length(1) as b"; //WHERE distance(m.py, b.py, m.px, b.px) < 1000";
        eventHandler.addInputStream("MyAvroEvent3", generateTimestampSchema());
        eventHandler.addInputStream("bus2345",generateBusEventSchema());
        eventHandler.addCheckExpression("3","media",query2,sender);


        channel.queueBind(queueName, "EXCHANGE", "1");

        eventHandler.handle(genBusEvt1(),"bus2345");
        eventHandler.handle(generateTimestampEvent1(),"MyAvroEvent3");
        eventHandler.handle(generateTimestampEvent2(),"MyAvroEvent3");
        eventHandler.handle(generateTimestampEvent3(),"MyAvroEvent3");

        Schema schema = eventHandler.getSchema("2");
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {

                assertEquals(schema.getFields(),Serializer.AvroDeserialize(body,schema).getSchema().getFields());
            }
        };
        channel.basicConsume(queueName, true, consumer);
        eventHandler.deleteCheckExpression("3");
        eventHandler.deleteInputStream("MyAvroEvent3");
        eventHandler.deleteInputStream("bus2345");
        channel.close();
        connection.close();
        System.gc();
    }

    public void testHandleWithTimeStamp() throws IOException, TimeoutException {
        ConnectionFactory factory = new MockConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        String queueName = channel.queueDeclare().getQueue();

        RabbitmqSender sender  = new RabbitmqSender(channel);
        EventHandler eventHandler = new EventHandler();

        String query1 = "SELECT * FROM MyAvroEvent#ext_timed(timestamp, 1) as VanLocation WHERE carType = \"Truck\" ";
        String query2 = "SELECT carType,carId FROM MyAvroEvent#ext_timed(timestamp, 1) as Location WHERE carId = 7 ";

        eventHandler.addInputStream("MyAvroEvent",generateTimestampSchema());
        eventHandler.addCheckExpression("2","VanLocation",query1,sender);
        eventHandler.addCheckExpression("3","Location",query2, sender);

        channel.queueBind(queueName, "EXCHANGE", "1");

        eventHandler.handle(generateTimestampEvent1(),"MyAvroEvent");
        eventHandler.handle(generateTimestampEvent2(),"MyAvroEvent");
        eventHandler.handle(generateTimestampEvent3(),"MyAvroEvent");

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {
                assertEquals(SerializerTest.generateEvent(), Serializer.AvroDeserialize(body,SerializerTest.generateSchema()));
            }
        };
        channel.basicConsume(queueName, true, consumer);
        eventHandler.deleteCheckExpression("3");
        eventHandler.deleteCheckExpression("2");
        eventHandler.deleteInputStream("MyAvroEvent");
        channel.close();
        connection.close();
        System.gc();
    }

    public void testAggregationWithTimestamp() throws IOException, TimeoutException {
        ConnectionFactory factory = new MockConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        String queueName = channel.queueDeclare().getQueue();

        RabbitmqSender sender  = new RabbitmqSender(channel);
        EventHandler eventHandler = new EventHandler();
        String query = "SELECT average from MyAvroEvent#length(4)#uni(carId)";

        eventHandler.addInputStream("MyAvroEvent",generateTimestampSchema());
        eventHandler.addCheckExpression("2","avgwithT",query,sender);

        channel.queueBind(queueName, "EXCHANGE", "1");

        eventHandler.handle(generateTimestampEvent1(),"MyAvroEvent");
        eventHandler.handle(generateTimestampEvent2(),"MyAvroEvent");
        eventHandler.handle(generateTimestampEvent3(),"MyAvroEvent");

        Schema schema = eventHandler.getSchema("2");


        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {

                assertEquals(schema.getFields(),Serializer.AvroDeserialize(body,schema).getSchema().getFields());
            }
        };
        channel.basicConsume(queueName, true, consumer);
        eventHandler.deleteCheckExpression("2");
        eventHandler.deleteInputStream("MyAvroEvent");
        channel.close();
        connection.close();
        System.gc();
    }

    public void testTimeAggregationWithTimestamp() throws IOException, TimeoutException {
        ConnectionFactory factory = new MockConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        String queueName = channel.queueDeclare().getQueue();

        RabbitmqSender sender  = new RabbitmqSender(channel);
        EventHandler eventHandler = new EventHandler();
        //String query = "SELECT avg(carId) as avgcar, timestamp from MyAvroEvent#ext_timed(timestamp,1)";

        String query = "SELECT c from 1bus WHERE px > 0 AND px < 10000000000000 AND py > 0 AND py < 10000000000000";
        String query2 = "SELECT c as cO from MyAvroEvent#ext_timed(timestamp,15)";

        eventHandler.addInputStream("MyAvroEvent",generateBusEventSchema());
        eventHandler.addCheckExpression("2","avgwithT",query2,sender);

        channel.queueBind(queueName, "EXCHANGE", "1");

        eventHandler.handle(generateTimestampEvent1(),"MyAvroEvent");
        eventHandler.handle(generateTimestampEvent2(),"MyAvroEvent");
        eventHandler.handle(generateTimestampEvent3(),"MyAvroEvent");

        Schema schema = eventHandler.getSchema("2");



        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {

                assertEquals(schema.getFields(),Serializer.AvroDeserialize(body,schema).getSchema().getFields());
            }
        };
        channel.basicConsume(queueName, true, consumer);
        eventHandler.deleteCheckExpression("2");
        eventHandler.deleteInputStream("MyAvroEvent");
        channel.close();
        connection.close();


        // In this example, it uses the "timestamp" field to track the time of the aggregation.
        // Since the first two events come with just .4 second of diference,
        // the second trigger agregates the average of the carId field.
        // However, the third event comes 6 seconds later,
        // falling outside the 1 second window.
        System.gc();
    }

    public void testJointwo() throws IOException, TimeoutException {
        ConnectionFactory factory = new MockConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        String queueName = channel.queueDeclare().getQueue();

        RabbitmqSender sender  = new RabbitmqSender(channel);
        EventHandler eventHandler = new EventHandler();
        String query = "SELECT cl,p, py, px, ta, timestamp, distance(py, px, prev(py), prev(px))*1000*60*60/(ta - prev(ta)) as v FROM bus234#length(2) WHERE p = 718 and distance(py, prev(py), px, prev(px))*1000/(ta - prev(ta)) > 10";
        //String query2 = "SELECT distance(py, prev(py), px, prev(px))*1000/(ta - prev(ta)) as v FROM bus234#length(10)";

        eventHandler.addInputStream("bus234",generateBusEventSchema());
        eventHandler.addCheckExpression("2","prevbus234",query,sender);
        //eventHandler.addCheckExpression("3","vint",query,sender);


        channel.queueBind(queueName, "EXCHANGE", "1");

        eventHandler.handle(genBusEvt1(),"bus234");
        eventHandler.handle(genBusEvt2(),"bus234");


        Schema schema = eventHandler.getSchema("2");
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {

                assertEquals(schema.getFields(),Serializer.AvroDeserialize(body,schema).getSchema().getFields());
            }
        };
        channel.basicConsume(queueName, true, consumer);
        //eventHandler.deleteCheckExpression("3");
        eventHandler.deleteCheckExpression("2");
        eventHandler.deleteInputStream("bus234");
        channel.close();
        connection.close();
        System.gc();
    }

    public void testbusbunching() throws IOException, TimeoutException {
        ConnectionFactory factory = new MockConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        String queueName = channel.queueDeclare().getQueue();

        RabbitmqSender sender  = new RabbitmqSender(channel);
        EventHandler eventHandler = new EventHandler();
        String query = "SELECT v.p as p, v.cl as cl,v.py as py, v.px as px, v.ta as ta, v.timestamp as timestamp from bus234(p != 718)#ext_timed(ta,60) as v, bus234(p = 718)#ext_timed(ta,60) as b WHERE distance(v.py, v.px, b.py, b.px) < 0.5";
        //String query2 = "SELECT distance(py, prev(py), px, prev(px))*1000/(ta - prev(ta)) as v FROM bus234#length(10)";

        eventHandler.addInputStream("bus234",generateBusEventSchema());
        eventHandler.addCheckExpression("2","BB234",query,sender);
        //eventHandler.addCheckExpression("3","vint",query,sender);


        channel.queueBind(queueName, "EXCHANGE", "1");

        eventHandler.handle(genBusEvt1(),"bus234");
        eventHandler.handle(genBusEvt2(),"bus234");
        eventHandler.handle(genBusEvt3(),"bus234");
        eventHandler.handle(genBusEvt4(),"bus234");


        Schema schema = eventHandler.getSchema("2");
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {

                assertEquals(schema.getFields(),Serializer.AvroDeserialize(body,schema).getSchema().getFields());
            }
        };
        channel.basicConsume(queueName, true, consumer);

        eventHandler.deleteCheckExpression("2");
        eventHandler.deleteInputStream("bus234");
        channel.close();
        connection.close();
        System.gc();
    }

    public void testexperimentinstantseep() throws IOException, TimeoutException {
        ConnectionFactory factory = new MockConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        String queueName = channel.queueDeclare().getQueue();

        RabbitmqSender sender1  = new RabbitmqSender(channel);
        RabbitmqSender sender2  = new RabbitmqSender(channel);
        RabbitmqSender sender3  = new RabbitmqSender(channel);
        EventHandler eventHandler = new EventHandler();

        String queryas1 = "SELECT cl, ta, timestamp, distance(py, px, prev(py), prev(px))*1000*60*60/(ta - prev(ta)) as v FROM bus234#length(2) group by p having  distance(py, px, prev(py), prev(px))*1000*60*60/(ta - prev(ta)) < 10 ";
        String queryas2 = null;
        String queryas3 = null;

        eventHandler.addInputStream("bus234",generateBusEventSchema());

        eventHandler.addCheckExpression("2","avgspeed234",queryas1,sender1);
        //eventHandler.addInputStream("2","avgspeedInput234",eventHandler.getSchema("1"));


        Schema schema = eventHandler.getSchema("2");
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {

                //eventHandler.handle(Serializer.AvroDeserialize(body,schema),"avgspeedInput234");
            }
        };


        //eventHandler.addCheckExpression("3","prevbus234",queryas2,sender2);
        //eventHandler.addInputStream("4","bus234",generateBusEventSchema());

        //eventHandler.addCheckExpression("5","prevbus234",queryas3,sender3);
        //eventHandler.addCheckExpression("3","vint",query,sender);


        channel.queueBind(queueName, "EXCHANGE", "1");

        eventHandler.handle(genBusEvt1(),"bus234");
        eventHandler.handle(genBusEvt2(),"bus234");
        eventHandler.handle(genBusEvt3(),"bus234");
        eventHandler.handle(genBusEvt4(),"bus234");


        assertTrue(true);
        channel.basicConsume(queueName, true, consumer);
        //eventHandler.deleteCheckExpression("3");
        eventHandler.deleteCheckExpression("2");
        eventHandler.deleteInputStream("bus234");
        channel.close();
        connection.close();
        System.gc();
    }

    public void testexperimentmergedstream() throws IOException, TimeoutException {
        ConnectionFactory factory = new MockConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        String queueName = channel.queueDeclare().getQueue();

        RabbitmqSender sender1  = new RabbitmqSender(channel);
        RabbitmqSender sender2  = new RabbitmqSender(channel);
        RabbitmqSender sender3  = new RabbitmqSender(channel);
        EventHandler eventHandler = new EventHandler();

        String queryas1 = "SELECT cl, ta, timestamp, distance(py, px, prev(py), prev(px))*1000*60*60/(ta - prev(ta)) as v FROM bus234#length(2) group by p having  distance(py, px, prev(py), prev(px))*1000*60*60/(ta - prev(ta)) < 10 ";
        String queryas2 = "select * from bus";


        eventHandler.addInputStream("bus",generateSchema("bus"));

        eventHandler.addCheckExpression("2","query",queryas2,sender1);
        //eventHandler.addInsertClause("4",queryas3);
        //eventHandler.addInputStream("2","avgspeedInput234",eventHandler.getSchema("1"));


        Schema schema = eventHandler.getSchema("2");
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {

                //eventHandler.handle(Serializer.AvroDeserialize(body,schema),"avgspeedInput234");
            }
        };


        //eventHandler.addCheckExpression("3","prevbus234",queryas2,sender2);
        //eventHandler.addInputStream("4","bus234",generateBusEventSchema());

        //eventHandler.addCheckExpression("5","prevbus234",queryas3,sender3);
        //eventHandler.addCheckExpression("3","vint",query,sender);


        channel.queueBind(queueName, "EXCHANGE", "1");

        eventHandler.handle(genBusEvt1(),"bus");
        eventHandler.handle(genBusEvt2(),"bus");
        eventHandler.handle(genBusEvt11(),"bus");
        eventHandler.handle(genBusEvt3(),"bus");
        eventHandler.handle(genBusEvt4(),"bus");


        assertTrue(true);
        channel.basicConsume(queueName, true, consumer);
        //eventHandler.deleteCheckExpression("3");
        eventHandler.deleteCheckExpression("2");
        //eventHandler.deleteInputStream("1");
        eventHandler.deleteInputStream("bus");
        channel.close();
        connection.close();
        System.gc();
    }

    public void testexperimentavgspeedwithtimestamp() throws IOException, TimeoutException {
        ConnectionFactory factory = new MockConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        String queueName = channel.queueDeclare().getQueue();

        RabbitmqSender sender1  = new RabbitmqSender(channel);
        EventHandler eventHandler = new EventHandler();

        String query = "Select cl, p, py, px, avg(p) as pa, ta, timestamp FROM bus234#ext_timed(timestamp,300) HAVING avg(px) < 100";

        eventHandler.addInputStream("bus234",generateBusEventSchema());

        eventHandler.addCheckExpression("2","avgspeed",query,sender1);
        //eventHandler.addInputStream("2","avgspeedInput234",eventHandler.getSchema("1"));


        Schema schema = eventHandler.getSchema("2");
        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope,
                                       AMQP.BasicProperties properties, byte[] body) throws IOException {

                //eventHandler.handle(Serializer.AvroDeserialize(body,schema),"avgspeedInput234");
            }
        };


        //eventHandler.addCheckExpression("3","prevbus234",queryas2,sender2);
        //eventHandler.addInputStream("4","bus234",generateBusEventSchema());

        //eventHandler.addCheckExpression("5","prevbus234",queryas3,sender3);
        //eventHandler.addCheckExpression("3","vint",query,sender);


        channel.queueBind(queueName, "EXCHANGE", "1");

        eventHandler.handle(genBusEvt1(),"bus234");
        eventHandler.handle(genBusEvt2(),"bus234");
        eventHandler.handle(genBusEvt3(),"bus234");
        eventHandler.handle(genBusEvt4(),"bus234");


        assertTrue(true);
        channel.basicConsume(queueName, true, consumer);
        //eventHandler.deleteCheckExpression("3");
        eventHandler.deleteCheckExpression("2");
        eventHandler.deleteInputStream("bus234");
        channel.close();
        connection.close();
        System.gc();
    }





    static Schema generateTimestampSchema(){
        Schema.Parser parser = new Schema.Parser();
        return parser.parse("{" +
                "  \"type\" : \"record\"," +
                "  \"name\" : \"MyAvroEvent\"," +
                "  \"fields\" : [ "+
                "{ \"name\" : \"carId\", \"type\" : \"int\" },"+
                "{ \"name\" : \"carType\", \"type\" : { \"type\" : \"string\", \"avro.java.string\" : \"String\" } }," +
                "{ \"name\" : \"timestamp\", \"type\" : { \"type\" : \"long\", \"logicalType\" : \"timestamp-millis\" } }," +
                "{ \"name\" : \"py\",  \"type\" : \"double\" }," +
                "{ \"name\" : \"px\",  \"type\" : \"double\" }" +
                " ]" +
                "}");
    }

    static GenericData.Record generateTimestampEvent1(){
        GenericData.Record event;
        event = new GenericData.Record(generateTimestampSchema());
        event.put("carId",6);
        event.put("carType","Truck");
        event.put("py", -23.560763249999997);
        event.put("px", -46.64364025);
        long date = Long.valueOf("1358080561000");
        event.put("timestamp",date);
        return event;
    }

    static GenericData.Record generateTimestampEvent2(){
        GenericData.Record event;
        event = new GenericData.Record(generateTimestampSchema());
        event.put("carId",7);
        event.put("carType","Truck");
        event.put("py", -23.63450125);
        event.put("px", -46.737007500000004);
        long date = Long.valueOf("1358080561000");
        event.put("timestamp",date+400);
        return event;
    }

    static GenericData.Record generateTimestampEvent3(){
        GenericData.Record event;
        event = new GenericData.Record(generateTimestampSchema());
        event.put("carId",8);
        event.put("carType","Truck");
        event.put("py", 12.0298483);
        event.put("px", 38.07228472);
        long date = Long.valueOf("1358080561000");
        event.put("timestamp",date+6000);
        return event;
    }

    static GenericData.Record genBusEvt1(){
        GenericData.Record event;
        event = new GenericData.Record(generateBusEventSchema());
        event.put("c","Truck");
        event.put("cl",234);
        event.put("timestamp",System.currentTimeMillis());
        event.put("ta", 1572865165000L);
        event.put("sl", 1);
        event.put("lt0", "letreiro 0");
        event.put("lt1", "letreitro 1");
        event.put("qv", 8);
        event.put("p", 718);
        event.put("a", true);
        event.put("py", -23.6495375);
        event.put("px", -46.707279);
        return event;
    }

    static GenericData.Record genBusEvt2(){
        GenericData.Record event;
        event = new GenericData.Record(generateBusEventSchema());
        event.put("c","Truck");
        event.put("cl",234);
        event.put("timestamp",System.currentTimeMillis());
        event.put("ta",1572865185000L);
        event.put("sl", 1);
        event.put("lt0", "letreiro 0");
        event.put("lt1", "letreitro 1");
        event.put("qv", 8);
        event.put("p", 718);
        event.put("a", true);
        event.put("py", -23.65830425);
        event.put("px", -46.76489425);
        return event;
    }

    static GenericData.Record genBusEvt3(){
        GenericData.Record event;
        event = new GenericData.Record(generateBusEventSchema());
        event.put("c","Truck");
        event.put("cl",234);
        event.put("timestamp",System.currentTimeMillis());
        event.put("ta", 1572865190000L);
        event.put("sl", 1);
        event.put("lt0", "letreiro 0");
        event.put("lt1", "letreitro 1");
        event.put("qv", 8);
        event.put("p", 432);
        event.put("a", true);
        event.put("py", -23.560763249999997);
        event.put("px", -46.64364025);
        return event;
    }

    static GenericData.Record genBusEvt4(){
        GenericData.Record event;
        event = new GenericData.Record(generateBusEventSchema());
        event.put("c","Truck");
        event.put("cl",234);
        event.put("timestamp",System.currentTimeMillis());
        event.put("ta", 1572865196000L);
        event.put("sl", 1);
        event.put("lt0", "letreiro 0");
        event.put("lt1", "letreitro 1");
        event.put("qv", 8);
        event.put("p", 432);
        event.put("a", true);
        event.put("py", -23.65830500);
        event.put("px", -46.76489500);
        return event;
    }

    static GenericData.Record genBusEvt11(){
        GenericData.Record event;
        event = new GenericData.Record(generateBusEventSchema());
        event.put("c","Truck");
        event.put("cl",235);
        event.put("timestamp",System.currentTimeMillis());
        event.put("ta", 1572865196000L);
        event.put("sl", 1);
        event.put("lt0", "letreiro 0");
        event.put("lt1", "letreitro 1");
        event.put("qv", 8);
        event.put("p", 4324);
        event.put("a", true);
        event.put("py", -23.65830500);
        event.put("px", -46.76489500);
        return event;
    }

    static Schema generateBusEventSchema(){
        Schema.Parser parser = new Schema.Parser();
        return parser.parse(AvroSchema);
    }

    static Schema generateBusEventSchema2(){
        Schema.Parser parser = new Schema.Parser();
        return parser.parse(AvroSchema2);
    }

    static Schema generateSchema(String name){
        Schema.Parser parser = new Schema.Parser();
        return parser.parse(AvroSchema(name));
    }

    static String AvroSchema = "{" +
            "  \"type\" : \"record\"," +
            "  \"name\" : \"bus234\"," +
            "  \"fields\" : [ " +
            "{ \"name\" : \"c\",   \"type\" : { \"type\" : \"string\", \"avro.java.string\" : \"String\" } }," +
            "{ \"name\" : \"cl\",  \"type\" : \"int\" }," +
            "{ \"name\" : \"sl\",  \"type\" : \"int\" }," +
            "{ \"name\" : \"lt0\", \"type\" : { \"type\" : \"string\", \"avro.java.string\" : \"String\" } }," +
            "{ \"name\" : \"lt1\", \"type\" : { \"type\" : \"string\", \"avro.java.string\" : \"String\" } }," +
            "{ \"name\" : \"qv\",  \"type\" : \"int\" }," +
            "{ \"name\" : \"p\",   \"type\" : \"int\" }," +
            "{ \"name\" : \"a\",   \"type\" : \"boolean\" }," +
            "{ \"name\" : \"ta\",  \"type\" : { \"type\" : \"long\", \"logicalType\" : \"timestamp-millis\" } }," +
            "{ \"name\" : \"py\",  \"type\" : \"double\" }," +
            "{ \"name\" : \"px\",  \"type\" : \"double\" }," +
            "{ \"name\" : \"timestamp\",  \"type\" : { \"type\" : \"long\", \"logicalType\" : \"timestamp-millis\" } }" +
            "] }";

    static String AvroSchema(String name){ return "{" +
            "  \"type\" : \"record\"," +
            "  \"name\" : \""+name+"\"," +
            "  \"fields\" : [ " +
            "{ \"name\" : \"c\",   \"type\" : { \"type\" : \"string\", \"avro.java.string\" : \"String\" } }," +
            "{ \"name\" : \"cl\",  \"type\" : \"int\" }," +
            "{ \"name\" : \"sl\",  \"type\" : \"int\" }," +
            "{ \"name\" : \"lt0\", \"type\" : { \"type\" : \"string\", \"avro.java.string\" : \"String\" } }," +
            "{ \"name\" : \"lt1\", \"type\" : { \"type\" : \"string\", \"avro.java.string\" : \"String\" } }," +
            "{ \"name\" : \"qv\",  \"type\" : \"int\" }," +
            "{ \"name\" : \"p\",   \"type\" : \"int\" }," +
            "{ \"name\" : \"a\",   \"type\" : \"boolean\" }," +
            "{ \"name\" : \"ta\",  \"type\" : { \"type\" : \"long\", \"logicalType\" : \"timestamp-millis\" } }," +
            "{ \"name\" : \"py\",  \"type\" : \"double\" }," +
            "{ \"name\" : \"px\",  \"type\" : \"double\" }," +
            "{ \"name\" : \"timestamp\",  \"type\" : { \"type\" : \"long\", \"logicalType\" : \"timestamp-millis\" } }" +
            "] }"; }

    static String AvroSchema2 = "{" +
            "  \"type\" : \"record\"," +
            "  \"name\" : \"bus235\"," +
            "  \"fields\" : [ " +
            "{ \"name\" : \"c\",   \"type\" : { \"type\" : \"string\", \"avro.java.string\" : \"String\" } }," +
            "{ \"name\" : \"cl\",  \"type\" : \"int\" }," +
            "{ \"name\" : \"sl\",  \"type\" : \"int\" }," +
            "{ \"name\" : \"lt0\", \"type\" : { \"type\" : \"string\", \"avro.java.string\" : \"String\" } }," +
            "{ \"name\" : \"lt1\", \"type\" : { \"type\" : \"string\", \"avro.java.string\" : \"String\" } }," +
            "{ \"name\" : \"qv\",  \"type\" : \"int\" }," +
            "{ \"name\" : \"p\",   \"type\" : \"int\" }," +
            "{ \"name\" : \"a\",   \"type\" : \"boolean\" }," +
            "{ \"name\" : \"ta\",  \"type\" : { \"type\" : \"long\", \"logicalType\" : \"timestamp-millis\" } }," +
            "{ \"name\" : \"py\",  \"type\" : \"double\" }," +
            "{ \"name\" : \"px\",  \"type\" : \"double\" }," +
            "{ \"name\" : \"timestamp\",  \"type\" : { \"type\" : \"long\", \"logicalType\" : \"timestamp-millis\" } }" +
            "] }";

}
