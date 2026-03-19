package com.rcs.system.accupick;

import com.rcs.system.accupick.event.AccupickAckReceivedEvent;
import com.rcs.system.service.OrderCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccupickAckEventListener {

    private final OrderCommandService orderCommandService;

    @Async
    @EventListener
    public void onAckReceived(AccupickAckReceivedEvent event) {
        try {
            orderCommandService.handleAsyncAck(event.ack());
        } catch (Exception e) {
            log.error("Failed to process async AccuPick ACK: {}", event.ack().getRawMessage(), e);
        }
    }
}
