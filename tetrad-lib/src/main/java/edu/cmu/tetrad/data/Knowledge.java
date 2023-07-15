///////////////////////////////////////////////////////////////////////////////
// For information as to what this class does, see the Javadoc, below.       //
// Copyright (C) 1998, 1999, 2000, 2001, 2002, 2003, 2004, 2005, 2006,       //
// 2007, 2008, 2009, 2010, 2014, 2015, 2022 by Peter Spirtes, Richard        //
// Scheines, Joseph Ramsey, and Clark Glymour.                               //
//                                                                           //
// This program is free software; you can redistribute it and/or modify      //
// it under the terms of the GNU General Public License as published by      //
// the Free Software Foundation; either version 2 of the License, or         //
// (at your option) any later version.                                       //
//                                                                           //
// This program is distributed in the hope that it will be useful,           //
// but WITHOUT ANY WARRANTY; without even the implied warranty of            //
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the             //
// GNU General Public License for more details.                              //
//                                                                           //
// You should have received a copy of the GNU General Public License         //
// along with this program; if not, write to the Free Software               //
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA //
///////////////////////////////////////////////////////////////////////////////
package edu.cmu.tetrad.data;

import edu.cmu.tetrad.graph.*;
import edu.cmu.tetrad.util.TetradSerializable;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.rmi.MarshalledObject;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Stores information about required and forbidden edges and common causes for use in algorithm. This information can be
 * set edge by edge or else globally via temporal tiers. When setting temporal tiers, all edges from later tiers to
 * earlier tiers are forbidden.
 * <p>
 * For this class, all variable names are referenced by name only. This is because the same Knowledge object is intended
 * to plug into different graphs with MyNodes that possibly have the same names. Thus, if the Knowledge object forbids
 * the edge X --&gt; Y, then it forbids any edge which connects a MyNode named "X" to a MyNode named "Y", even if the
 * underlying MyNodes themselves named "X" and "Y", respectively, are not the same.
 * <p>
 * In place of variable names, wildcard expressions containing the wildcard '*' may be substituted. These will be
 * matched to as many myNodes as possible. The '*' wildcard matches any string of consecutive characters up until the
 * following character is encountered. Thus, "X*a" will match "X123a" and "X45a".
 *
 * @author josephramsey
 * @author Kevin V. Bui (kvb2@pitt.edu)
 */
public final class Knowledge implements TetradSerializable {

    private static final long serialVersionUID = 23L;

    private static final Pattern VARNAME_PATTERN = Pattern.compile("[A-Za-z0-9:_\\-.]+");
    private static final Pattern SPEC_PATTERN = Pattern.compile("[A-Za-z0-9:-_,\\-.*]+");
    private static final Pattern COMMAN_DELIM = Pattern.compile(",");
    private final Set<Node> variables;
    private final List<OrderedPair<Set<Node>>> forbiddenRulesSpecs;
    private final List<OrderedPair<Set<Node>>> requiredRulesSpecs;
    private final List<Set<Node>> tierSpecs;
    // Legacy.
    private final List<KnowledgeGroup> knowledgeGroups;
    private final Map<KnowledgeGroup, OrderedPair<Set<Node>>> knowledgeGroupRules;
    private boolean defaultToKnowledgeLayout;

    public Knowledge() {
        this.variables = new HashSet<>();
        this.forbiddenRulesSpecs = new ArrayList<>();
        this.requiredRulesSpecs = new ArrayList<>();
        this.tierSpecs = new ArrayList<>();
        this.knowledgeGroups = new LinkedList<>();
        this.knowledgeGroupRules = new HashMap<>();
    }

    public Knowledge(Collection<String> nodes) {
        this();

        nodes.forEach(node -> {
            if (checkVarName(node)) {
                this.variables.add(new GraphNode(node));
            } else {
                throw new IllegalArgumentException(String.format("Bad variable node %s.", node));
            }
        });
    }

    public Knowledge(Knowledge knowledge) {
        if (knowledge == null) {
            throw new IllegalArgumentException("Knowledge is null.");
        }

        try {
            Knowledge copy = new MarshalledObject<>(knowledge).get();

            this.defaultToKnowledgeLayout = copy.defaultToKnowledgeLayout;
            this.variables = copy.variables;
            this.forbiddenRulesSpecs = copy.forbiddenRulesSpecs;
            this.requiredRulesSpecs = copy.requiredRulesSpecs;
            this.tierSpecs = copy.tierSpecs;
            this.knowledgeGroups = copy.knowledgeGroups;
            this.knowledgeGroupRules = copy.knowledgeGroupRules;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generates a simple exemplar of this class to test serialization.
     */
    public static Knowledge serializableInstance() {
        return new Knowledge();
    }

    // Checking this spec can cause the drag and drop feature to fail.
    private boolean checkVarName(String name) {
        return true;
//        return Knowledge.VARNAME_PATTERN.matcher(name).matches();
    }

    // Checking this spec can cause the drag and drop feature to fail.
    private String checkSpec(String spec) {
//        Matcher matcher = Knowledge.SPEC_PATTERN.matcher(spec);
//        if (!matcher.matches()) {
//            throw new IllegalArgumentException(spec + ": Cpdag names can consist of alphabetic "
//                    + "characters plus :, _, -, and .. A wildcard '*' may be included to match a "
//                    + "string of such characters.");
//        }

        return spec.replace(".", "\\.");
    }

    private Set<Node> getExtent(String spec) {
        Set<Node> vars = new HashSet<>();

        if (spec.contains("*")) {
            split(spec).stream()
                    .map(e -> e.replace("*", ".*"))
                    .forEach(e -> {
                        Pattern cpdag = Pattern.compile(e);
                        this.variables.stream()
                                .filter(var -> cpdag.matcher(var.getName()).matches())
                                .collect(Collectors.toCollection(() -> vars));
                    });
        } else {
            if (this.variables.contains(new GraphNode(spec))) {
                vars.add(new GraphNode(spec));
            }
        }

        return vars;
    }

    private Set<String> split(String spec) {
        return Arrays.stream(Knowledge.COMMAN_DELIM.split(spec))
                .map(String::trim)
                .filter(e -> !e.isEmpty())
                .collect(Collectors.toSet());
    }

    private void ensureTiers(int tier) {
        for (int i = this.tierSpecs.size(); i <= tier; i++) {
            this.tierSpecs.add(new HashSet<>());

            for (int j = 0; j < i; j++) {
                this.forbiddenRulesSpecs.add(new OrderedPair<>(this.tierSpecs.get(i), this.tierSpecs.get(j)));
            }
        }
    }

    private OrderedPair<Set<Node>> getGroupRule(KnowledgeGroup group) {
        Set<Node> fromExtent = new HashSet<>();
        group.getFromVariables()
                .forEach(e -> fromExtent.addAll(getExtent(e)));

        Set<Node> toExtent = new HashSet<>();
        group.getToVariables()
                .forEach(e -> toExtent.addAll(getExtent(e)));

        return new OrderedPair<>(fromExtent, toExtent);
    }

    private Set<OrderedPair<Set<Node>>> forbiddenTierRules() {
        Set<OrderedPair<Set<Node>>> rules = new HashSet<>();

        for (int i = 0; i < this.tierSpecs.size(); i++) {
            if (isTierForbiddenWithin(i)) {
                rules.add(new OrderedPair<>(this.tierSpecs.get(i), this.tierSpecs.get(i)));
            }
        }

        for (int i = 0; i < this.tierSpecs.size(); i++) {
            if (isOnlyCanCauseNextTier(i)) {
                for (int j = i + 2; j < this.tierSpecs.size(); j++) {
                    rules.add(new OrderedPair<>(this.tierSpecs.get(i), this.tierSpecs.get(j)));
                }
            }
        }

        for (int i = 0; i < this.tierSpecs.size(); i++) {
            for (int j = i + 1; j < this.tierSpecs.size(); j++) {
                rules.add(new OrderedPair<>(this.tierSpecs.get(j), this.tierSpecs.get(i)));
            }
        }

        return rules;
    }

    /**
     * Adds the given variable or wildcard cpdag to the given tier. The tier is a non-negative integer.
     */
    public void addToTier(int tier, String spec) {
        if (tier < 0) {
            throw new IllegalArgumentException();
        }
        if (spec == null) {
            throw new NullPointerException();
        }

        addVariable(spec);
        spec = checkSpec(spec);
        ensureTiers(tier);

        getExtent(spec)
//                .filter(this::checkVarName)
                .forEach(e -> {
                    this.variables.add(e);
                    this.tierSpecs.get(tier).add(e);
                });
    }

    /**
     * Puts a variable into tier i if its name is xxx:ti for some xxx and some i.
     */
    public void addToTiersByVarNames(List<String> varNames) {
        if (!this.variables.containsAll(varNames)) {
            varNames.forEach(e -> {
                if (checkVarName(e)) {
                    this.variables.add(new GraphNode(e));
                } else {
                    throw new IllegalArgumentException(String.format("Bad variable node %s.", e));
                }
            });
        }

        varNames.forEach(e -> {
            int index = e.lastIndexOf(":t");
            if (index >= 0) {
                addToTier(Integer.parseInt(e.substring(index + 2)), e);
            }
        });
    }

    /**
     * Adds a knowledge group. Legacy method, replaced by setForbidden, setRequired with cpdags. Needed for the
     * interface.
     */
    public void addKnowledgeGroup(KnowledgeGroup group) {
        this.knowledgeGroups.add(group);

        OrderedPair<Set<Node>> o = getGroupRule(group);
        this.knowledgeGroupRules.put(group, o);

        if (group.getType() == KnowledgeGroup.FORBIDDEN) {
            this.forbiddenRulesSpecs.add(o);
        } else if (group.getType() == KnowledgeGroup.REQUIRED) {
            this.requiredRulesSpecs.add(o);
        }
    }

    public void addVariable(String varName) {
        this.variables.add(new GraphNode(varName));
    }

    /**
     * Removes explicit knowledge and tier information.
     */
    public void clear() {
        this.variables.clear();
        this.forbiddenRulesSpecs.clear();
        this.requiredRulesSpecs.clear();
        this.tierSpecs.clear();
    }

    /**
     * Iterator over the KnowledgeEdge's representing forbidden edges.
     */
    public Iterator<KnowledgeEdge> forbiddenEdgesIterator() {
        Set<KnowledgeEdge> edges = new HashSet<>();

        this.forbiddenRulesSpecs.forEach(o -> o.getFirst().forEach(s1 -> o.getSecond().forEach(s2 -> {
            if (!s1.equals(s2)) {
                edges.add(new KnowledgeEdge(s1.getName(), s2.getName()));
            }
        })));

        return edges.iterator();
    }

    /**
     * @return a shallow copy of the list of group rules.
     */
    public List<KnowledgeGroup> getKnowledgeGroups() {
        return new ArrayList<>(this.knowledgeGroups);
    }

    /**
     * Get a list of variables.
     *
     * @return a copy of the list of variable, in alphabetical order.
     */
    public List<String> getVariables() {
        return getStrings(variables);

//        return this.variables.stream()
//                .sorted()
//                .collect(Collectors.toList());
    }

    private List<String> getStrings(Collection<Node> vars) {
        List<String> strings = new ArrayList<>();
        for (Node node : vars) {
            strings.add(node.getName());
        }
        Collections.sort(strings);
        return strings;
    }

    /**
     * @return the list of edges not in any tier.
     */
    public List<String> getVariablesNotInTiers() {
        List<Node> notInTier = new ArrayList<>(this.variables);

        for (Set<Node> tier : this.tierSpecs) {
            notInTier.removeAll(tier);
        }

        return getStrings(notInTier);
    }

    /**
     * @param tier the index of the desired tier
     * @return a copy of this tier
     */
    public List<String> getTier(int tier) {
        ensureTiers(tier);

        try {
            return getStrings(this.tierSpecs.get(tier).stream()
                    .sorted()
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            throw new RuntimeException("Unexpected knowledge configuration.", e);
        }
    }

    /**
     * @return the number of temporal tiers
     */
    public int getNumTiers() {
        return this.tierSpecs.size();
    }

    public boolean isDefaultToKnowledgeLayout() {
        return this.defaultToKnowledgeLayout;
    }

    public void setDefaultToKnowledgeLayout(boolean defaultToKnowledgeLayout) {
        this.defaultToKnowledgeLayout = defaultToKnowledgeLayout;
    }

    private boolean isForbiddenByRules(String var1, String var2) {
        GraphNode _var1 = new GraphNode(var1);
        GraphNode _var2 = new GraphNode(var2);

        return this.forbiddenRulesSpecs.stream()
                .anyMatch(rule -> !_var1.equals(_var2)
                        && rule.getFirst().contains(_var1)
                        && rule.getSecond().contains(_var2));
    }

    /**
     * Determines whether the edge var1 --&gt; var2 is forbidden.
     */
    public boolean isForbidden(String var1, String var2) {
        if (isRequired(var1, var2)) {
            return false;
        }

        return isForbiddenByRules(var1, var2) || isForbiddenByTiers(var1, var2);
    }

    /**
     * Legacy.
     */
    public boolean isForbiddenByGroups(String var1, String var2) {
        Set<OrderedPair<Set<Node>>> s = this.knowledgeGroups.stream()
                .filter(e -> e.getType() == KnowledgeGroup.FORBIDDEN)
                .map(this::getGroupRule)
                .collect(Collectors.toSet());

        return s.stream()
                .anyMatch(rule -> rule.getFirst().contains(var1)
                        && rule.getSecond().contains(var2));
    }

    /**
     * Determines whether the edge var1 --&gt; var2 is forbidden by the temporal tiers.
     */
    public boolean isForbiddenByTiers(String var1, String var2) {
        return forbiddenTierRules().stream()
                .anyMatch(rule -> rule.getFirst().contains(var1)
                        && rule.getSecond().contains(var2));
    }

    /**
     * Determines whether the edge var1 --&gt; var2 is required.
     */
    public boolean isRequired(String var1, String var2) {
        GraphNode _var1 = new GraphNode(var1);
        GraphNode _var2 = new GraphNode(var2);

        return this.requiredRulesSpecs.stream()
                .anyMatch(rule -> !_var1.equals(_var2)
                        && rule.getFirst().contains(_var1)
                        && rule.getSecond().contains(_var2));
    }

    /**
     * Legacy.
     */
    public boolean isRequiredByGroups(String var1, String var2) {
        Set<OrderedPair<Set<Node>>> s = this.knowledgeGroups.stream()
                .filter(e -> e.getType() == KnowledgeGroup.REQUIRED)
                .map(this::getGroupRule)
                .collect(Collectors.toSet());

        return s.stream()
                .anyMatch(rule -> rule.getFirst().contains(var1)
                        && rule.getSecond().contains(var2));
    }

    /**
     * true if there is no background knowledge recorded.
     */
    public boolean isEmpty() {
        return this.forbiddenRulesSpecs.isEmpty()
                && this.requiredRulesSpecs.isEmpty()
                && this.tierSpecs.isEmpty();
    }

    /**
     * Checks whether it is the case that any variable is forbidden by any other variable within a given tier.
     */
    public boolean isTierForbiddenWithin(int tier) {
        ensureTiers(tier);

        Set<Node> varsInTier = this.tierSpecs.get(tier);
        if (varsInTier.isEmpty()) {
            return false;
        }

        return this.forbiddenRulesSpecs.contains(new OrderedPair<>(varsInTier, varsInTier));
    }

    public boolean isViolatedBy(Graph graph) {
        if (graph == null) {
            throw new NullPointerException("Sorry, a graph hasn't been provided.");
        }

        return graph.getEdges().stream()
                .filter(Edge::isDirected)
                .anyMatch(edge -> {
                    Node from = Edges.getDirectedEdgeTail(edge);
                    Node to = Edges.getDirectedEdgeHead(edge);

                    return isForbidden(from.getName(), to.getName());
                });
    }

    public boolean noEdgeRequired(String x, String y) {
        return !(isRequired(x, y) || isRequired(y, x));
    }

    /**
     * Removes the given variable by name or search string from all tiers.
     */
    public void removeFromTiers(String spec) {
        if (spec == null) {
            throw new NullPointerException();
        }

        spec = checkSpec(spec);
        getExtent(spec).forEach(s -> this.tierSpecs.forEach(tier -> tier.remove(s)));
    }

    /**
     * Removes the knowledge group at the given index.
     */
    public void removeKnowledgeGroup(int index) {
        OrderedPair<Set<Node>> old = this.knowledgeGroupRules.get(this.knowledgeGroups.get(index));

        this.forbiddenRulesSpecs.remove(old);
        this.requiredRulesSpecs.remove(old);

        this.knowledgeGroups.remove(index);
    }

    /**
     * Iterator over the KnowledgeEdge's representing required edges.
     */
    public Iterator<KnowledgeEdge> requiredEdgesIterator() {
        Set<KnowledgeEdge> edges = new HashSet<>();

        this.requiredRulesSpecs.forEach(o -> o.getFirst().forEach(s1 -> o.getSecond().forEach(s2 -> {
            if (!s1.equals(s2)) {
                edges.add(new KnowledgeEdge(s1.getName(), s2.getName()));
            }
        })));

        return edges.iterator();
    }

    /**
     * Marks the edge var1 --&gt; var2 as forbid.
     */
    public void setForbidden(String var1, String var2) {
        if (isForbidden(var1, var2)) return;

        addVariable(var1);
        addVariable(var2);

        var1 = checkSpec(var1);
        var2 = checkSpec(var2);

        Set<Node> f1 = getExtent(var1);
        Set<Node> f2 = getExtent(var2);

        this.forbiddenRulesSpecs.add(new OrderedPair<>(f1, f2));
    }

    /**
     * Marks the edge var1 --&gt; var2 as not forbid.
     */
    public void removeForbidden(String var1, String var2) {
        var1 = checkSpec(var1);
        var2 = checkSpec(var2);

        Set<Node> f1 = getExtent(var1);
        Set<Node> f2 = getExtent(var2);

        this.forbiddenRulesSpecs.remove(new OrderedPair<>(f1, f2));
    }

    /**
     * Marks the edge var1 --&gt; var2 as required.
     */
    public void setRequired(String var1, String var2) {
        addVariable(var1);
        addVariable(var2);

        var1 = checkSpec(var1);
        var2 = checkSpec(var2);

        Set<Node> f1 = getExtent(var1);
        Set<Node> f2 = getExtent(var2);

        f1.forEach(s -> {
            if (checkVarName(s.getName())) {
                this.variables.add(s);
            }
        });
        f2.forEach(s -> {
            if (checkVarName(s.getName())) {
                this.variables.add(s);
            }
        });

        this.requiredRulesSpecs.add(new OrderedPair<>(f1, f2));
    }

    /**
     * Marks the edge var1 --&gt; var2 as not required.
     */
    public void removeRequired(String var1, String var2) {
        var1 = checkSpec(var1);
        var2 = checkSpec(var2);

        Set<Node> f1 = getExtent(var1);
        Set<Node> f2 = getExtent(var2);

        this.requiredRulesSpecs.remove(new OrderedPair<>(f1, f2));
    }

    /**
     * Legacy, do not use.
     */
    public void setKnowledgeGroup(int index, KnowledgeGroup group) {
        OrderedPair<Set<Node>> o = getGroupRule(group);
        OrderedPair<Set<Node>> old = this.knowledgeGroupRules.get(this.knowledgeGroups.get(index));

        this.forbiddenRulesSpecs.remove(old);
        this.requiredRulesSpecs.remove(old);

        if (group.getType() == KnowledgeGroup.FORBIDDEN) {
            this.forbiddenRulesSpecs.add(o);
        } else if (group.getType() == KnowledgeGroup.REQUIRED) {
            this.requiredRulesSpecs.add(o);
        }

        this.knowledgeGroups.set(index, group);
    }

    /**
     * Sets the variable in a given tier to the specified list.
     */
    public void setTier(int tier, List<String> vars) {
        ensureTiers(tier);
        Set<Node> varsInTier = this.tierSpecs.get(tier);
        if (varsInTier != null) {
            varsInTier.clear();
        }

        vars.forEach(var -> addToTier(tier, var));
    }

    /**
     * Forbids any variable from being parent of any other variable within the given tier, or cancels this forbidding.
     */
    public void setTierForbiddenWithin(int tier, boolean forbidden) {
        ensureTiers(tier);
        Set<Node> varsInTier = this.tierSpecs.get(tier);

        if (forbidden) {
            this.forbiddenRulesSpecs.add(new OrderedPair<>(varsInTier, varsInTier));
        } else {
            this.forbiddenRulesSpecs.remove(new OrderedPair<>(varsInTier, varsInTier));
        }
    }

    /**
     * @return the largest indes of a tier in which every variable is forbidden by every other variable, or -1 if there
     * is not such tier.
     */
    public int getMaxTierForbiddenWithin() {
        for (int tier = this.tierSpecs.size(); tier >= 0; tier--) {
            if (isTierForbiddenWithin(tier)) {
                return tier;
            }
        }

        return -1;
    }

    /**
     * Makes a shallow copy.
     */
    public Knowledge copy() {
        return new Knowledge(this);
    }

    /**
     * Returns the index of the tier of node if it's in a tier, otherwise -1.
     */
    public int isInWhichTier(Node node) {
        for (int i = 0; i < this.tierSpecs.size(); i++) {
            Set<Node> tier = this.tierSpecs.get(i);

            for (Node myNode : tier) {
                if (myNode.equals(node)) {
                    return i;
                }
            }
        }

        return -1;
    } // added by DMalinsky for tsFCI on 4/20/16

    public List<KnowledgeEdge> getListOfRequiredEdges() {
        Set<KnowledgeEdge> edges = new HashSet<>();

        this.requiredRulesSpecs.forEach(e -> e.getFirst().forEach(e1 -> e.getSecond().forEach(e2 -> {
            if (!e1.equals(e2)) {
                edges.add(new KnowledgeEdge(e1.getName(), e2.getName()));
            }
        })));

        return new ArrayList<>(edges);
    }

    public List<KnowledgeEdge> getListOfExplicitlyRequiredEdges() {
        return getListOfRequiredEdges();
    }

    public List<KnowledgeEdge> getListOfForbiddenEdges() {
        Set<KnowledgeEdge> edges = new HashSet<>();

        this.forbiddenRulesSpecs.forEach(e -> e.getFirst().forEach(e1 -> e.getSecond().forEach(e2 -> {
            if (!e1.equals(e2)) {
                edges.add(new KnowledgeEdge(e1.getName(), e2.getName()));
            }
        })));

        return new ArrayList<>(edges);
    }

    public List<KnowledgeEdge> getListOfExplicitlyForbiddenEdges() {
        Set<OrderedPair<Set<Node>>> copy = new HashSet<>(this.forbiddenRulesSpecs);
        copy.removeAll(forbiddenTierRules());

        this.knowledgeGroups.forEach(e -> copy.remove(this.knowledgeGroupRules.get(e)));

        Set<KnowledgeEdge> edges = new HashSet<>();
        for (OrderedPair<Set<Node>> e : copy)
            e.getFirst().forEach(e1 -> e.getSecond().forEach(e2 -> edges.add(new KnowledgeEdge(e1.getName(), e2.getName()))));

        return new ArrayList<>(edges);
    }

    public boolean isOnlyCanCauseNextTier(int tier) {
        ensureTiers(tier);

        Set<Node> varsInTier = this.tierSpecs.get(tier);
        if (varsInTier.isEmpty()) {
            return false;
        }

        if (tier + 2 >= this.tierSpecs.size()) {
            return false;
        }

        // all successive tiers > tier + 2 must be forbidden
        for (int tierN = tier + 2; tierN < this.tierSpecs.size(); tierN++) {
            Set<Node> varsInTierN = this.tierSpecs.get(tierN);
            OrderedPair<Set<Node>> o = new OrderedPair<>(varsInTier, varsInTierN);

            if (!this.forbiddenRulesSpecs.contains(o)) {
                return false;
            }
        }

        return true;
    }

    public void setOnlyCanCauseNextTier(int tier, boolean onlyCausesNext) {
        ensureTiers(tier);

        Set<Node> varsInTier = this.tierSpecs.get(tier);

        for (int tierN = tier + 2; tierN < this.tierSpecs.size(); tierN++) {
            Set<Node> varsInTierN = this.tierSpecs.get(tierN);
            if (onlyCausesNext) {
                this.forbiddenRulesSpecs.add(new OrderedPair<>(varsInTier, varsInTierN));
            } else {
                this.forbiddenRulesSpecs.remove(new OrderedPair<>(varsInTier, varsInTierN));
            }
        }
    }

    /**
     * Computes a hashcode.
     */
    public int hashCode() {
        int hash = 37;
        hash += 17 * this.variables.hashCode() + 37;
        hash += 17 * this.forbiddenRulesSpecs.hashCode() + 37;
        hash += 17 * this.requiredRulesSpecs.hashCode() + 37;
        hash += 17 * this.tierSpecs.hashCode() + 37;
        return hash;
    }

    /**
     * Two Knowledge objects are equal just in case their forbidden and required edges are equal, and their tiers are
     * equal.
     */
    public boolean equals(Object o) {
        if (!(o instanceof Knowledge)) {
            return false;
        }
        Knowledge that = (Knowledge) o;

        return this.forbiddenRulesSpecs.equals(that.forbiddenRulesSpecs)
                && this.requiredRulesSpecs.equals(that.requiredRulesSpecs)
                && this.tierSpecs.equals(that.tierSpecs);
    }

    /**
     * @return the contents of this Knowledge object in String form.
     */
    public String toString() {
        try {
            CharArrayWriter out = new CharArrayWriter();
            DataWriter.saveKnowledge(this, out);
            return out.toString();
        } catch (IOException e) {
            throw new IllegalStateException("Could not render knowledge.");
        }
    }

}
