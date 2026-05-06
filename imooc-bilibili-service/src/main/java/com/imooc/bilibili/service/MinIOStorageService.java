package com.imooc.bilibili.service;

import com.imooc.bilibili.domain.exception.ConditionException;
import io.minio.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

/**
 * MinIO 文件存储服务实现
 * 提供分片上传（断点续传）、图片/视频查看等功能
 *
 * 分片上传策略：前端分片 → 服务端临时落盘 → 所有分片到齐后一次性 putObject 到 MinIO
 * 兼容 MinIO SDK 8.5.7（该版本无 createMultipartUpload/uploadPart/completeMultipartUpload API）
 */
@Service("minioStorageService")
public class MinIOStorageService implements StorageService {

    private static final Logger logger = LoggerFactory.getLogger(MinIOStorageService.class);

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Value("${minio.bucket-name}")
    private String bucketName;

    private static final String UPLOADED_SIZE_KEY = "minio-uploaded-size:";
    private static final String UPLOADED_NO_KEY = "minio-uploaded-no:";

    private static final String TEMP_UPLOAD_DIR = System.getProperty("java.io.tmpdir") + File.separator + "minio-uploads";

    // ==================== 分片上传（断点续传） ====================

    @Override
    public String uploadFileBySlices(MultipartFile file, String fileMd5, Integer sliceNo, Integer totalSliceNo) throws Exception {
        if (file == null || sliceNo == null || totalSliceNo == null) {
            throw new ConditionException("参数异常！");
        }

        String fileType = getFileType(file);
        String objectName = fileMd5 + "." + fileType;

        String uploadedSizeKey = UPLOADED_SIZE_KEY + fileMd5;
        String uploadedNoKey = UPLOADED_NO_KEY + fileMd5;

        // 为当前文件创建临时目录
        File tempDir = new File(TEMP_UPLOAD_DIR, fileMd5);
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        // 将当前分片保存到临时文件
        File sliceFile = new File(tempDir, String.valueOf(sliceNo));
        file.transferTo(sliceFile);

        // 更新分片计数
        redisTemplate.opsForValue().increment(uploadedNoKey);

        // 更新已上传总大小
        String uploadedSizeStr = redisTemplate.opsForValue().get(uploadedSizeKey);
        long uploadedSize = uploadedSizeStr != null ? Long.parseLong(uploadedSizeStr) : 0L;
        uploadedSize += file.getSize();
        redisTemplate.opsForValue().set(uploadedSizeKey, String.valueOf(uploadedSize));

        // 检查是否所有分片都已上传完成
        String uploadedNoStr = redisTemplate.opsForValue().get(uploadedNoKey);
        int uploadedNo = Integer.parseInt(uploadedNoStr);
        String resultPath = "";
        if (uploadedNo == totalSliceNo) {
            // 所有分片到齐，合并并上传到 MinIO
            resultPath = uploadMergedFile(tempDir, totalSliceNo, objectName);

            // 清理临时文件和 Redis
            deleteTempDir(tempDir);
            List<String> keyList = Arrays.asList(uploadedNoKey, uploadedSizeKey);
            redisTemplate.delete(keyList);
        }
        return resultPath;
    }

    /**
     * 将本地临时分片合并后上传到 MinIO
     */
    private String uploadMergedFile(File tempDir, int totalSliceNo, String objectName) throws Exception {
        // 计算总大小
        long totalSize = 0;
        List<File> sliceFiles = new ArrayList<>();
        for (int i = 1; i <= totalSliceNo; i++) {
            File f = new File(tempDir, String.valueOf(i));
            sliceFiles.add(f);
            totalSize += f.length();
        }

        logger.info("MinIO 开始合并上传: object={}, totalSize={} bytes, slices={}",
                objectName, totalSize, totalSliceNo);

        // 创建顺序输入流（按分片编号顺序读取）
        Vector<FileInputStream> streams = new Vector<>();
        for (File f : sliceFiles) {
            streams.add(new FileInputStream(f));
        }
        SequenceInputStream combinedStream = new SequenceInputStream(streams.elements());

        // 上传到 MinIO
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(combinedStream, totalSize, -1)
                        .contentType(getContentType(objectName))
                        .build());

        logger.info("MinIO 上传成功: bucket={}, object={}", bucketName, objectName);
        return objectName;
    }

    /**
     * 递归删除临时目录
     */
    private void deleteTempDir(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteTempDir(f);
                }
            }
        }
        dir.delete();
    }

    // ==================== 文件类型 ====================

    @Override
    public String getFileType(MultipartFile file) {
        if (file == null) {
            throw new ConditionException("非法文件！");
        }
        String fileName = file.getOriginalFilename();
        int index = fileName.lastIndexOf(".");
        return fileName.substring(index + 1);
    }

    // ==================== 图片查看 ====================

    @Override
    public void viewImage(HttpServletRequest request, HttpServletResponse response, String url) throws Exception {
        try (InputStream inputStream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(url)
                        .build());
             OutputStream outputStream = response.getOutputStream()) {

            String contentType = getContentType(url);
            response.setContentType(contentType);

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();
        } catch (Exception e) {
            logger.error("MinIO 读取图片失败: bucket={}, object={}", bucketName, url, e);
            try {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
            } catch (IOException ignored) {}
        }
    }

    // ==================== 视频在线播放（支持Range请求） ====================

    @Override
    public void viewVideoOnlineBySlices(HttpServletRequest request, HttpServletResponse response, String url) throws Exception {
        try {
            // 获取文件元信息
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(url)
                            .build());
            long totalFileSize = stat.size();

            // 解析 Range 请求头
            String rangeStr = request.getHeader("Range");
            long begin = 0;
            long end = totalFileSize - 1;
            if (rangeStr != null && !rangeStr.isEmpty()) {
                String[] range = rangeStr.split("bytes=|-");
                if (range.length >= 2) {
                    begin = Long.parseLong(range[1]);
                }
                if (range.length >= 3) {
                    end = Long.parseLong(range[2]);
                }
            }
            long len = (end - begin) + 1;

            // 设置响应头
            String contentRange = "bytes " + begin + "-" + end + "/" + totalFileSize;
            response.setHeader("Content-Range", contentRange);
            response.setHeader("Accept-Ranges", "bytes");
            response.setContentType("video/mp4");
            response.setContentLength((int) len);
            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);

            // 流式返回视频数据
            try (InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(url)
                            .offset(begin)
                            .length(len)
                            .build());
                 OutputStream outputStream = response.getOutputStream()) {

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
            }
        } catch (Exception e) {
            logger.error("MinIO 读取视频失败: bucket={}, object={}", bucketName, url, e);
            throw e;
        }
    }

    // ==================== 文件删除 ====================

    @Override
    public void deleteFile(String filePath) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(filePath)
                        .build());
    }

    // ==================== 私有辅助方法 ====================

    private String getContentType(String fileName) {
        String ext = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        switch (ext) {
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "png":
                return "image/png";
            case "gif":
                return "image/gif";
            case "webp":
                return "image/webp";
            case "bmp":
                return "image/bmp";
            case "svg":
                return "image/svg+xml";
            case "mp4":
                return "video/mp4";
            default:
                return "application/octet-stream";
        }
    }
}
