package customer.ai2code.service.variable.impl;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import com.sap.cds.CdsData;

import cds.gen.mainservice.CDSViews;
import cds.gen.mainservice.CDSViews_;
import cds.gen.mainservice.DraftAdministrativeData;

public class CDSViewsMock implements CDSViews {

    private String viewName;
    private String viewDesc;

    public CDSViewsMock(String viewName, String viewDesc) {
        this.viewName = viewName;
        this.viewDesc = viewDesc;
    }

    @Override
    public String getViewName() {
        return viewName;
    }

    @Override
    public void setViewName(String viewName) {
        this.viewName = viewName;
    }

    @Override
    public String getViewDesc() {
        return viewDesc;
    }

    @Override
    public void setViewDesc(String viewDesc) {
        this.viewDesc = viewDesc;
    }

    @Override
    public boolean containsPath(String arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'containsPath'");
    }

    @Override
    public <T extends CdsData> T forRemoval(boolean arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'forRemoval'");
    }

    @Override
    public Object get(Object arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'get'");
    }

    @Override
    public <T> T getMetadata(String arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getMetadata'");
    }

    @Override
    public <T> T getPath(String arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPath'");
    }

    @Override
    public <T> T getPathOrDefault(String arg0, T arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getPathOrDefault'");
    }

    @Override
    public boolean isForRemoval() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isForRemoval'");
    }

    @Override
    public <T> T putMetadata(String arg0, T arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'putMetadata'");
    }

    @Override
    public <T> T putPath(String arg0, T arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'putPath'");
    }

    @Override
    public <T> T putPathIfAbsent(String arg0, T arg1) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'putPathIfAbsent'");
    }

    @Override
    public <T> T removePath(String arg0) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'removePath'");
    }

    @Override
    public int size() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'size'");
    }

    @Override
    public boolean isEmpty() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'isEmpty'");
    }

    @Override
    public boolean containsKey(Object key) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'containsKey'");
    }

    @Override
    public boolean containsValue(Object value) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'containsValue'");
    }

    @Override
    public Object put(String key, Object value) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'put'");
    }

    @Override
    public Object remove(Object key) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'remove'");
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> m) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'putAll'");
    }

    @Override
    public void clear() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'clear'");
    }

    @Override
    public Set<String> keySet() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'keySet'");
    }

    @Override
    public Collection<Object> values() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'values'");
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'entrySet'");
    }

    @Override
    public String toJson() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'toJson'");
    }

    @Override
    public String getId() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getId'");
    }

    @Override
    public void setId(String id) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setId'");
    }

    @Override
    public Instant getCreatedAt() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getCreatedAt'");
    }

    @Override
    public void setCreatedAt(Instant createdAt) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setCreatedAt'");
    }

    @Override
    public String getCreatedBy() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getCreatedBy'");
    }

    @Override
    public void setCreatedBy(String createdBy) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setCreatedBy'");
    }

    @Override
    public Instant getModifiedAt() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getModifiedAt'");
    }

    @Override
    public void setModifiedAt(Instant modifiedAt) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setModifiedAt'");
    }

    @Override
    public String getModifiedBy() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getModifiedBy'");
    }

    @Override
    public void setModifiedBy(String modifiedBy) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setModifiedBy'");
    }

    @Override
    public String getViewCategory() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getViewCategory'");
    }

    @Override
    public void setViewCategory(String viewCategory) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setViewCategory'");
    }

    @Override
    public Boolean getIsActive() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getIsActive'");
    }

    @Override
    public void setIsActive(Boolean isActive) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setIsActive'");
    }

    @Override
    public Boolean getIsActiveEntity() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getIsActiveEntity'");
    }

    @Override
    public void setIsActiveEntity(Boolean isActiveEntity) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setIsActiveEntity'");
    }

    @Override
    public Boolean getHasActiveEntity() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getHasActiveEntity'");
    }

    @Override
    public void setHasActiveEntity(Boolean hasActiveEntity) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setHasActiveEntity'");
    }

    @Override
    public Boolean getHasDraftEntity() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getHasDraftEntity'");
    }

    @Override
    public void setHasDraftEntity(Boolean hasDraftEntity) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setHasDraftEntity'");
    }

    @Override
    public DraftAdministrativeData getDraftAdministrativeData() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getDraftAdministrativeData'");
    }

    @Override
    public void setDraftAdministrativeData(Map<String, ?> draftAdministrativeData) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setDraftAdministrativeData'");
    }

    @Override
    public String getDraftAdministrativeDataDraftUUID() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getDraftAdministrativeDataDraftUUID'");
    }

    @Override
    public void setDraftAdministrativeDataDraftUUID(String draftAdministrativeDataDraftUUID) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setDraftAdministrativeDataDraftUUID'");
    }

    @Override
    public CDSViews getSiblingEntity() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getSiblingEntity'");
    }

    @Override
    public void setSiblingEntity(Map<String, ?> siblingEntity) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setSiblingEntity'");
    }

    @Override
    public CDSViews_ ref() {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'ref'");
    }

    // 其他字段如果用不到，可以不实现
}
