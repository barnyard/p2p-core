package com.bt.pi.core.conf;

public class ExampleBean {
    private String unannotated;
    private String annotated;
    private String annotatedButNoPropertyDefined;
    private int intProp;
    private long longProp;
    private boolean booleanProp;
    private int newIntProp;
    private double doubleProp;

    public void setUnannotated(String unannotated) {
        this.unannotated = unannotated;
    }

    public Object getUnrequiredProperty() {
        return null;
    }

    @Property(key = "propKey")
    public void setAnnotated(String annotated) {
        this.annotated = annotated;
    }

    @Property(key = "missing", defaultValue = "defaultValue")
    public void setAnnotatedButNoPropertyDefined(String annotatedButNoPropertyDefined) {
        this.annotatedButNoPropertyDefined = annotatedButNoPropertyDefined;
    }

    @Property(key = "intValue")
    public void setIntProp(int intProp) {
        this.intProp = intProp;
    }

    @Property(key = "longValue")
    public void setLongProp(long longProp) {
        this.longProp = longProp;
    }

    @Property(key = "booleanValue")
    public void setBooleanProp(boolean booleanProp) {
        this.booleanProp = booleanProp;
    }

    @Property(key = "missing", required = false)
    public void setUnrequired(String missing) {
        // no-op
    }

    @Property(key = "newIntValue", defaultValue = "100")
    public void setNewInt(int newInt) {
        newIntProp = newInt;
    }

    public int getNewIntProp() {
        return newIntProp;
    }

    @Property(key = "doubleValue")
    public void setDoubleProp(double doubleProp) {
        this.doubleProp = doubleProp;
    }

    public double getDoubleProp() {
        return this.doubleProp;
    }

    public String getUnannotated() {
        return unannotated;
    }

    public String getAnnotated() {
        return annotated;
    }

    public String getAnnotatedButNoPropertyDefined() {
        return annotatedButNoPropertyDefined;
    }

    public int getIntProp() {
        return intProp;
    }

    public long getLongProp() {
        return longProp;
    }

    public boolean getBooleanProp() {
        return booleanProp;
    }

    @Override
    public String toString() {
        return "ExampleBean [annotated=" + annotated + ", annotatedButNoPropertyDefined=" + annotatedButNoPropertyDefined + ", booleanProp=" + booleanProp + ", intProp=" + intProp + ", longProp=" + longProp + ", unannotated=" + unannotated + "]";
    }

}
