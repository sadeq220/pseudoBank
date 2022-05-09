package ir.sadeqcloud.processor.service.gateway;

import ir.sadeqcloud.processor.service.gateway.dto.IssueRequest;
import ir.sadeqcloud.processor.service.gateway.dto.TrackIssueDTO;

/**
 * common base class to request core module
 * it could be implemented with multiple protocols
 */
public interface CoreGateway {
    TrackIssueDTO reverseWithdraw(String trackNo);
    TrackIssueDTO issueDocument(IssueRequest issueRequest);
    TrackIssueDTO trackStatus(String trackNo);
}
