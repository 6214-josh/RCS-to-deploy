package com.rcs.system.accupick;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class AccupickGatewayRouter implements AccupickGateway {

    private final TcpAccupickGateway tcpAccupickGateway;
    private final MockAccupickGateway mockAccupickGateway;

    @Value("${rcs.accupick.mock-enabled:true}")
    private boolean mockEnabled;

    public AccupickGatewayRouter(TcpAccupickGateway tcpAccupickGateway, MockAccupickGateway mockAccupickGateway) {
        this.tcpAccupickGateway = tcpAccupickGateway;
        this.mockAccupickGateway = mockAccupickGateway;
    }

    @Override
    public AccupickAck sendCommand(String payload, int timeoutMs) {
        return mockEnabled ? mockAccupickGateway.sendCommand(payload, timeoutMs) : tcpAccupickGateway.sendCommand(payload, timeoutMs);
    }

    @Override
    public String mode() {
        return mockEnabled ? mockAccupickGateway.mode() : tcpAccupickGateway.mode();
    }
}
