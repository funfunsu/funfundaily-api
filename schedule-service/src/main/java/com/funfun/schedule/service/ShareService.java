package com.funfun.schedule.service;

import com.funfun.schedule.entity.ShareRecord;
import com.funfun.schedule.repository.ShareRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class ShareService {

    @Autowired
    private ShareRecordRepository shareRecordRepository;

    @Transactional
    public String createShare(Long creatorId, String sceneCode,String content, int expireHours) {
        ShareRecord record = new ShareRecord();
        record.setToken(generateToken());
        record.setSceneCode(sceneCode);
        record.setContent(content);
        record.setCreatorId(creatorId);
        record.setCreatedAt(LocalDateTime.now());
        record.setExpiresAt(LocalDateTime.now().plusHours(expireHours));

        shareRecordRepository.save(record);
        return record.getToken();
    }

    public Optional<ShareRecord> getShareByToken(String token) {
        Optional<ShareRecord> record = shareRecordRepository.findByToken(token);
        if (record.isPresent() && record.get().getExpiresAt().isBefore(LocalDateTime.now())) {
            // 已过期，可选择自动删除或返回空
            shareRecordRepository.delete(record.get());
            return Optional.empty();
        }
        return record;
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}