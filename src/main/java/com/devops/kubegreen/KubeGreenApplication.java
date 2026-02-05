package com.devops.kubegreen;

import io.kubernetes.client.spring.extended.controller.config.KubernetesReconcilerAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {KubernetesReconcilerAutoConfiguration.class})
public class KubeGreenApplication {

    public static void main(String[] args) {
        SpringApplication.run(KubeGreenApplication.class, args);
    }

}