# service-merge-validate
A service to validate and merge pull requests from GitHub Actions

![CI Build](https://github.com/HardNorth/service-merge-validate/workflows/CI%20Build/badge.svg)

### Create service account
With:
Cloud Datastore User
Secret Manager Secret Accessor
Secret Manager Secret Version Manager
Secret Manager Viewer

### Build
```none
./mvnw package
```

### Deploy
```none
gcloud beta functions deploy function-1 \ 
  --entry-point=io.quarkus.gcp.functions.http.QuarkusHttpFunction \ 
  --runtime=java11 --trigger-http --source=target/deployment --allow-unauthenticated \
  --service-account={your_service_account}
```

### Set URLs in Github app
- Homepage
- Callback
