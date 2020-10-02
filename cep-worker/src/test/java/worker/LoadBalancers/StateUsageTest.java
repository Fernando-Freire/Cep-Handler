package worker.LoadBalancers;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import junit.framework.TestCase;
import redis.embedded.RedisServer;
import worker.DatabaseAccess.Lettuce;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class StateUsageTest extends TestCase {

    public void testfindEventTypetoRellocate() throws IOException {

        RedisServer redisServer = new RedisServer();
        redisServer.start();

        RedisClient redisClient = RedisClient.create("redis://localhost:6379/");
        StatefulRedisConnection<String, String> connection = redisClient.connect();
        RedisCommands<String,String> syncCommands = connection.sync();
        syncCommands.sadd("111:Inputs","001","002","003");
        syncCommands.sadd("222:Inputs","001","002","004");
        syncCommands.sadd("333:Inputs","001","004");
        connection.close();
        redisClient.shutdown();

        StateUsage st = new StateUsage(null);

        st.StateRank.put("111",0);
        st.StateRank.put("222",0);
        st.StateRank.put("333",0);

        Lettuce store = new Lettuce("localhost");

        Map<String,Integer> inputseventspassed = new HashMap<>();
        inputseventspassed.put("001",8);
        inputseventspassed.put("002",20);
        inputseventspassed.put("003",90);
        inputseventspassed.put("004",5);

        st.OrderTypesByInputEvents(inputseventspassed,store);

        store.Close();

        assertEquals("111",st.findEventTypetoRellocate());



        redisServer.stop();
    }
}
