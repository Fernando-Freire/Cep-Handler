package worker.Events;

import junit.framework.TestCase;


import java.util.HashMap;


public class QueryAnalysisTest extends TestCase {

    public void testGetStateType(){
        String query = "SELECT average from MyAvroEvent#length(4)#uni(carId)";
        assertEquals("length", QueryAnalysis.getStateType(query,"MyAvroEvent"));
    }

    public void testGetStateNumber(){
        String query = "SELECT average from MyAvroEvent#length(4)#uni(carId)";
        assertEquals("4",QueryAnalysis.getStateNumber(query,"MyAvroEvent"));
    }

    public void testAnalyzeLength(){
        HashMap<String,Integer> counters = new HashMap<>();
        String query = "SELECT average from MyAvroEvent#length(4)#uni(carId)";
        String stateType = QueryAnalysis.getStateType(query,"MyAvroEvent");
        String stateNumber = QueryAnalysis.getStateNumber(query,"MyAvroEvent");
        QueryAnalysis.analyzeQueryLengh(stateType,stateNumber,"123",counters);
        assertTrue(counters.containsKey("123"));
        assertTrue(counters.containsValue(4));
    }

    public void testAnalyzeTime(){
        String query = "SELECT average from MyAvroEvent#ext_time(timestamp,4)#uni(carId)";
        String stateType = QueryAnalysis.getStateType(query,"MyAvroEvent");
        String stateNumber = QueryAnalysis.getStateNumber(query,"MyAvroEvent");
        assertEquals(4000,QueryAnalysis.analyzeQueryTime(stateType,stateNumber));
    }

    public void testHasDataWindow(){
        String query = "SELECT average from MyAvroEvent#ext_time(timestamp,4)#uni(carId)";
        String query2 = "SELECT carType,carId FROM MyAvroEvent as Location WHERE carId = 7 ";
        assertTrue(QueryAnalysis.hasDataWindow(query,"MyAvroEvent"));
        assertFalse(QueryAnalysis.hasDataWindow(query2,"MyAvroEvent"));
    }

}
