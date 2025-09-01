import es.urjc.grafo.ABII.Algorithms.Algorithm;
import es.urjc.grafo.ABII.Algorithms.Algorithm1;
import org.junit.jupiter.api.Test;


public class Algorithm1Test {

    Algorithm algorithm = new Algorithm1();

    @Test
    public void testAlgorithm1() {
        AlgorithmGeneralTest.generalTest(
                "src/main/resources/instances",
                algorithm,
                60
        );
    }
}
