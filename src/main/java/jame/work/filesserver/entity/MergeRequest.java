package jame.work.filesserver.entity;

import lombok.Data;

@Data
public class MergeRequest {
    private String md5;
    private String fileName;
    private int totalChunks;
    private String currentPath;


}