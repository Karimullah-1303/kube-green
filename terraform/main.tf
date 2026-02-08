resource "kubernetes_namespace" "dev" {
    metadata {
        name = "kube-green-dev"
        }
    }

resource "kubernetes_namespace" "prod"{
    metadata{
        name = "kube-green-prod"
        }
    }
resource "kubernetes_resource_quota" "dev_quota" {
    metadata{
        name = "dev-cpu-limit"
        namespace = kubernetes_namespace.dev.metadata[0].name
        }
    spec {
        hard = {
            "requests.cpu" = "1"
            "requests.memory" = "1Gi"
            "pods" = "5"
            }
        }
    }
