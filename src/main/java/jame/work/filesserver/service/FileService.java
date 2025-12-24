package jame.work.filesserver.service;

import jame.work.filesserver.entity.DownloadRequest;
import jame.work.filesserver.entity.MoveBatchRequest;
import jame.work.filesserver.entity.MoveResult;
import jame.work.filesserver.entity.RecycleResult;
import jame.work.filesserver.entity.vo.FileItemVO;
import jame.work.filesserver.entity.vo.FileVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * @author : Jame
 * @date : 2025/12/23 下午 2:35
 */
@Service
public class FileService {

    @Value("${cfg.root-path}")
    private String rootPath;

    private final String tempFolderName = "_file_temp";
    private final String deletedFolderName = "_file_del";

    private static final Set<String> EXCLUDE_DIRS = new HashSet<>();


    public FileService() {
        EXCLUDE_DIRS.add(tempFolderName);
        EXCLUDE_DIRS.add(deletedFolderName);
    }

    // 每 10 分钟执行一次
    @Scheduled(cron = "* */10  * * * ?")
    public void cleanTempFolder() {
        File tempDir = new File(rootPath, tempFolderName);
        if (!tempDir.exists() || !tempDir.isDirectory()) {
            return;
        }

        long now = System.currentTimeMillis();
        long expireTime = 2L * 60 * 60 * 1000; // 2 小时

        File[] files = tempDir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            // 使用 lastModified 作为“创建时间”（跨平台最稳）
            long createTime = file.lastModified();

            if (now - createTime >= expireTime) {
                boolean deleted = file.delete();
                if (!deleted) {
                    System.err.println("临时文件删除失败：" + file.getAbsolutePath());
                }
            }
        }
    }


    public FileVO list(String path, String sort, String keyword) {
        if (path == null || path.trim().isEmpty() || "/".equals(path)) {
            path = "/";
        } else {
            path = Paths.get(path).normalize().toString().replace("\\", "/");
            if (path.startsWith("..") || path.contains("../")) {
                throw new IllegalArgumentException("无效路径");
            }
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            if (!path.endsWith("/")) {
                path += "/";
            }
        }

        // 拼接完整物理路径
        String fullPath = rootPath;
        if (!fullPath.endsWith(File.separator)) {
            fullPath += File.separator;
        }
        // 如果 path 是 /，则直接使用 rootPath
        if (!"/".equals(path)) {
            // 去除开头的 /
            String sub = path.substring(1);
            fullPath += sub;
        }

        Path dirPath = Paths.get(fullPath);
        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            throw new RuntimeException("目录不存在或不是文件夹");
        }


        Comparator<Path> comparator = Comparator.comparing((Path p) -> !Files.isDirectory(p));

        switch (sort) {
            case "size":
                comparator = comparator.thenComparing(p -> {
                    try {
                        return Files.isDirectory(p) ? 0L : Files.size(p);
                    } catch (IOException e) {
                        return 0L;
                    }
                }, Comparator.reverseOrder());
                break;

            case "type":
                comparator = comparator.thenComparing(p -> {
                    if (Files.isDirectory(p)) {
                        return "";
                    }
                    return getFileExtension(p.getFileName().toString());
                });
                break;

            case "date":
            default:
                comparator = comparator.thenComparing(p -> {
                    try {
                        return Files.getLastModifiedTime(p).toMillis();
                    } catch (IOException e) {
                        return 0L;
                    }
                }, Comparator.reverseOrder());
                break;
        }

        List<FileItemVO> fileItemVOList;

        boolean isSearch = keyword != null && !keyword.trim().isEmpty();
        Path root = Paths.get(rootPath).toAbsolutePath().normalize();
        try (Stream<Path> stream = isSearch
                ? Files.walk(dirPath)
                : Files.list(dirPath)) {
            fileItemVOList = stream
                    .filter(p -> !p.equals(dirPath))
                    .filter(p -> {
                        for (Path part : p) {
                            if (EXCLUDE_DIRS.contains(part.toString())) {
                                return false;
                            }
                        }
                        return true;
                    })

                    .filter(p -> {
                        if (!isSearch) return true;
                        return p.getFileName().toString()
                                .toLowerCase()
                                .contains(keyword.toLowerCase());
                    })
                    .sorted(comparator)
                    .map(p -> {
                        try {
                            String name = p.getFileName().toString();
                            boolean isDir = Files.isDirectory(p);

                            String type;
                            String originalType;

                            if (isDir) {
                                type = "folder";
                                originalType = "folder";
                            } else {
                                originalType = getFileExtension(name);
                                type = mapFileType(originalType);
                            }

                            // 大小
                            String size = isDir ? "-" : formatFileSize(Files.size(p));

                            // 修改时间
                            BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
                            String updateTime = formatTime(attrs.lastModifiedTime());

                            // 相对路径
                            Path abs = p.toAbsolutePath().normalize();

                            String relative = "/" + root.relativize(abs)
                                    .toString()
                                    .replace("\\", "/");
                            int items = 0;
                            if (isDir) {
                                relative += "/";
                                try (Stream<Path> childStream = Files.list(p)) {
                                    items = (int) childStream.count();
                                }
                            }

                            String id;
                            if (isDir) {
                                id = calculateStringMD5(relative);
                            } else {
                                id = calculateFileMD5(p);
                            }


                            FileItemVO item = new FileItemVO();
                            item.setId(id);
                            item.setPath(relative);
                            item.setName(name);
                            item.setType(type);
                            item.setOriginType(originalType);
                            item.setSize(size);
                            item.setDate(updateTime);
                            item.setItems(items);

                            return item;
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("读取目录失败", e);
        }

        FileVO fileVO = new FileVO();
        fileVO.setCurrentPath(path);
        fileVO.setFileItemVOList(fileItemVOList);

        return fileVO;
    }

    private String getFileExtension(String filename) {
        int index = filename.lastIndexOf(".");
        if (index == -1 || index == filename.length() - 1) {
            return "";
        }
        return filename.substring(index + 1).toLowerCase();
    }

    private String mapFileType(String ext) {
        switch (ext) {
            case "png":
            case "jpg":
            case "jpeg":
            case "gif":
            case "bmp":
            case "webp":
                return "image";

            case "mp4":
            case "avi":
            case "mov":
            case "mkv":
                return "video";

            case "mp3":
            case "wav":
            case "flac":
                return "audio";

            case "zip":
            case "rar":
            case "7z":
            case "tar":
            case "gz":
                return "archive";

            default:
                return "file";
        }
    }


    // 格式化文件大小
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }

    // 格式化时间
    private String formatTime(FileTime fileTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(fileTime.toMillis()));
    }


    //==========

    public boolean checkFileExists(String md5, String fileName, String path) {
        // 双重校验：MD5 + 文件名（或大小等），这里简单用 MD5 + 文件是否存在
        // 实际可使用数据库存储 MD5 -> 文件路径映射
        Path targetPath = Paths.get(rootPath, path, fileName);
        if (Files.exists(targetPath)) {
            try {
                String existingMd5 = calculateFileMD5(targetPath);
                return md5.equals(existingMd5);
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    public List<Integer> getUploadedChunks(String md5, String fileName) {
        Path tempDir = Paths.get(rootPath, tempFolderName, md5);
        if (!Files.exists(tempDir)) {
            return new ArrayList<>();
        }
        try (Stream<Path> stream = Files.list(tempDir)) {
            return stream
                    .filter(p -> p.getFileName().toString().startsWith("chunk_"))
                    .map(p -> Integer.parseInt(p.getFileName().toString().split("_")[1]))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    public void uploadChunk(MultipartFile chunk, int chunkIndex, int totalChunks, String md5, String fileName) throws IOException {
        Path tempDir = Paths.get(rootPath, tempFolderName, md5);
        if (!Files.exists(tempDir)) {
            Files.createDirectories(tempDir);
        }
        Path chunkPath = tempDir.resolve("chunk_" + chunkIndex);
        chunk.transferTo(chunkPath);
    }

    public void mergeChunks(String md5, String fileName, int totalChunks, String filePath) throws IOException {
        Path tempDir = Paths.get(rootPath, tempFolderName, md5);
        if (!Files.exists(tempDir)) {
            Files.createDirectory(tempDir);
            if (!Files.exists(tempDir)) {
                throw new IOException("临时目录不存在");
            }
        }

        Path targetDir = Paths.get(rootPath, filePath);
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
        }
        Path targetPath = targetDir.resolve(fileName);

        try (OutputStream out = Files.newOutputStream(targetPath)) {
            for (int i = 0; i < totalChunks; i++) {
                Path chunkPath = tempDir.resolve("chunk_" + i);
                if (!Files.exists(chunkPath)) {
                    throw new IOException("缺少分片: " + i);
                }
                Files.copy(chunkPath, out);
                Files.delete(chunkPath); // 复制后删除
            }
        }

        // 删除临时目录
        Files.deleteIfExists(tempDir);
    }

    // 计算文件 MD5 的辅助方法
    private String calculateFileMD5(Path filePath) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (InputStream is = Files.newInputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int readLen;
            while ((readLen = is.read(buffer)) != -1) {
                md.update(buffer, 0, readLen);
            }
        }
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String calculateStringMD5(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public void cancelUpload(String md5) throws IOException {
        Path tempDir = Paths.get(rootPath, "temp", md5);
        if (Files.exists(tempDir)) {
            try (Stream<Path> stream = Files.walk(tempDir)) {
                stream.sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException ignored) {
                            }
                        });
            }
        }
    }

    public Boolean createFolder(String path, String name) {
        Path path1 = Paths.get(rootPath, path, name);
        File file = new File(path1.toString());
        return file.mkdirs();
    }

    public boolean rename(String path, String name, String newName) {
        Path path1 = Paths.get(rootPath, path, name);
        File file = new File(path1.toString());
        return file.renameTo(new File(path1.getParent().toString(), newName));
    }

    public MoveResult moveBatch(MoveBatchRequest req) {

        MoveResult result = new MoveResult();
        result.setSuccess(new ArrayList<>());
        result.setSkipped(new ArrayList<>());
        result.setConflicted(new ArrayList<>());

        for (String name : req.getNames()) {

            Path source = Paths.get(rootPath, req.getSourcePath(), name);
            Path target = Paths.get(rootPath, req.getTargetPath(), name);

            if (!Files.exists(source)) {
                result.getSkipped().add(name);
                continue;
            }

            try {
                Files.createDirectories(target.getParent());

                if (Files.exists(target)) {
                    if (!req.isOverwrite()) {
                        result.getConflicted().add(name);
                        continue;
                    }
                }

                Files.move(
                        source,
                        target,
                        StandardCopyOption.REPLACE_EXISTING
                );

                result.getSuccess().add(name);

            } catch (IOException e) {
                result.getSkipped().add(name);
            }
        }

        return result;
    }

    public void preview(String path,
                        HttpServletRequest request,
                        HttpServletResponse response) throws IOException {
        File file = new File(Paths.get(rootPath, path).toString());
        if (!file.exists()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String suffix = getSuffix(path);
        long fileLength = file.length();
        String range = request.getHeader("Range");

        response.setHeader("Access-Control-Allow-Origin", "*");
        response.setHeader("Accept-Ranges", "bytes");

        if (isImage(suffix)) {
            response.setContentType(getImageType(suffix));
            response.setContentLengthLong(fileLength);
            stream(file, response.getOutputStream());
            return;
        }

        response.setContentType(getMediaType(suffix));

        if (range == null) {
            response.setContentLengthLong(fileLength);
            Files.copy(file.toPath(), response.getOutputStream());
            return;
        }

        // Range: bytes=start-
        long start = Long.parseLong(range.replace("bytes=", "").split("-")[0]);
        long end = fileLength - 1;
        long contentLength = end - start + 1;

        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        response.setHeader("Content-Range",
                "bytes " + start + "-" + end + "/" + fileLength);
        response.setContentLengthLong(contentLength);

        try (RandomAccessFile raf = new RandomAccessFile(file, "r");
             OutputStream os = response.getOutputStream()) {

            raf.seek(start);
            byte[] buffer = new byte[8192];
            long remaining = contentLength;
            int len;

            while ((len = raf.read(buffer)) != -1 && remaining > 0) {
                os.write(buffer, 0, len);
                remaining -= len;
            }
        }
    }


    private void stream(File file, OutputStream os) throws IOException {
        try (InputStream in = Files.newInputStream(file.toPath())) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
        }
    }

    private String getSuffix(String filename) {
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private boolean isImage(String suffix) {
        return suffix.matches("jpg|jpeg|png|gif|webp");
    }

    private String getImageType(String suffix) {
        switch (suffix) {
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "webp":
                return "image/webp";
            default:
                return "image/jpeg";
        }
    }

    private String getMediaType(String suffix) {
        switch (suffix) {
            case "mp4":
                return "video/mp4";
            case "webm":
                return "video/webm";
            case "ogg":
                return "video/ogg";
            case "mp3":
                return "audio/mpeg";
            case "wav":
                return "audio/wav";
            case "aac":
                return "audio/aac";
            default:
                return "application/octet-stream";
        }
    }

    public void download(DownloadRequest request,
                         HttpServletRequest httpRequest,
                         HttpServletResponse response) throws IOException {

        List<File> files = request.getPath().stream()
                .map(p -> new File(rootPath + p))
                .filter(File::exists)
                .collect(Collectors.toList());

        if (files.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        boolean hasDir = files.stream().anyMatch(File::isDirectory);
        boolean needZip = hasDir || files.size() > 1;

        if (needZip) {
            File zipFile = prepareZip(files);
            downloadWithRange(zipFile, zipFile.getName(), httpRequest, response);
        } else {
            File file = files.get(0);
            downloadWithRange(file, file.getName(), httpRequest, response);
        }
    }

    private File prepareZip(List<File> sources) throws IOException {

        File tempDir = new File(rootPath, tempFolderName);
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        String zipName = "download_" + System.currentTimeMillis() + ".zip";
        File zipFile = new File(tempDir, zipName);

        try (ZipOutputStream zos = new ZipOutputStream(
                new BufferedOutputStream(new FileOutputStream(zipFile)))) {

            for (File src : sources) {
                zipRecursively(zos, src, src.getName());
            }
        }
        return zipFile;
    }

    private void zipRecursively(ZipOutputStream zos, File file, String entryName)
            throws IOException {

        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    zipRecursively(zos, child, entryName + "/" + child.getName());
                }
            }
        } else {
            zos.putNextEntry(new ZipEntry(entryName));
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = fis.read(buffer)) != -1) {
                    zos.write(buffer, 0, len);
                }
            }
            zos.closeEntry();
        }
    }


    private void downloadWithRange(File file,
                                   String downloadName,
                                   HttpServletRequest request,
                                   HttpServletResponse response) throws IOException {

        long fileLength = file.length();
        long start = 0;
        long end = fileLength - 1;

        String range = request.getHeader("Range");
        if (range != null && range.startsWith("bytes=")) {
            String[] parts = range.replace("bytes=", "").split("-");
            start = Long.parseLong(parts[0]);
            if (parts.length > 1 && !parts[1].isEmpty()) {
                end = Long.parseLong(parts[1]);
            }
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        }

        long contentLength = end - start + 1;

        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Content-Type", "application/octet-stream");
        response.setHeader("Content-Disposition",
                "attachment; filename=\"" +
                        URLEncoder.encode(downloadName, String.valueOf(StandardCharsets.UTF_8)) + "\"");
        response.setHeader("Content-Length", String.valueOf(contentLength));
        response.setHeader("Content-Range",
                "bytes " + start + "-" + end + "/" + fileLength);

        try (RandomAccessFile raf = new RandomAccessFile(file, "r");
             OutputStream os = response.getOutputStream()) {

            raf.seek(start);
            byte[] buffer = new byte[8192];
            long remaining = contentLength;
            int len;

            while (remaining > 0 &&
                    (len = raf.read(buffer, 0,
                            (int) Math.min(buffer.length, remaining))) != -1) {
                os.write(buffer, 0, len);
                remaining -= len;
            }
        }
    }


    public MoveResult delete(MoveBatchRequest request) {

        return null;
    }

    public RecycleResult recycle(List<String> request) {
        RecycleResult result = new RecycleResult();

        Path recycleDir = Paths.get(rootPath, deletedFolderName);
        try {
            Files.createDirectories(recycleDir);
        } catch (IOException e) {
            throw new RuntimeException("无法创建回收站目录", e);
        }

        for (String relativePath : request) {
            try {
                // 真实源路径
                Path source = Paths.get(rootPath, relativePath);

                if (!Files.exists(source)) {
                    result.getFailed().add(relativePath);
                    continue;
                }

                String originalName = source.getFileName().toString();

                // 时间戳 + UUID + 原文件名
                String newName = System.currentTimeMillis()
                        + "_" + originalName;

                Path target = recycleDir.resolve(newName);

                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                result.getSuccess().add(relativePath);

            } catch (Exception e) {
                result.getFailed().add(relativePath);
            }
        }
        return result;
    }
}
