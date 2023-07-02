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

import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.graph.NodeType;
import edu.cmu.tetrad.graph.NodeVariableType;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;

/**
 * <p>
 * Represents a discrete variable as a range of integer-valued categories 0, 1, ..., m - 1, where m is the number of
 * categories for the variable. These integer-valued categories may be associated with categories that may be explicitly
 * set. Categories that are not explicitly set take the are set to DataUtils.defaultCategory(i) for category i.
 * <p>
 * Instances of this class may currently be used only to represent nominal discrete variables. Support for ordinal
 * discrete variables may be added in the future.
 * <p>
 * Like other variable classes, DiscreteVariable implements the Node interface. The purpose of this is to allow
 * variables to serve as nodes in graphs.
 *
 * <p>
 * The index value used to indicate missing data is -99.
 *
 * @author josephramsey
 */
public final class DiscreteVariable extends AbstractVariable {

    static final long serialVersionUID = 23L;

    /**
     * This is the index in the data which represents missing data internally for this variable.
     */
    public static final int MISSING_VALUE = -99;

    /**
     * The string displayed for missing values.
     */
    private static final String MISSING_VALUE_STRING = "*";

    /**
     * The "bulletin board" of lists of categories. Nominal variables should use these where possible to avoid ordering
     * their categories in novel ways. Since this "bulletin board" must be reconstructed each time Tetrad restarts, this
     * field must be transient.
     */
    private static List<LinkedList<String>> STORED_CATEGORY_LISTS;

    /**
     * The list of categories for the variable. Since the order must be reestablished every time Tetrad is started, this
     * field must be transient. Within each Tetrad session, it must be guaranteed that any particular list of categories
     * occurs in at most on permutation.
     */
    private transient List<String> categories;

    /**
     * A copy of the category list is stored here for when the discrete variable is deserialized. On deserialization,
     * STORED_CATEGORY_LISTS will be constructed if it hasn't been already, and this list will be looked up on the
     * "bulletin board" to see if a permutation of it already exists.
     *
     * @serial
     */
    private List<String> categoriesCopy = new ArrayList<>();

    /**
     * The discreteVariableType of discrete variable this is.
     *
     * @serial
     */
    private DiscreteVariableType discreteVariableType = DiscreteVariableType.NOMINAL;

    /**
     * True iff the category categories for this variable should be displayed; false if the integer indices of
     * categories should be displayed.
     *
     * @serial
     */
    private boolean categoryNamesDisplayed = true;

    /**
     * The node discreteVariableType.
     *
     * @serial
     */
    private NodeType nodeType = NodeType.MEASURED;

    /**
     * Node variable type (domain, interventional status, interventional value, ...) of this node variable
     */
    private NodeVariableType nodeVariableType = NodeVariableType.DOMAIN;

    /**
     * True iff new variables should be allowed to be constructed to replace this one, accommodating new categories.
     */
    private boolean accommodateNewCategories = true;

    /**
     * The x coordinate of the center of the node.
     *
     * @serial
     */
    private int centerX = -1;

    /**
     * The y coordinate of the center of the node.
     *
     * @serial
     */
    private int centerY = -1;

    /**
     * Fires property change events.
     */
    private transient PropertyChangeSupport pcs;

    private Map<String, Object> attributes = new HashMap<>();

    //=========================CONSTRUCTORS=============================//

    /**
     * Builds a discrete variable with the given name and an empty list of categories. Use this constructor if a
     * variable is needed to represent just a list of integer categories with no categories associated with the
     * categories.
     */
    public DiscreteVariable(String name) {
        super(name);
    }

    /**
     * Builds a qualitative variable with the given name and number of categories. The categories have the form
     * 'category'.
     */
    public DiscreteVariable(String name, int numCategories) {
        super(name);
        setCategories(numCategories);
        setCategoryNamesDisplayed(false);
    }

    /**
     * Builds a qualitative variable with the given name and array of possible categories.
     *
     * @param name       The name of the variable.
     * @param categories A String[] array of categories, where the categories[i] is the category for index i.
     */
    public DiscreteVariable(String name, List<String> categories) {
        super(name);
        setCategories(categories.toArray(new String[0]));
        setCategoryNamesDisplayed(true);
    }

    /**
     * Copy constructor.
     */
    public DiscreteVariable(DiscreteVariable variable) {
        super(variable.getName());
        this.categoriesCopy = DiscreteVariable.getStoredCategoryList(variable.getCategories());
        this.accommodateNewCategories = variable.accommodateNewCategories;
        this.attributes = new HashMap<>(variable.attributes);
        this.categories = new ArrayList<>(variable.categories);
        this.discreteVariableType = variable.discreteVariableType;
        this.centerX = variable.centerX;
        this.centerY = variable.centerY;
        this.nodeType = variable.nodeType;
        this.nodeVariableType = variable.nodeVariableType;
        setCategoryNamesDisplayed(true);
    }

    /**
     * Generates a simple exemplar of this class to test serialization.
     */
    public static DiscreteVariable serializableInstance() {
        return new DiscreteVariable("X");
    }

    //=============================PUBLIC METHODS========================//

    /**
     * @return the index of the given String category, or -1 if the category is not a category for this variable.
     */
    public int getIndex(String category) {
        return getCategories().indexOf(category);
    }

    /**
     * @return the number of possible categories for this variable. If categories are associated, this is just the
     * number of string categories. If no categories are associated, this is the maximum integer in the column.
     */
    public int getNumCategories() {
        return getCategories().size();
    }

    /**
     * @return the missing value marker as an Integer.
     */
    public Object getMissingValueMarker() {
        return DiscreteVariable.MISSING_VALUE;
    }

    /**
     * @return the variable category specified by the given category.
     */
    public String getCategory(int category) {
        if (category == DiscreteVariable.MISSING_VALUE) {
            return DiscreteVariable.MISSING_VALUE_STRING;
        } else {
            return getCategories().get(category);
        }
    }

    /**
     * @return a copy of the array containing the categories for this variable. The string at index i is the category
     * for index i.
     */
    public List<String> getCategories() {
        if (this.categories == null) {
            this.categories = Collections.unmodifiableList(DiscreteVariable.getStoredCategoryList(this.categoriesCopy));
        }
        return this.categories;
    }

    /**
     * @param category a category to be checked
     * @return true if the given category is legal.
     */
    public boolean checkValue(int category) {
        boolean inRange = (category >= 0) && (category < getNumCategories());
        boolean isMissing = (category == DiscreteVariable.MISSING_VALUE);
        return inRange || isMissing;
    }

    /**
     * Determines whether the given value is the missing value marker.
     *
     * @param value the value to test; should be an Integer or a String.
     */
    public boolean isMissingValue(Object value) {
        if (value instanceof Integer) {
            Integer ivalue = (Integer) value;
            return ivalue == DiscreteVariable.MISSING_VALUE;
        } else if (value instanceof String) {
            return DiscreteVariable.MISSING_VALUE_STRING.equals(value);
        }

        return false;
    }

    /**
     * @return true iff categories for this variable should be displayed.
     */
    public boolean isCategoryNamesDisplayed() {
        return this.categoryNamesDisplayed;
    }

    /**
     * Sets whether categories for this variable should be displayed.
     */
    public void setCategoryNamesDisplayed(
            boolean categoryNamesDisplayed) {
        this.categoryNamesDisplayed = categoryNamesDisplayed;
    }

    public int hashCode() {
        return this.getName().hashCode();
    }

    /**
     * @return true iff the given object is a discrete variable with the same number of categories and the same
     * categories.
     */
    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof DiscreteVariable)) return false;
        if (!getName().equals(((Node) o).getName())) return false;

        DiscreteVariable variable = (DiscreteVariable) o;

        if (!(getNumCategories() == variable.getNumCategories())) {
            return false;
        }

        for (int i = 0; i < getNumCategories(); i++) {
            if (!(getCategory(i).equals(variable.getCategory(i)))) {
                return false;
            }
        }

        return getNodeType() == variable.getNodeType();
    }

    public NodeType getNodeType() {
        return this.nodeType;
    }

    public void setNodeType(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    public boolean isAccommodateNewCategories() {
        return this.accommodateNewCategories;
    }

    /**
     * @return the x coordinate of the center of the node.
     */
    public int getCenterX() {
        return this.centerX;
    }

    /**
     * Sets the x coordinate of the center of this node.
     */
    public void setCenterX(int centerX) {
        this.centerX = centerX;
    }

    /**
     * @return the y coordinate of the center of the node.
     */
    public int getCenterY() {
        return this.centerY;
    }

    /**
     * Sets the y coordinate of the center of this node.
     */
    public void setCenterY(int centerY) {
        this.centerY = centerY;
    }

    /**
     * Sets the (x, y) coordinates of the center of this node.
     */
    public void setCenter(int centerX, int centerY) {
        this.centerX = centerX;
        this.centerY = centerY;
    }

    /**
     * Adds a property change listener.
     */
    public void addPropertyChangeListener(PropertyChangeListener l) {
        getPcs().addPropertyChangeListener(l);
    }

    /**
     * @return the name of the variable followed by its list of categories.
     */
    public String toString() {
        return getName();
    }

    public Node like(String name) {
        DiscreteVariable variable = new DiscreteVariable(name);
        variable.setNodeType(getNodeType());
        return variable;
    }

//    public final String toStringFull() {
//        StringBuilder buf = new StringBuilder();
//
//        buf.append(getNode());
//        buf.append(" <");
//
//        for (int i = 0; i < getNumCategories(); i++) {
//            buf.append(getCategory(i));
//
//            if (i < getNumCategories() - 1) {
//                buf.append(", ");
//            }
//        }
//
//        buf.append(">");
//        return buf.toString();
//    }
    //==============================PRIVATE METHODS=======================//

    /**
     * Sets the category of the category at the given index.
     *
     * @throws IllegalArgumentException if the list of categories is longer than 100. Usually this happens only for
     *                                  index columns in data sets, in which a different type of variable that doesn't
     *                                  do all of the complicated things discrete variables do should be used.
     */
    private void setCategories(String[] categories) {
        for (String category : categories) {
            if (category == null) {
                throw new NullPointerException();
            }
        }

        List<String> categoryList = Arrays.asList(categories);

        if (new HashSet<>(categoryList).size() != categoryList.size()) {
            throw new IllegalArgumentException("Duplicate category.");
        }

        this.categoriesCopy = Collections.unmodifiableList(DiscreteVariable.getStoredCategoryList(categoryList));
    }

    /**
     * Sets the category of the category at the given index.
     */
    private void setCategories(int numCategories) {
        String[] categories = new String[numCategories];

        for (int i = 0; i < numCategories; i++) {
            categories[i] = DataUtils.defaultCategory(i);
        }

        setCategories(categories);
    }

    private PropertyChangeSupport getPcs() {
        if (this.pcs == null) {
            this.pcs = new PropertyChangeSupport(this);
        }

        return this.pcs;
    }

    private static List<String> getStoredCategoryList(
            List<String> categoryList) {
        if (categoryList == null) {
            throw new NullPointerException();
        }

        Set<String> categorySet = new HashSet<>(categoryList);

        if (DiscreteVariable.STORED_CATEGORY_LISTS == null) {
            DiscreteVariable.STORED_CATEGORY_LISTS = new ArrayList<>();
        }

        for (LinkedList<String> list : DiscreteVariable.STORED_CATEGORY_LISTS) {
            if (categorySet.equals(new HashSet<>(list))) {
                return list;
            }
        }

        LinkedList<String> newList = new LinkedList<>(categoryList);
        DiscreteVariable.STORED_CATEGORY_LISTS.add(newList);
        return newList;
    }

    /**
     * Adds semantic checks to the default deserialization method. This method must have the standard signature for a
     * readObject method, and the body of the method must begin with "s.defaultReadObject();". Other than that, any
     * semantic checks can be specified and do not need to stay the same from version to version. A readObject method of
     * this form may be added to any class, even if Tetrad sessions were previously saved out using a version of the
     * class that didn't include it. (That's what the "s.defaultReadObject();" is for. See J. Bloch, Effective Java, for
     * help.
     */
    private void readObject(ObjectInputStream s)
            throws IOException, ClassNotFoundException {
        s.defaultReadObject();

        if (this.categoriesCopy == null) {
            throw new NullPointerException();
        }

        if (this.discreteVariableType == null) {
            throw new NullPointerException();
        }

        if (this.nodeType == null) {
            throw new NullPointerException();
        }
    }

    @Override
    public NodeVariableType getNodeVariableType() {
        return this.nodeVariableType;
    }

    @Override
    public void setNodeVariableType(NodeVariableType nodeVariableType) {
        this.nodeVariableType = nodeVariableType;
    }

    @Override
    public Map<String, Object> getAllAttributes() {
        return this.attributes;
    }

    @Override
    public Object getAttribute(String key) {
        return this.attributes.get(key);
    }

    @Override
    public void removeAttribute(String key) {
        this.attributes.remove(key);
    }

    @Override
    public void addAttribute(String key, Object value) {
        this.attributes.put(key, value);
    }

}
