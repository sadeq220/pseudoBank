package ir.sadeqcloud.processor.service.gateway.rest;

import ir.sadeqcloud.processor.constants.PropertyConstants;
import ir.sadeqcloud.processor.service.gateway.CoreGateway;
import ir.sadeqcloud.processor.service.gateway.dto.IssueRequest;
import ir.sadeqcloud.processor.service.gateway.dto.TrackIssueDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

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
        UriComponents uriComponents = UriComponentsBuilder.fromHttpUrl(PropertyConstants.getCoreBankAddress()).build();
        RequestEntity requestEntity = new RequestEntity(issueRequest, httpHeaders , HttpMethod.PUT , uriComponents.toUri());
        ResponseEntity<TrackIssueDTO> responseEntity = restTemplate.exchange(requestEntity, TrackIssueDTO.class);
        return responseEntity.getBody();
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
