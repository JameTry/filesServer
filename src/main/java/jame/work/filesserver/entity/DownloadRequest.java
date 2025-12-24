package jame.work.filesserver.entity;

import lombok.Data;

import java.util.List;

@Data
public class DownloadRequest {
   private List<String> path;

}