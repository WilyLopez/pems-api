package com.playzone.pems.infrastructure.storage;

import com.playzone.pems.domain.storage.StoragePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class SupabaseStorageAdapter implements StoragePort {

    private final S3Client s3Client;

    @Value("${supabase.storage.url-publica}")
    private String urlPublica;

    @Override
    public String upload(String bucket, String key, byte[] contenido, String contentType) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .contentLength((long) contenido.length)
                .build();
        s3Client.putObject(request, RequestBody.fromBytes(contenido));
        String url = getUrlPublica(bucket, key);
        log.info("Archivo subido al storage: {}", url);
        return url;
    }

    @Override
    public void delete(String bucket, String key) {
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
            log.info("Archivo eliminado del storage: {}/{}", bucket, key);
        } catch (Exception e) {
            log.warn("No se pudo eliminar {}/{}: {}", bucket, key, e.getMessage());
        }
    }

    @Override
    public void deleteByUrl(String url) {
        try {
            String[] bucketYKey = parseBucketYKey(url);
            delete(bucketYKey[0], bucketYKey[1]);
        } catch (Exception e) {
            log.warn("No se pudo eliminar por URL {}: {}", url, e.getMessage());
        }
    }

    @Override
    public String getUrlPublica(String bucket, String key) {
        String base = urlPublica.endsWith("/") ? urlPublica : urlPublica + "/";
        return base + bucket + "/" + key;
    }

    @Override
    public byte[] downloadByUrl(String url) {
        String[] bucketYKey = parseBucketYKey(url);
        return s3Client.getObjectAsBytes(GetObjectRequest.builder()
                        .bucket(bucketYKey[0])
                        .key(bucketYKey[1])
                        .build())
                .asByteArray();
    }

    private String[] parseBucketYKey(String url) {
        String base = urlPublica.endsWith("/") ? urlPublica : urlPublica + "/";
        String bucketAndKey = url.startsWith(base)
                ? url.substring(base.length())
                : url.substring(url.indexOf("/object/public/") + "/object/public/".length());
        int slash = bucketAndKey.indexOf('/');
        return new String[]{bucketAndKey.substring(0, slash), bucketAndKey.substring(slash + 1)};
    }
}
