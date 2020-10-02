package worker.LoadBalancers;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import junit.framework.TestCase;
import redis.embedded.RedisServer;
import worker.DatabaseAccess.Lettuce;


import java.io.IOException;


public class InputSimilarityTest extends TestCase {


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

        InputSimilarity lb = new InputSimilarity(null);

        Lettuce store = new Lettuce("localhost");

        lb.addTypeToRank("111",store);
        lb.addTypeToRank("222",store);
        lb.addTypeToRank("333",store);


        String TypeIdToBeRellocated = lb.findTypetoRellocate(store);

        assertEquals("111",TypeIdToBeRellocated);

        lb.deleteTypeofRank("111",store);

        TypeIdToBeRellocated = lb.findTypetoRellocate(store);

        assertEquals("222",TypeIdToBeRellocated);

        store.Close();

        redisServer.stop();
    }

}
