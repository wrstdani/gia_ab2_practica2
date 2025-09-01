package es.urjc.grafo.ABII.Algorithms;

import es.urjc.grafo.ABII.Model.Instance;
import es.urjc.grafo.ABII.Model.Solution;

public interface Algorithm {
    public Solution run(Instance instance);
}
