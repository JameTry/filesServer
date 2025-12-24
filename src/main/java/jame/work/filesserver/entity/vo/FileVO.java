package jame.work.filesserver.entity.vo;

import lombok.Data;

import java.util.List;

/**
 * @author : Jame
 * @date : 2025/12/23 下午 2:31
 */
@Data
public class FileVO {


    private String currentPath;

    private List<FileItemVO> fileItemVOList;

}
