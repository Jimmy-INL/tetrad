package edu.cmu.tetrad.graph;

import edu.cmu.tetrad.util.TetradSerializable;

import java.util.ArrayList;
import java.util.List;

/**
 * Apr 13, 2017 3:56:46 PM
 *
 * @author Chirayu (Kong) Wongchokprasitti, PhD
 */
public class EdgeTypeProbability implements TetradSerializable {

    private static final long serialVersionUID = 23L;
    private EdgeType edgeType;
    private List<Edge.Property> properties = new ArrayList<>();
    private double probability;

    public EdgeTypeProbability() {

    }

    public EdgeTypeProbability(EdgeType edgeType, List<Edge.Property> properties, double probability) {
        this.edgeType = edgeType;
        this.properties = properties;
        this.probability = probability;
    }

    public EdgeTypeProbability(EdgeType edgeType, double probability) {
        this.edgeType = edgeType;
        this.probability = probability;
    }

    public EdgeType getEdgeType() {
        return this.edgeType;
    }

    public void setEdgeType(EdgeType edgeType) {
        this.edgeType = edgeType;
    }

    public void addProperty(Edge.Property property) {
        if (!properties.contains(property)) {
            properties.add(property);
        }
    }

    public void removeProperty(Edge.Property property) {
        properties.remove(property);
    }

    public ArrayList<Edge.Property> getProperties() {
        return new ArrayList<>(this.properties);
    }

    public double getProbability() {
        return this.probability;
    }

    public void setProbability(double probability) {
        this.probability = probability;
    }

    public enum EdgeType {
        nil, ta, at, ca, ac, cc, aa, tt
    }

}
