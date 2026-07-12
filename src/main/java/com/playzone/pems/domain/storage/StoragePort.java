package com.playzone.pems.domain.storage;

public interface StoragePort {
    String upload(String bucket, String key, byte[] contenido, String contentType);
    void delete(String bucket, String key);
    void deleteByUrl(String url);
    String getUrlPublica(String bucket, String key);
    byte[] downloadByUrl(String url);
}
