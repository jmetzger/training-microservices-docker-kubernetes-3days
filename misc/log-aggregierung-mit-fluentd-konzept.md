# Logging

## Prinzipien (einfachste) 

  * Alle Logs nach /dev/stdout bzw. /dev/stderr schreiben
  * Oftmals ist das so gelöst, auf ein symbolischer Link auf ein File gesetzt
  * z.B. in nginx 


<img width="981" height="185" alt="image" src="https://github.com/user-attachments/assets/98e6228b-bbdd-45c0-8bd7-3acfcccfc554" />


## Wie landet das in der Log-Aggregierung  (z.B. EFK - Stack) 

  * EFK (Elastiksearch, Fluentd, Kibana
  * Loki (Grafanalabs)
  * Also DaemonSet (fluend installieren)
  * Schreibt alle Logs aus /var/log/container <- hier sind die Logs aller container zu einem endpunkt z.B. elasticsearch
  * Man installiert und konfiguriert den Endpunkt https://artifacthub.io/packages/helm/fluent-bit-collector/fluent-bit-collector
 
