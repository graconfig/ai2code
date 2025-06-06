package customer.ai2code.service.impl;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.sap.cds.Result;

import cds.gen.mainservice.ContextNodes;
import customer.ai2code.service.ContextService;

@Service
public class ContextServiceImpl implements ContextService{

    @Override
    public List<Map<String, Object>> buildContextAsHierarchy(List<ContextNodes> contextNodes) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'buildContextAsHierarchy'");
    }

    @Override
    public String getContextFullPath(String subPathPrefix, String subPath) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getContextFullPath'");
    }

    @Override
    public Result upsertContext(String taskId, String contextPath, String contextValue) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'upsertContext'");
    }

    @Override
    public ContextNodes getContextNode(String contextNodeId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getContextNode'");
    }
    
}
