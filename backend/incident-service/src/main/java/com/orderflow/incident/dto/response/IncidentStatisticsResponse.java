package com.orderflow.incident.dto.response;

public class IncidentStatisticsResponse {
    private long total;
    private long open;
    private long investigating;
    private long mitigating;
    private long resolved;
    private long closed;
    private long critical;
    private long high;
    private long medium;
    private long low;
    private long remediationAttempts;

    public IncidentStatisticsResponse() {}

    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public long getOpen() { return open; }
    public void setOpen(long open) { this.open = open; }
    public long getInvestigating() { return investigating; }
    public void setInvestigating(long investigating) { this.investigating = investigating; }
    public long getMitigating() { return mitigating; }
    public void setMitigating(long mitigating) { this.mitigating = mitigating; }
    public long getResolved() { return resolved; }
    public void setResolved(long resolved) { this.resolved = resolved; }
    public long getClosed() { return closed; }
    public void setClosed(long closed) { this.closed = closed; }
    public long getCritical() { return critical; }
    public void setCritical(long critical) { this.critical = critical; }
    public long getHigh() { return high; }
    public void setHigh(long high) { this.high = high; }
    public long getMedium() { return medium; }
    public void setMedium(long medium) { this.medium = medium; }
    public long getLow() { return low; }
    public void setLow(long low) { this.low = low; }
    public long getRemediationAttempts() { return remediationAttempts; }
    public void setRemediationAttempts(long remediationAttempts) { this.remediationAttempts = remediationAttempts; }
}
