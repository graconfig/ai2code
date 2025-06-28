package customer.ai2code.service.impl.rag;


import cds.gen.ai.orchestration.rag.CDSViews;
import cds.gen.ai.orchestration.rag.Viewfields;
import com.sap.cds.ql.Insert;
import com.sap.cds.services.cds.CqnService;

import customer.ai2code.exception.BusinessException;
import customer.ai2code.service.execution.CdsViewFileUploadService;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

@Service
public class CdsViewFileUploadServiceImpl implements CdsViewFileUploadService {
    
    private final CqnService cqnService;
    
    public CdsViewFileUploadServiceImpl(CqnService cqnService) {
        this.cqnService = cqnService;
    }
    
    @Override
    @Transactional
    public int uploadExcelForCdsViews(MultipartFile file) throws BusinessException, IOException {
        if (file.isEmpty()) {
            throw new BusinessException("上传的Excel文件为空");
        }
        
        if (!file.getOriginalFilename().endsWith(".xlsx")) {
            throw new BusinessException("仅支持.xlsx格式的Excel文件");
        }
        
        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(inputStream)) {
            
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new BusinessException("Excel工作表不存在");
            }
            
            // 验证表头
            if (!validateExcelHeader(sheet.getRow(0))) {
                throw new BusinessException("Excel表头格式错误，需包含ViewName、ViewDesc、ViewCategory列");
            }
            
            // 解析数据行并转换为CDSViews实体
            List<CDSViews> viewList = new ArrayList<>();
            for (int rowNum = 1; rowNum <= sheet.getLastRowNum(); rowNum++) {
                Row row = sheet.getRow(rowNum);
                if (row == null) continue;
                
                CDSViews view = CDSViews.create();
                view.setViewName(getCellValue(row.getCell(0)));
                view.setViewDesc(getCellValue(row.getCell(1)));
                view.setViewCategory(getCellValue(row.getCell(2)));
                view.setIsActive(true);
                view.setCreatedAt(Instant.now());
                
                viewList.add(view);
            }
            
            // 使用CQN批量插入
            cqnService.run(Insert.into("AI_Orchestration_Rag_CDSViews").entries(viewList));
            return viewList.size();
            
        } catch (IOException e) {
            throw new BusinessException("读取Excel文件失败：" + e.getMessage(), e);
        }
    }
    
    @Override
    @Transactional
    public int uploadTxtForCdsViewFields(MultipartFile file, String viewName, String locale) 
            throws BusinessException, IOException {
        if (file.isEmpty()) {
            throw new BusinessException("上传的TXT文件为空");
        }
        
        if (!file.getOriginalFilename().endsWith(".txt")) {
            throw new BusinessException("仅支持.txt格式的TXT文件");
        }
        
        try (InputStream inputStream = file.getInputStream();
             Scanner scanner = new Scanner(inputStream, "UTF-8")) {
            
            List<Viewfields> fieldList = new ArrayList<>();
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue;
                
                String[] parts = line.split("\t");
                if (parts.length >= 2) {
                    Viewfields field = Viewfields.create();
                    field.setTableName(viewName);
                    field.setTableDesc(viewName);
                    field.setContent(parts[0] + "|" + parts[1] + (parts.length > 2 ? "|" + parts[2] : ""));
                    field.setLangu(locale);
                    field.setCreatedAt(Instant.now());
                    
                    fieldList.add(field);
                }
            }
            
            // 使用CQN批量插入
            cqnService.run(Insert.into("AI_Orchestration_Rag_Viewfields").entries(fieldList));
            return fieldList.size();
            
        } catch (IOException e) {
            throw new BusinessException("读取TXT文件失败：" + e.getMessage(), e);
        }
    }
    
    // -------------------- 私有辅助方法 --------------------
    
    /**
     * 验证Excel表头
     */
    private boolean validateExcelHeader(Row headerRow) {
        if (headerRow == null || headerRow.getLastCellNum() < 3) {
            return false;
        }
        
        String[] expectedHeaders = {"ViewName", "ViewDesc", "ViewCategory"};
        for (int i = 0; i < expectedHeaders.length; i++) {
            Cell cell = headerRow.getCell(i);
            if (cell == null || !expectedHeaders[i].equals(getCellValue(cell))) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 获取单元格值
     */
    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf((int) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }
}