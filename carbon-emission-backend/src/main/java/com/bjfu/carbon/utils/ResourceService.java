package com.bjfu.carbon.utils;

import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Describe:
 * Author：Zhang chengliang
 * Time：2023/9/28
 */
@Service
public class ResourceService {
    private final ResourceLoader resourceLoader;

    public ResourceService(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public String getResourceFolderPath(String folderName) {
        try {
            Resource resource = resourceLoader.getResource("classpath:" + folderName);
            return resource.getFile().getAbsolutePath();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
