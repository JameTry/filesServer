package jame.work.filesserver.entity;

import lombok.Data;

import java.util.List;

@Data
public class MoveResult {

    private List<String> success;     // 成功移动
    private List<String> skipped;     // 被跳过
    private List<String> conflicted;  // 发生冲突（仅 overwrite=false 时）
}