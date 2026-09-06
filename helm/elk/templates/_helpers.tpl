{{- define "elk.logstash.fullname" -}}
{{- if .Values.logstash.fullnameOverride -}}
{{- .Values.logstash.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default "logstash" .Values.logstash.nameOverride -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{- define "elk.elasticsearch.fullname" -}}
{{- if .Values.elasticsearch.fullnameOverride -}}
{{- .Values.elasticsearch.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name "elasticsearch" | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{- define "elk.kibana.fullname" -}}
{{- if .Values.kibana.fullnameOverride -}}
{{- .Values.kibana.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name "kibana" | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{- define "elk.logstash.selectorLabels" -}}
app.kubernetes.io/name: logstash
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "elk.elasticsearch.selectorLabels" -}}
app.kubernetes.io/name: elasticsearch
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "elk.kibana.selectorLabels" -}}
app.kubernetes.io/name: kibana
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{- define "elk.logstash.labels" -}}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version | replace "+" "_" }}
{{ include "elk.logstash.selectorLabels" . }}
app.kubernetes.io/version: {{ .Values.logstash.image.tag | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{- define "elk.elasticsearch.labels" -}}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version | replace "+" "_" }}
{{ include "elk.elasticsearch.selectorLabels" . }}
app.kubernetes.io/version: {{ .Values.elasticsearch.image.tag | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{- define "elk.kibana.labels" -}}
helm.sh/chart: {{ .Chart.Name }}-{{ .Chart.Version | replace "+" "_" }}
{{ include "elk.kibana.selectorLabels" . }}
app.kubernetes.io/version: {{ .Values.kibana.image.tag | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}