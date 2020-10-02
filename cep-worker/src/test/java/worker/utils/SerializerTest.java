package worker.utils;

import junit.framework.TestCase;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;

import java.io.IOException;

public class SerializerTest extends TestCase {

    public void testAvro() throws IOException, ClassNotFoundException {
        byte [] serial = Serializer.AvroSerialize(generateEvent());
        GenericData.Record received = Serializer.AvroDeserialize(serial,generateSchema());
        assertEquals(generateEvent(),received);

    }

    public static GenericData.Record generateEvent(){
        GenericData.Record event;
        event = new GenericData.Record(generateSchema());
        event.put("carId",7);
        event.put("carType","Van");
        return event;
    }

    public static GenericData.Record generateEventTruck(){
        GenericData.Record event;
        event = new GenericData.Record(generateSchema());
        event.put("carId",6);
        event.put("carType","Truck");
        return event;
    }

    public static Schema generateSchema(){
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

    public static String avroschema(){
        return "{\"type\":\"record\",\"name\":\"MyAvroEvent\",\"fields\":"+
                "[{\"name\":\"carId\",\"type\":\"int\"},"+
                 "{\"name\":\"carType\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}}]" +
                "}";
    }
}
