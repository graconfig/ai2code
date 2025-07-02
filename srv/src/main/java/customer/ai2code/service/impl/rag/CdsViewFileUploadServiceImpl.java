package customer.ai2code.service.impl.rag;

import cds.gen.mainservice.CDSViewFiles;
import cds.gen.mainservice.CDSViews; // 导入mainservice包下的实体
import cds.gen.mainservice.Viewfields;
import customer.ai2code.exception.BusinessException;
import customer.ai2code.service.execution.CdsViewFileUploadService;
import customer.ai2code.service.impl.GenericCqnService;
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
    public String uploadCDSViews(InputStream excel) {
        try {
            // 1. 读取完整文件内容
            byte[] content = IOUtils.toByteArray(excel);

            // 2. 保存文件到CDSViewFiles
            CDSViewFiles fileRecord = CDSViewFiles.create();
            fileRecord.setId(UUID.randomUUID().toString());
            fileRecord.setFileName("cds_view_" + UUID.randomUUID() + ".xlsx");
            fileRecord.setSize(String.valueOf(content.length));
            // fileRecord.setMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            fileRecord.setFileContent(new ByteArrayInputStream(content)); // 直接存储byte[]
            cqnService.insertCDSViewFiles(fileRecord);

            // 3. 解析并保存视图数据
            List<CDSViews> views = parser.parseCDSViews(excel);

            // 获取所有视图名称
            Set<String> viewNames = new HashSet<>();
            for (CDSViews view : views) {
                if (view.getViewName() != null) {
                    viewNames.add(view.getViewName());
                }
            }

            // 删除已存在的视图
            if (!viewNames.isEmpty()) {
                cqnService.deleteCDSViewsByNames(new ArrayList<>(viewNames));
            }

            for (CDSViews view : views) {
                cqnService.insertCDSViews(view);
            }

            return "成功导入 " + views.size() + " 条CDS视图";
        } catch (Exception e) {
            throw new BusinessException("CDS视图上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public String uploadCDSViewFields(InputStream txt, String langu) {
        try {
            // 1. 读取完整文件内容
            byte[] content = IOUtils.toByteArray(txt);

            // 2. 保存文件到CDSViewFiles
            CDSViewFiles fileRecord = CDSViewFiles.create();
            fileRecord.setId(UUID.randomUUID().toString());
            fileRecord.setFileName("view_fields_" + UUID.randomUUID() + ".txt");
            fileRecord.setSize(String.valueOf(content.length));
            fileRecord.setMediaType("text/plain");
            fileRecord.setFileContent(new ByteArrayInputStream(content)); // 直接存储byte[]
            cqnService.insertCDSViewFiles(fileRecord);

            // 3. 解析并保存字段数据
            List<Viewfields> fields = parser.parseViewFields(txt, langu);

            // 按表名和语言分组
            Map<String, List<Viewfields>> fieldsByTable = new HashMap<>();
            for (Viewfields field : fields) {
                String key = field.getTableName() + "|" + field.getLangu();
                fieldsByTable.computeIfAbsent(key, k -> new ArrayList<>()).add(field);
            }

            // 删除已存在的字段
            for (Map.Entry<String, List<Viewfields>> entry : fieldsByTable.entrySet()) {
                String[] parts = entry.getKey().split("\\|");
                if (parts.length == 2) {
                    cqnService.deleteViewFieldsByTableAndLangu(parts[0], parts[1]);
                }
            }

            for (Viewfields field : fields) {
                // 设置正确的文件关联
                field.setFile(fileRecord);
                cqnService.insertViewfields(field);
            }

            return "成功导入 " + fields.size() + " 条视图字段";
        } catch (Exception e) {
            throw new BusinessException("视图字段上传失败: " + e.getMessage(), e);
        }
    }
}