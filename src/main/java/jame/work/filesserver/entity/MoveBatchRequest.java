package jame.work.filesserver.entity;

import lombok.Data;

import java.util.List;

@Data
public class MoveBatchRequest {

    private String sourcePath;
    private String targetPath;

    private List<String> names;   // 多个文件名

    /**
     * 覆盖策略
     * true  = 直接覆盖
     * false = 由后端检测并返回冲突列表
     */
    private boolean overwrite;
}
