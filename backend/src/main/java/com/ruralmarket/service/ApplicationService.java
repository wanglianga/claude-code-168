package com.ruralmarket.service;

import com.ruralmarket.common.ApiException;
import com.ruralmarket.domain.*;
import com.ruralmarket.repo.ApplicationRepository;
import com.ruralmarket.repo.FairDayRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final FairDayRepository fairDayRepository;

    public ApplicationService(ApplicationRepository applicationRepository, FairDayRepository fairDayRepository) {
        this.applicationRepository = applicationRepository;
        this.fairDayRepository = fairDayRepository;
    }

    @Transactional
    public Application submit(Long fairDayId, Application payload, SysUser vendor) {
        FairDay day = fairDayRepository.findById(fairDayId)
                .orElseThrow(() -> new ApiException("赶集日不存在"));
        if (day.getStatus() != DayStatus.OPEN && day.getStatus() != DayStatus.PLANNED) {
            throw new ApiException("当场集市报名已截止（当前状态：" + day.getStatus().getLabel() + "）");
        }
        if (day.getSignupDeadline() != null && LocalDateTime.now().isAfter(day.getSignupDeadline())) {
            throw new ApiException("已超过报名截止时间");
        }

        Application app = applicationRepository.findByFairDayIdAndVendorId(fairDayId, vendor.getId())
                .orElseGet(Application::new);
        if (app.getId() != null && app.getStatus() == ApplyStatus.APPROVED) {
            throw new ApiException("已审核通过的报名不能修改");
        }

        app.setFairDay(day);
        app.setVendor(vendor);
        app.setCategory(payload.getCategory());
        app.setGoodsDesc(payload.getGoodsDesc());
        app.setCookedFood(Boolean.TRUE.equals(payload.getCookedFood()));
        app.setLivePoultry(Boolean.TRUE.equals(payload.getLivePoultry()));
        app.setVehicleLength(payload.getVehicleLength());
        app.setVehicleWidth(payload.getVehicleWidth());
        app.setNeedElectricity(Boolean.TRUE.equals(payload.getNeedElectricity()));
        app.setPowerWatt(payload.getPowerWatt() == null ? 0 : payload.getPowerWatt());
        app.setLicensePhotos(payload.getLicensePhotos());
        app.setHistoryRecord(payload.getHistoryRecord());
        if (payload.getHistoryCount() != null) {
            app.setHistoryCount(payload.getHistoryCount());
        }
        app.setStatus(ApplyStatus.SUBMITTED);
        app.setRejectReason(null);
        app.setSubmittedAt(LocalDateTime.now());
        return applicationRepository.save(app);
    }

    @Transactional
    public Application review(Long applicationId, boolean approved, String note,
                              Integer historyCount, SysUser reviewer) {
        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ApiException("报名不存在"));
        if (app.getStatus() != ApplyStatus.SUBMITTED) {
            throw new ApiException("该报名已审核");
        }
        if (approved && Boolean.TRUE.equals(app.getCookedFood())
                && (app.getLicensePhotos() == null || app.getLicensePhotos().isBlank())) {
            throw new ApiException("熟食类摊主必须上传证照（健康证/备案）照片，无法通过；如不符合条件请驳回");
        }
        if (approved) {
            app.setStatus(ApplyStatus.APPROVED);
            app.setRejectReason(null);
            if (historyCount != null) {
                // 管理人员核实后的历史赶集次数，作为老摊主优先依据
                app.setHistoryCount(Math.max(0, historyCount));
            }
        } else {
            app.setStatus(ApplyStatus.REJECTED);
            app.setRejectReason(note == null ? "不符合报名条件" : note);
        }
        app.setReviewer(reviewer);
        app.setReviewedAt(LocalDateTime.now());
        app.setReviewNote(note);
        return applicationRepository.save(app);
    }
}
