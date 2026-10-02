# astrodc

The active project is this monorepo: frontend/ (Vue 2), backend/ (Java, including Agent), docs/, scripts/.
Use this root explicitly for development and Git operations.
Run npm run check:health to check local backend, public Pages and Cloudflare Tunnel.
Cloudflare uploads are paused in deployment.json; only resume deployment when the user requests it.
Do not commit backend/config/application-private.yml, tunnel credentials, or .runtime logs.
