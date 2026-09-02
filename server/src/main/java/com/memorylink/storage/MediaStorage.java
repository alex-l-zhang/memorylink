package com.memorylink.storage;

import java.io.InputStream;

public interface MediaStorage {

    void put(String objectKey, InputStream in, long size, String contentType);

    String presignedGetUrl(String objectKey);

    void delete(String objectKey);
}
