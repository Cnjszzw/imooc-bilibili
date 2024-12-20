package com.imooc.bilibili.service.util;

import com.github.tobato.fastdfs.domain.fdfs.FileInfo;
import com.github.tobato.fastdfs.domain.fdfs.MetaData;
import com.github.tobato.fastdfs.domain.fdfs.StorePath;
import com.github.tobato.fastdfs.service.AppendFileStorageClient;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.imooc.bilibili.domain.exception.ConditionException;
import io.netty.util.internal.StringUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.*;

@Component
public class FastDFSUtil {

    public FastDFSUtil(){
        httpClient = HttpClientBuilder.create().build();
    }

    @Autowired
    private FastFileStorageClient fastFileStorageClient;

    @Autowired
    private AppendFileStorageClient appendFileStorageClient;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private HttpClient httpClient;

    private static final String PATH_KEY = "path-key:";

    private static final String UPLOADED_SIZE_KEY = "uploaded-size-key:";

    private static final String UPLOADED_NO_KEY = "uploaded-no-key:";

    private static final String DEFAULT_GROUP = "group1";

    private static final int SLICE_SIZE = 1024 * 1024 * 2;

    @Value("${fdfs.http.storage-addr}")
    private String httpFdfsStorageAddr;

    public String getFileType(MultipartFile file){
        if(file == null){
            throw new ConditionException("非法文件！");
        }
        String fileName = file.getOriginalFilename();
        int index = fileName.lastIndexOf(".");
        return fileName.substring(index+1);
    }

    //上传
    public String uploadCommonFile(MultipartFile file) throws Exception {
        Set<MetaData> metaDataSet = new HashSet<>();
        String fileType = this.getFileType(file);
        StorePath storePath = fastFileStorageClient.uploadFile(file.getInputStream(), file.getSize(), fileType, metaDataSet);
        return storePath.getPath();
    }

    //删除
    public void deleteFile(String filePath){
        fastFileStorageClient.deleteFile(filePath);
    }
    //上传可以断点续传的文件
    public String uploadAppenderFile(MultipartFile file) throws Exception{
        String fileType = this.getFileType(file);
        StorePath storePath = appendFileStorageClient.uploadAppenderFile(DEFAULT_GROUP, file.getInputStream(), file.getSize(), fileType);
        return storePath.getPath();
    }
    public void modifyAppenderFile(MultipartFile file, String filePath, long offset) throws Exception{
        appendFileStorageClient.modifyFile(DEFAULT_GROUP, filePath, file.getInputStream(), file.getSize(), offset);
    }

    public String uploadFileBySlices(MultipartFile file, String fileMd5, Integer sliceNo, Integer totalSliceNo) throws Exception {
        if(file == null || sliceNo == null || totalSliceNo == null){
            throw new ConditionException("参数异常！");
        }
        String pathKey = PATH_KEY + fileMd5;
        String uploadedSizeKey = UPLOADED_SIZE_KEY + fileMd5;
        String uploadedNoKey = UPLOADED_NO_KEY + fileMd5;
        String uploadedSizeStr = redisTemplate.opsForValue().get(uploadedSizeKey);
        Long uploadedSize = 0L;
        if(!StringUtil.isNullOrEmpty(uploadedSizeStr)){
            uploadedSize = Long.valueOf(uploadedSizeStr);
        }
        if(sliceNo == 1){ //上传的是第一个分片
            String path = this.uploadAppenderFile(file);
            if(StringUtil.isNullOrEmpty(path)){
                throw new ConditionException("上传失败！");
            }
            redisTemplate.opsForValue().set(pathKey, path);
            redisTemplate.opsForValue().set(uploadedNoKey, "1");
        }else{
            String filePath = redisTemplate.opsForValue().get(pathKey);
            if(StringUtil.isNullOrEmpty(filePath)){
                throw new ConditionException("上传失败！");
            }
            this.modifyAppenderFile(file, filePath,  uploadedSize);
            redisTemplate.opsForValue().increment(uploadedNoKey);
        }
        // 修改历史上传分片文件大小
        uploadedSize  += file.getSize();
        redisTemplate.opsForValue().set(uploadedSizeKey, String.valueOf(uploadedSize));
        //如果所有分片全部上传完毕，则清空redis里面相关的key和value
        String uploadedNoStr = redisTemplate.opsForValue().get(uploadedNoKey);
        Integer uploadedNo = Integer.valueOf(uploadedNoStr);
        String resultPath = "";
        if(uploadedNo.equals(totalSliceNo)){
            resultPath = redisTemplate.opsForValue().get(pathKey);
            List<String> keyList = Arrays.asList(uploadedNoKey, pathKey, uploadedSizeKey);
            redisTemplate.delete(keyList);
        }
        return resultPath;
    }

    public void convertFileToSlices(MultipartFile multipartFile) throws Exception{
        String fileType = this.getFileType(multipartFile);
        //生成临时文件，将MultipartFile转为File
        File file = this.multipartFileToFile(multipartFile);
        long fileLength = file.length();
        int count = 1;
        for(int i = 0; i < fileLength; i += SLICE_SIZE){
            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r");
            randomAccessFile.seek(i);
            byte[] bytes = new byte[SLICE_SIZE];
            int len = randomAccessFile.read(bytes);
            String path = "C:/Users/14272/Desktop/Bilibili/tempFile/" + count + "." + fileType;
            File slice = new File(path);
            FileOutputStream fos = new FileOutputStream(slice);
            fos.write(bytes, 0, len);
            fos.close();
            randomAccessFile.close();
            count++;
        }
        //删除临时文件
        file.delete();
    }

    public File multipartFileToFile(MultipartFile multipartFile) throws Exception{
        String originalFileName = multipartFile.getOriginalFilename();
        String[] fileName = originalFileName.split("\\.");
        File file = File.createTempFile(fileName[0], "." + fileName[1]);
        multipartFile.transferTo(file);
        return file;
    }

    public void viewVideoOnlineBySlices(HttpServletRequest request,
                                        HttpServletResponse response,
                                        String path) throws Exception{
        FileInfo fileInfo = fastFileStorageClient.queryFileInfo(DEFAULT_GROUP, path);
        long totalFileSize = fileInfo.getFileSize();
        String url = httpFdfsStorageAddr + path;
        Enumeration<String> headerNames = request.getHeaderNames();
        Map<String, Object> headers = new HashMap<>();
        while(headerNames.hasMoreElements()){
            String header = headerNames.nextElement();
            headers.put(header, request.getHeader(header));
        }
        String rangeStr = request.getHeader("Range");
        String[] range;
        if(StringUtil.isNullOrEmpty(rangeStr)){
            rangeStr = "bytes=0-" + (totalFileSize-1);
        }
        range = rangeStr.split("bytes=|-");
        long begin = 0;
        if(range.length >= 2){
            begin = Long.parseLong(range[1]);
        }
        long end = totalFileSize-1;
        if(range.length >= 3){
            end = Long.parseLong(range[2]);
        }
        long len = (end - begin) + 1;
        String contentRange = "bytes " + begin + "-" + end + "/" + totalFileSize;
        response.setHeader("Content-Range", contentRange);
        response.setHeader("Accept-Ranges", "bytes");
        response.setHeader("Content-Type", "video/mp4");
        response.setContentLength((int)len);
        response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
        HttpUtil.get(url, headers, response);
    }

    public void viewVideoOnlineBySlicesSimple(HttpServletRequest request, HttpServletResponse response, String url) throws Exception {
        HttpGet httpGet = new HttpGet(httpFdfsStorageAddr + url);
        HttpResponse execute = httpClient.execute(httpGet);
        HttpEntity entity = execute.getEntity();

        if (entity != null) {
            // 设置返回的内容类型为视频文件
            response.setContentType("video/mp4");
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                // 从 HttpEntity 中获取输入流
                inputStream = entity.getContent();
                // 获取响应输出流，用于返回数据给客户端
                outputStream = response.getOutputStream();
                // 定义缓冲区，逐块读取数据并传递到响应输出流
                byte[] buffer = new byte[1024];
                int bytesRead;
                // 循环读取输入流的数据，并写入输出流
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                // 将输出流中的数据刷新到客户端
                outputStream.flush();
            } catch (Exception e) {
                // 捕获并处理异常
                e.printStackTrace();
            } finally {
                // 关闭输入流，避免资源泄漏
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                // 关闭输出流，避免资源泄漏
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                // 确保 HttpEntity 资源被释放
                EntityUtils.consume(entity);
            }

        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Video not found.");
        }
    }

    public void viewImage(HttpServletRequest request, HttpServletResponse response, String url) throws Exception {
        // 创建 HTTP GET 请求，url 是图片的相对路径
        HttpGet httpGet = new HttpGet(httpFdfsStorageAddr + url);
        HttpResponse execute = httpClient.execute(httpGet);
        HttpEntity entity = execute.getEntity();

        if (entity != null) {
            // 设置返回的内容类型为图片文件（例如 JPEG 格式）
            response.setContentType("image/jpeg");  // 这里也可以改成 "image/png" 等其他格式

            InputStream inputStream = null;
            OutputStream outputStream = null;

            try {
                // 从 HttpEntity 中获取输入流
                inputStream = entity.getContent();
                // 获取响应输出流，用于返回数据给客户端
                outputStream = response.getOutputStream();
                // 定义缓冲区，逐块读取数据并传递到响应输出流
                byte[] buffer = new byte[1024];
                int bytesRead;

                // 循环读取输入流的数据，并写入输出流
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                // 将输出流中的数据刷新到客户端
                outputStream.flush();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                // 关闭输入流，避免资源泄漏
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                // 关闭输出流，避免资源泄漏
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                // 确保 HttpEntity 资源被释放
                EntityUtils.consume(entity);
            }

        } else {
            // 如果图片未找到，返回 404 错误
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Image not found.");
        }
    }

}
