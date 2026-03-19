package com.rcs.system.service;

import com.rcs.system.model.PickingOrder;
import com.rcs.system.repository.PickingOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CompletableFuture;
import com.rcs.system.wes.WesSocketService;

@Service
public class OrderProcessingService {

    @Autowired
    private PickingOrderRepository orderRepository;

    @Autowired
    private WesSocketService wesSocketService;

    /**
     * 非同步處理來自 WES 的訂單
     * 採用高併發執行緒池，確保主執行緒立即釋放
     */
    @Async("taskExecutor")
    @Transactional
    public CompletableFuture<Void> processWesOrderAsync(PickingOrder order) {
        try {
            // 1. 模擬工業邏輯驗證與排隊分配
            // 在實際場景中，這裡會進行機器人調度算法
            Thread.sleep(100); // 模擬極速處理
            
            order.setCommandStatus("Processing");
            order.setJobStatus("Sent to AccuPick");
            orderRepository.save(order);
            
            // 2. 這裡可再串 AccuPick
            // notifyAccuPick(order);
            
            System.out.println("[高併發] 訂單 " + order.getJobNo() + " 已成功進入處理隊列並通知機器人");
            
        } catch (Exception e) {
            System.err.println("處理 WES 訂單失敗: " + e.getMessage());
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 非同步處理來自 AccuPick 的結果
     * 確保機器人回傳結果時，系統能快速響應並更新狀態
     */
    @Async("taskExecutor")
    @Transactional
    public void handleAccupickResultAsync(String jobNo, String status, String ngCode, String comment, String imageUrl) {
        orderRepository.findByJobNo(jobNo).ifPresent(order -> {
            order.setCommandStatus(status);
            order.setNgCode(ngCode);
            order.setComment(comment);
            order.setImageUrl(imageUrl);
            order.setJobStatus("AccuPick Finished");
            orderRepository.save(order);
            
            // 非同步回傳結果給 WES Socket
            try {
                wesSocketService.sendResultToWes(order);
            } catch (Exception e) {
                System.err.println("回傳 WES Socket 失敗: " + e.getMessage());
            }

            System.out.println("[高併發] 機器人已回傳結果，訂單 " + jobNo + " 狀態更新為: " + status);
        });
    }
}
