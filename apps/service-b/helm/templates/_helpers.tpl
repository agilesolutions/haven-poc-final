{{- define "app.name" -}}
{{ .Chart.Name }}
{{- end }}

{{- define "app.fullname" -}}
{{ .Release.Name }}
{{- end }}