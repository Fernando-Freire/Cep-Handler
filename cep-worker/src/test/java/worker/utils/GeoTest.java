package worker.utils;

import junit.framework.TestCase;


public class GeoTest extends TestCase {

    public void testDistance()  {

        double latitude1 = -22.007667;
        double latitude2 = -22.005499;
        double longitude1 = -47.892564;
        double longitude2 = -47.894796;

        //expected result 333.52 kilometers
        double distance = Geo.distance(latitude1,longitude1,latitude2,longitude2);

        System.out.print(distance);

        assertEquals(0.3331043127233582,distance);
        //assertTrue(Geo.distance(latitude1,longitude1,latitude2,longitude2)>0);

    }
}
