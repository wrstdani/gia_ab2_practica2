import es.urjc.grafo.ABII.Algorithms.Algorithm;
import es.urjc.grafo.ABII.Algorithms.Algorithm2;
import org.junit.jupiter.api.Test;


public class Algorithm2Test {

    Algorithm algorithm = new Algorithm2();

    @Test
    public void testAlgorithm2() {
        AlgorithmGeneralTest.generalTest(
                "src/main/resources/instances",
                algorithm,
                60
        );
    }
}
