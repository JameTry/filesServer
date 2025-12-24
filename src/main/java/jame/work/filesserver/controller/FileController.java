package jame.work.filesserver.controller;

import jame.work.filesserver.entity.*;
import jame.work.filesserver.entity.vo.FileVO;
import jame.work.filesserver.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author Jame
 * @since 2025-12-22
 */
@RestController
@RequestMapping("/file")
public class FileController {

    @Autowired
    private FileService fileService;

    @GetMapping("/list")
    public FileVO list(@RequestParam("path") String path,
                       @RequestParam(value = "sort", required = false, defaultValue = "date") String sort,
                       @RequestParam(value = "keyword", required = false, defaultValue = "") String keyword) {
        return fileService.list(path, sort, keyword);
    }

    @GetMapping("/createFolder")
    public R createFolder(@RequestParam("path") String path,
                          @RequestParam("name") String name) {
        if (fileService.createFolder(path, name)) {
            return R.ok();
        }
        return R.error();
    }

    @GetMapping("/rename")
    public R rename(@RequestParam("path") String path,
                    @RequestParam("name") String name,
                    @RequestParam("newName") String newName) {
        if (fileService.rename(path, name, newName)) {
            return R.ok();
        }
        return R.error();
    }


    @PostMapping("/move")
    public R move(@RequestBody MoveBatchRequest request) {
        MoveResult result = fileService.moveBatch(request);
        return R.ok(result);
    }

    @PostMapping("/recycle")
    public R recycle(@RequestBody List<String> filePaths) {
        RecycleResult result = fileService.recycle(filePaths);
        return R.ok(result);
    }

    @PostMapping("/delete")
    public R delete(@RequestBody MoveBatchRequest request) {
        MoveResult result = fileService.delete(request);
        return R.ok(result);
    }


    @GetMapping("/check")
    public Map<String, Boolean> checkFile(@RequestParam("md5") String md5,
                                          @RequestParam("fileName") String fileName,
                                          @RequestParam("path") String path) {
        boolean exists = fileService.checkFileExists(md5, fileName, path);
        Map<String, Boolean> result = new HashMap<>();
        result.put("exists", exists);
        return result;
    }

    @GetMapping("/uploaded-chunks")
    public Map<String, List<Integer>> getUploadedChunks(@RequestParam("md5") String md5, @RequestParam("fileName") String fileName) {
        List<Integer> uploadedChunks = fileService.getUploadedChunks(md5, fileName);
        Map<String, List<Integer>> result = new HashMap<>();
        result.put("uploadedChunks", uploadedChunks);
        return result;
    }

    @PostMapping("/upload-chunk")
    public void uploadChunk(@RequestParam("chunk") MultipartFile chunk,
                            @RequestParam("chunkIndex") int chunkIndex,
                            @RequestParam("totalChunks") int totalChunks,
                            @RequestParam("md5") String md5,
                            @RequestParam("fileName") String fileName) throws IOException {
        fileService.uploadChunk(chunk, chunkIndex, totalChunks, md5, fileName);
    }

    @PostMapping("/merge-chunks")
    public void mergeChunks(@RequestBody MergeRequest mergeRequest) throws IOException {
        fileService.mergeChunks(mergeRequest.getMd5(), mergeRequest.getFileName(),
                mergeRequest.getTotalChunks(), mergeRequest.getCurrentPath());
    }

    @PostMapping("/cancel-upload")
    public void cancelUpload(@RequestBody MergeRequest request) throws IOException {
        fileService.cancelUpload(request.getMd5());
    }


    @GetMapping("/preview/{path}")
    public void streamVideo(
            @PathVariable String path,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            fileService.preview(path, request, response);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @GetMapping("/download")
    public void download(
            @RequestParam("path") List<String> path,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        DownloadRequest dr = new DownloadRequest();
        dr.setPath(path);
        fileService.download(dr, request, response);
    }


}
