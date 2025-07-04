      
package customer.ai2code.service.impl.rag;

import cds.gen.mainservice.CDSViewFiles;
import cds.gen.mainservice.CDSViews; // 导入mainservice包下的实体
import cds.gen.mainservice.Viewfields;
import customer.ai2code.exception.BusinessException;
import customer.ai2code.service.impl.GenericCqnService;
import customer.ai2code.service.rag.CdsViewFileUploadService;
import customer.ai2code.util.FileParser;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;
import org.apache.commons.io.IOUtils;

// service/CdsViewFileUploadServiceImpl.java
@Service
public class CdsViewFileUploadServiceImpl implements CdsViewFileUploadService {
    private final GenericCqnService cqnService;
    private final FileParser parser;

    public CdsViewFileUploadServiceImpl(GenericCqnService cqnService, FileParser parser) {
        this.cqnService = cqnService;
        this.parser = parser;
    }

    @Override
    @Transactional
    public String uploadCDSViews(InputStream excel, String filename) {
        try {
            // 1. 读取完整文件内容
            byte[] content = IOUtils.toByteArray(excel);

            // 2. 保存文件到CDSViewFiles
            CDSViewFiles fileRecord = CDSViewFiles.create();
            fileRecord.setId(UUID.randomUUID().toString());
            // 
            if (filename != null ) {
                fileRecord.setFileName(filename);
            }else{
                fileRecord.setFileName("cds_view_" + UUID.randomUUID() + ".xlsx");
            }
            fileRecord.setSize(String.valueOf(content.length));
            fileRecord.setMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            fileRecord.setFileContent(new ByteArrayInputStream(content)); // 直接存储byte[]
            cqnService.insertCDSViewFiles(fileRecord);

            // 3. 解析并保存视图数据
            List<CDSViews> views = parser.parseCDSViews(new ByteArrayInputStream(content));

            // 获取所有视图名称
            Set<String> viewNames = new HashSet<>();
            for (CDSViews view : views) {
                if (view.getViewName() != null) {
                    viewNames.add(view.getViewName());
                }
            }

            // 批量查询已存在的视图
            List<CDSViews> existingViews = cqnService.batchSelectCDSViews(new ArrayList<>(viewNames));
            Map<String, CDSViews> existingViewMap = new HashMap<>();
            for (CDSViews view : existingViews) {
                existingViewMap.put(view.getViewName(), view);
            }
            
            // 分离需要更新和插入的视图
            List<CDSViews> toUpdate = new ArrayList<>();
            List<CDSViews> toInsert = new ArrayList<>();
            
            for (CDSViews view : views) {
                if (existingViewMap.containsKey(view.getViewName())) {
                    // 设置已存在视图的ID（如果需要其他字段可在此补充）
                    CDSViews existing = existingViewMap.get(view.getViewName());
                    view.setId(existing.getId()); // 如果CDSViews有ID字段
                    toUpdate.add(view);
                } else {
                    toInsert.add(view);
                }
            }
            
            // 执行批量操作
            if (!toInsert.isEmpty()) {
                cqnService.batchInsertCDSViews(toInsert);
            }
            if (!toUpdate.isEmpty()) {
                cqnService.batchUpdateCDSViews(toUpdate);
            }

            return "成功导入CDS视图: 新增" + toInsert.size() + "条, 更新" + toUpdate.size() + "条";
        } catch (Exception e) {
            throw new BusinessException("CDS视图上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public String uploadCDSViewFields(InputStream txt, String langu, String filename) {
        try {
            // 1. 读取完整文件内容
            byte[] content = IOUtils.toByteArray(txt);

            // 2. 保存文件到CDSViewFiles
            CDSViewFiles fileRecord = CDSViewFiles.create();
            fileRecord.setId(UUID.randomUUID().toString());
            // 
            if (filename != null ) {
                fileRecord.setFileName(filename);
            }else{
                fileRecord.setFileName("view_fields_" + UUID.randomUUID() + ".txt");
            }
            
            fileRecord.setSize(String.valueOf(content.length));
            fileRecord.setMediaType("text/plain");
            fileRecord.setFileContent(new ByteArrayInputStream(content)); // 直接存储byte[]
            cqnService.insertCDSViewFiles(fileRecord);

            // 3. 解析并保存字段数据
            List<Viewfields> fields = parser.parseViewFields(new ByteArrayInputStream(content), langu);

            // 收集所有表名
            Set<String> tableNames = new HashSet<>();
            for (Viewfields field : fields) {
                if (field.getTableName() != null) {
                    field.setFile(fileRecord);
                    tableNames.add(field.getTableName());
                }
            }
            
            // 批量查询已存在的字段（按表名和语言）
            List<Viewfields> existingFields = cqnService.batchSelectViewfieldsByTable(
                new ArrayList<>(tableNames), langu);
            
            // 构建复合键的映射 (tableName + fieldName + langu)
            Map<String, Viewfields> existingFieldMap = new HashMap<>();
            for (Viewfields field : existingFields) {
                String key = field.getTableName() + "|" + field.getLangu();
                existingFieldMap.put(key, field);
            }
            
            // 分离需要更新和插入的字段
            List<Viewfields> toUpdate = new ArrayList<>();
            List<Viewfields> toInsert = new ArrayList<>();
            
            for (Viewfields field : fields) {
                String compositeKey = field.getTableName() + "|"  + langu;
                
                if (existingFieldMap.containsKey(compositeKey)) {
                    // 设置已存在字段的ID
                    Viewfields existing = existingFieldMap.get(compositeKey);
                    field.setId(existing.getId());
                    toUpdate.add(field);
                } else {
                    toInsert.add(field);
                }
            }
            
            // 执行批量操作
            if (!toInsert.isEmpty()) {
                cqnService.batchInsertViewfields(toInsert);
            }
            if (!toUpdate.isEmpty()) {
                cqnService.batchUpdateViewfields(toUpdate);
            }
            
            return "成功导入视图字段: 新增" + toInsert.size() + "条, 更新" + toUpdate.size() + "条";
        } catch (Exception e) {
            throw new BusinessException("视图字段上传失败: " + e.getMessage(), e);
        }
    }
}

