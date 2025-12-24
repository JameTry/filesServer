package jame.work.filesserver.entity;

import lombok.Data;

/**
 * @author : Jame
 * @date : 2025/12/24 上午 8:15
 */
@Data
public class MoveFileRequest {

    private String path;
    private String targetPath;
    private String name;
}
