package jame.work.filesserver.entity.vo;

import lombok.Data;

/**
 * @author : Jame
 * @date : 2025/12/23 下午 2:32
 */
@Data
public class FileItemVO {

    private String id;
    private String path;
    private String name;
    private String type;
    private String originType;
    private String size;
    private String date;
    private int items;



}
