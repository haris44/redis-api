package k8s.local.dto

case class KubernetesMember(address: String, status: String)

case class KubernetesMembers(kubernetesMember: List[KubernetesMember])