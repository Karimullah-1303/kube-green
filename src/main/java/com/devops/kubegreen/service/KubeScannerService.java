package com.devops.kubegreen.service;

import io.kubernetes.client.custom.PodMetrics;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.extended.kubectl.Kubectl;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.Configuration;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaim;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1Volume;
import io.kubernetes.client.util.Config;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Service
public class KubeScannerService {

    private final CoreV1Api api;

    public KubeScannerService() throws IOException {
        ApiClient client = Config.defaultClient();
        Configuration.setDefaultApiClient(client);
        this.api = new CoreV1Api();
    }

    public void runFullAudit() {
        System.out.println("\n🌱 GREEN KUBE AUDIT REPORT");
        System.out.println("================================================");
        auditCompute();
        auditStorage();
        System.out.println("================================================");
    }

    private void auditCompute() {
        try {
            System.out.println("--- 🧠 COMPUTE AUDIT (CPU + RAM) ---");
            var podMetricsPairs = Kubectl.top(V1Pod.class, PodMetrics.class)
                    .namespace("simulation")
                    .execute();

            double totalClusterSavings = 0.0;

            for (Pair<V1Pod, PodMetrics> pair : podMetricsPairs) {
                V1Pod pod = pair.getLeft();
                PodMetrics metrics = pair.getRight();
                String podName = pod.getMetadata().getName();

                // --- CPU CALC ---
                double cpuReq = getCpuRequest(pod);
                if(cpuReq == 0.0){
                    System.out.println("Warning: Pod" + podName + " has no cpu limits set");
                    cpuReq = 0.1;
                }
                double cpuUsed = 0.0;
                if(metrics!=null&&metrics.getContainers()!=null){
                    cpuUsed=getCpuUsage(metrics);
                }
                double cpuWaste = cpuReq - cpuUsed;
                // CPU Cost: ~$0.0315 per Core/Hour (approx $23/month per core)
                double cpuCost = (cpuWaste > 0) ? (cpuWaste / 1000.0) * 0.0315 * 730 : 0.0;

                // --- RAM CALC ---
                double ramReq = getRamRequest(pod);
                double ramUsed = getRamUsage(metrics);
                double ramWaste = ramReq - ramUsed; // in Bytes
                double ramCost = calculateRamMonthlyCost(ramWaste);

                // --- TOTAL ---
                double totalPodWaste = cpuCost + ramCost;
                totalClusterSavings += totalPodWaste;

                String status = "✅ OPTIMIZED";
                if (totalPodWaste > 5.0) status = "⚠️  HIGH WASTE";
                else if (totalPodWaste > 1.0) status = "⚠️  WASTE";

                // Print only if there is significant data or waste
                if (totalPodWaste > 0.1 || cpuReq > 0 || ramReq > 0) {
                    System.out.printf("Pod: %-18s | CPU Waste: $%.2f | RAM Waste: $%.2f | Total: $%.2f/mo | %s%n",
                            podName, cpuCost, ramCost, totalPodWaste, status);
                }
            }
            System.out.printf("\n💰 TOTAL POTENTIAL SAVINGS: $%.2f / Month%n", totalClusterSavings);

        } catch (Exception e) {
            System.out.println("Error");
        }
    }

    private void auditStorage() {
        try {
            System.out.println("\n--- 💾 STORAGE (PVCs) ---");
            var pvcList = api.listNamespacedPersistentVolumeClaim("simulation").limit(100).execute();
            var podList = api.listNamespacedPod("simulation").limit(100).execute();

            Set<String> activeClaims = new HashSet<>();
            for (V1Pod pod : podList.getItems()) {
                if (pod.getSpec().getVolumes() != null) {
                    for (V1Volume v : pod.getSpec().getVolumes()) {
                        if (v.getPersistentVolumeClaim() != null) {
                            activeClaims.add(v.getPersistentVolumeClaim().getClaimName());
                        }
                    }
                }
            }

            boolean foundWaste = false;
            for (V1PersistentVolumeClaim pvc : pvcList.getItems()) {
                String pvcName = pvc.getMetadata().getName();
                if (!activeClaims.contains(pvcName)) {
                    String size = pvc.getSpec().getResources().getRequests().get("storage").toSuffixedString();
                    System.out.printf("❌ ORPHAN FOUND: PVC '%s' (%s) is Unused! Cost: ~$0.20/mo%n", pvcName, size);
                    foundWaste = true;
                }
            }
            if (!foundWaste) System.out.println("No storage waste found.");

        } catch (Exception e) {
            System.err.println("Storage audit failed: " + e.getMessage());
        }
    }

    // ================= HELPER METHODS =================

    private double getCpuRequest(V1Pod pod) {
        try {
            if (pod.getSpec().getContainers().isEmpty()) return 0.0;
            var container = pod.getSpec().getContainers().get(0);
            if (container.getResources() == null || container.getResources().getRequests() == null) return 0.0;
            Quantity q = container.getResources().getRequests().get("cpu");
            return (q != null) ? q.getNumber().doubleValue() * 1000 : 0.0;
        } catch (Exception e) { return 0.0; }
    }

    private double getCpuUsage(PodMetrics metrics) {
        try {
            if (metrics.getContainers().isEmpty()) return 0.0;
            Quantity q = metrics.getContainers().get(0).getUsage().get("cpu");
            return (q != null) ? q.getNumber().doubleValue() * 1000 : 0.0;
        } catch (Exception e) { return 0.0; }
    }

    private double getRamRequest(V1Pod pod) {
        try {
            if (pod.getSpec().getContainers().isEmpty()) return 0.0;
            var container = pod.getSpec().getContainers().get(0);
            if (container.getResources() == null || container.getResources().getRequests() == null) return 0.0;
            Quantity q = container.getResources().getRequests().get("memory");
            return (q != null) ? q.getNumber().doubleValue() : 0.0;
        } catch (Exception e) { return 0.0; }
    }

    private double getRamUsage(PodMetrics metrics) {
        try {
            if (metrics.getContainers().isEmpty()) return 0.0;
            Quantity q = metrics.getContainers().get(0).getUsage().get("memory");
            return (q != null) ? q.getNumber().doubleValue() : 0.0;
        } catch (Exception e) { return 0.0; }
    }

    private double calculateRamMonthlyCost(double wasteBytes) {
        if (wasteBytes <= 0) return 0.0;
        // Convert Bytes -> GB
        double gb = wasteBytes / (1024.0 * 1024.0 * 1024.0);
        // Price: $0.004 per GB/Hour * 730 Hours (Approx $2.92/GB/Month)
        return gb * 0.004 * 730;
    }
}