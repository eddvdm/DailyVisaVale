package com.wirapuru.dailyvisavale;

/**
 * Created by IntelliJ IDEA.
 * User: eduardo
 * Date: 2/21/12
 * Time: 12:43 PM
 * To change this template use File | Settings | File Templates.
 */
abstract public class MetaStatAbstract {
    protected static DailyVisaVale.CardData base_data = null;
    protected static Object metastat_data = null;
    protected Boolean force_regenerate = false;

    public MetaStatAbstract(DailyVisaVale.CardData base_data_obj) {
        if (getBaseData() == null || !isBaseDataSameAs(base_data_obj)) {
            setBaseData(base_data_obj);
            setForceRegenerate(true);
        }
    }
    
    public MetaStatAbstract() throws Exception {
        if (getBaseData() == null) 
            throw new Exception(this.getClass().getName()+" was instantiated without any base data!");
        if (getMetastatData() == null)
            setForceRegenerate(true);
    }

    public MetaStatAbstract(Boolean force_regenerate) throws Exception {
        if (getBaseData() == null)
            throw new Exception(this.getClass().getName()+" was instantiated without any base data!");

        setForceRegenerate(true);
    }

    protected Object get() {
        if (isForceRegenerate()) {
            generateMetastatData();
        }
        return getMetastatData();
    }
    
    final void generateMetastatData() {
        processBaseData();
        setForceRegenerate(false);
    }

    abstract void processBaseData();

    protected Boolean isBaseDataSameAs(Object obj) {
        // todo check: this probably is a non-fatal bug, must compare content not the obj
        return obj.equals(getBaseData());
    }

    public String getAsString() {
        return String.valueOf(get());
    }

    public Integer getAsInteger() {
        return Integer.parseInt(getAsString());
    }

    public Float getAsFloat() {
        return Float.parseFloat(getAsString());
    }

    public DailyVisaVale.CardData getBaseData() {
        return base_data;
    }

    protected void setBaseData(DailyVisaVale.CardData base_data_obj) {
        base_data = base_data_obj;
    }

    protected Object getMetastatData() {
        return metastat_data;
    }

    protected void setMetastatData(Object metastat_data_obj) {
        metastat_data = metastat_data_obj;
    }

    protected Boolean isForceRegenerate() {
        return force_regenerate;
    }

    protected void setForceRegenerate(Boolean force_regenerate) {
        this.force_regenerate = force_regenerate;
    }
}