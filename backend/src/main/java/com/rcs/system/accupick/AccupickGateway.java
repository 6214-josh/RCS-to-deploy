package com.rcs.system.accupick;

public interface AccupickGateway {
    AccupickAck sendCommand(String payload, int timeoutMs);
    String mode();
}
