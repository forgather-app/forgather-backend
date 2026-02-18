package com.forgather.domain.upload.repository;

import com.forgather.domain.upload.domain.DeletionFailLog;

public interface DeletionFailLogRepository {

    DeletionFailLog save(DeletionFailLog deletionFailLog);
}
