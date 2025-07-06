      
package customer.ai2code.util;

import cds.gen.mainservice.CDSViews;
import cds.gen.mainservice.Viewfields;
import customer.ai2code.exception.BusinessException;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Component
public class FileParser {
    
    // 解析CDSViews（Excel） - 保持不变
    public List<CDSViews> parseCDSViews(InputStream excelStream) {
        List<CDSViews> views = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(excelStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null || sheet.getLastRowNum() < 1) return views;
            
            Row headerRow = sheet.getRow(0);
            int nameCol = getColumnIndex(headerRow, "viewName");
            int categoryCol = getColumnIndex(headerRow, "viewCategory");
            int descCol = getColumnIndex(headerRow, "viewDesc");
            
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                CDSViews view = CDSViews.create();
                view.setViewName(getCellValue(row.getCell(nameCol)));
                view.setViewCategory(getCellValue(row.getCell(categoryCol)));
                view.setViewDesc(getCellValue(row.getCell(descCol)));
                view.setIsActive(true);
                
                if (view.getViewName() != null && !view.getViewName().isEmpty()) {
                    views.add(view);
                }
            }
        } catch (Exception e) {
            throw new BusinessException("Excel解析失败: " + e.getMessage(), e);
        }
        return views;
    }
    
    // 解析Viewfields（TXT） - 关键修改
    public List<Viewfields> parseViewFields(InputStream txtStream, String defaultLang) {
        List<Viewfields> fields = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(txtStream))) {
            StringBuilder lineBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                
                // 检查行是否完整（以]]结尾）
                if (line.endsWith("]]")) {
                    // 完整行直接处理
                    if (lineBuilder.length() > 0) {
                        // 如果有之前未处理的内容，先处理
                        processLine(lineBuilder.toString(), fields, defaultLang);
                        lineBuilder.setLength(0);
                    }
                    processLine(line, fields, defaultLang);
                } else {
                    // 不完整的行追加到builder
                    lineBuilder.append(line);
                }
            }
            
            // 处理最后未处理的行
            if (lineBuilder.length() > 0) {
                processLine(lineBuilder.toString(), fields, defaultLang);
            }
        } catch (Exception e) {
            throw new BusinessException("TXT解析失败: " + e.getMessage(), e);
        }
        return fields;
    }

    // 处理单行内容
    private void processLine(String line, List<Viewfields> fields, String defaultLang) {
        // 查找第一个引号结束位置
        int firstQuoteEnd = line.indexOf("\",");
        if (firstQuoteEnd == -1) return;
        
        // 查找第二个引号结束位置
        int secondQuoteEnd = line.indexOf("\",", firstQuoteEnd + 2);
        if (secondQuoteEnd == -1) return;
        
        // 提取表名（第一个引号内容）
        String tableName = line.substring(1, firstQuoteEnd).trim();
        
        // 提取表描述（第二个引号内容）
        String tableDesc = line.substring(firstQuoteEnd + 3, secondQuoteEnd).trim();
        
        // 提取内容部分（第二个引号之后的内容）
        String content = line.substring(secondQuoteEnd + 2).trim();
        
        // 移除内容开头的逗号（如果有）
        if (content.startsWith(",")) {
            content = content.substring(1).trim();
        }

        // 创建实体
        Viewfields field = Viewfields.create();
        field.setCategory("CDSViewFields");
        field.setContent(content);
        field.setTableName(tableName);
        field.setTableDesc(tableDesc);
        field.setLangu(defaultLang);
        
        fields.add(field);
    }
    
    // 工具方法 - 保持不变
    private int getColumnIndex(Row headerRow, String header) {
        for (int i = 0; i < headerRow.getPhysicalNumberOfCells(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell != null && header.equals(cell.getStringCellValue().trim())) {
                return i;
            }
        }
        throw new IllegalArgumentException("缺少表头：" + header);
    }
    
    // 增强单元格处理
    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
    
    // 增强表头查找
    private int indexOf(String[] array, String target) {
        if (array == null || target == null) return -1;
        
        for (int i = 0; i < array.length; i++) {
            if (target.equalsIgnoreCase(array[i].trim())) {
                return i;
            }
        }
        return -1; // 未找到返回-1
    }
}

    