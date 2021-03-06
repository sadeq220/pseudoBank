package ir.sadeqcloud.gateway.controller;

import ir.sadeqcloud.gateway.awareClasses.IoCContainerUtil;
import ir.sadeqcloud.gateway.controller.dto.ReverseDTO;
import ir.sadeqcloud.gateway.controller.dto.WithdrawTransferDTO;
import ir.sadeqcloud.gateway.model.client.ClientResponse;
import ir.sadeqcloud.gateway.service.PublishTransferRequest;
import ir.sadeqcloud.gateway.sharedResource.IntermediaryObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class TrRqController {
    private PublishTransferRequest publishTransferRequest;
    @Autowired
    public TrRqController(PublishTransferRequest publishTransferRequest){
        this.publishTransferRequest=publishTransferRequest;
    }
    @PostMapping("transfer/withdraw")
    public ResponseEntity processWithdraw(@RequestBody WithdrawTransferDTO withdrawTransferDTO){
        ClientResponse clientResponse = publishTransferRequest.publishTransferMessage(withdrawTransferDTO.buildModel());
        return ResponseEntity.ok(clientResponse);
    }
    @PostMapping("transfer/reverse")
    public ResponseEntity processReverse(@RequestBody ReverseDTO reverseDTO){
        publishTransferRequest.publishTransferMessage(reverseDTO.buildModel());
        return ResponseEntity.ok(null);
    }
}
