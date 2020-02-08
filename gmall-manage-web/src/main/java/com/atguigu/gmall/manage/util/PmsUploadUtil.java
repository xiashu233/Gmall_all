package com.atguigu.gmall.manage.util;

import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class PmsUploadUtil {

    public String uploadImage(MultipartFile multipartFile){
    // 获得配置文件路径
        String tracker = PmsUploadUtil.class.getResource("/tracker.conf").getPath();
        String url = "http://192.168.87.3";

        try{
            ClientGlobal.init(tracker);
            TrackerClient trackerClient =  new TrackerClient();

            TrackerServer trackerServer = trackerClient.getTrackerServer();

            StorageClient storageClient = new StorageClient(trackerServer,null);
            byte[] bytes = multipartFile.getBytes();
            String path = multipartFile.getOriginalFilename();

            String extName = path.substring(path.lastIndexOf('.')+1);

            String[] uploadInfos = storageClient.upload_file(bytes, extName, null);

            for (String uploadInfo : uploadInfos) {
                url +=  "/" + uploadInfo;
            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return url;
    }
}
