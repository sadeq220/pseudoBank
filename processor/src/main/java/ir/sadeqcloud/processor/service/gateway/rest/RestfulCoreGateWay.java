package ir.sadeqcloud.processor.service.gateway.rest;

import ir.sadeqcloud.processor.service.gateway.CoreGateway;
import ir.sadeqcloud.processor.service.gateway.dto.IssueRequest;
import ir.sadeqcloud.processor.service.gateway.dto.TrackIssueDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class RestfulCoreGateWay implements CoreGateway {
    private RestTemplate restTemplate;
    @Autowired
    public RestfulCoreGateWay(RestTemplate restTemplate){
        this.restTemplate=restTemplate;
    }
    @Override
    public void reverseWithdraw() {

    }

    @Override
    public TrackIssueDTO issueDocument(IssueRequest issueRequest) {
        HttpHeaders httpHeaders = defaultHttpHeaders(issueRequest.getCorrelationId());
        return null;
    }

    @Override
    public TrackIssueDTO trackStatus(String trackNo) {
        return null;
    }
    private HttpHeaders defaultHttpHeaders(String correlationId){
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("correlation-id",correlationId);
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        return httpHeaders;
    }
}
