terraform {
    required_providers {
        kubernetes = {
            source = "hashicorp/kubernetes"
            version = "2.23.0"
            }
        }
    }

provider "kubernetes" {

  config_path    = "C:/Users/777sh/.kube/config"

  config_context = "minikube"
}
