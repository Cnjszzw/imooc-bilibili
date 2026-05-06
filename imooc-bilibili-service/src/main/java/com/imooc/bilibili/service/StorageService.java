package com.imooc.bilibili.service;

import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 文件存储服务抽象接口
 * 支持多种存储后端（FastDFS、MinIO等）
 */
public interface StorageService {

    /**
     * 分片上传文件（支持断点续传）
     * @param file 当前分片文件
     * @param fileMd5 完整文件的MD5值
     * @param sliceNo 当前分片编号（从1开始）
     * @param totalSliceNo 总分片数
     * @return 文件存储路径
     */
    String uploadFileBySlices(MultipartFile file, String fileMd5, Integer sliceNo, Integer totalSliceNo) throws Exception;

    /**
     * 获取文件类型（扩展名）
     * @param file 文件
     * @return 文件扩展名（不含点）
     */
    String getFileType(MultipartFile file);

    /**
     * 在线查看图片（通过服务端代理）
     */
    void viewImage(HttpServletRequest request, HttpServletResponse response, String url) throws Exception;

    /**
     * 在线播放视频（分片传输）
     */
    void viewVideoOnlineBySlices(HttpServletRequest request, HttpServletResponse response, String url) throws Exception;

    /**
     * 删除文件
     * @param filePath 文件路径
     */
    void deleteFile(String filePath) throws Exception;
}
