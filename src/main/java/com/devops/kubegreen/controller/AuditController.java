package com.devops.kubegreen.controller;

import com.devops.kubegreen.service.KubeScannerService;
import lombok.experimental.Accessors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditController {


    private final KubeScannerService kubeScannerService;

    public  AuditController(KubeScannerService kubeScannerService) {
        this.kubeScannerService = kubeScannerService;
    }

    @GetMapping("/audit")
    public String triggerAudit(){
        kubeScannerService.runFullAudit();
        return "Audit triggered! Check your server for logs.";
        }


}
