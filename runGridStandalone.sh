set +e

docker stop selenium-standalone
docker rm selenium-standalone

IMAGE_VERSION='4.34'

cat <<EOF >config.toml
[docker]
configs = [
"selenium/standalone-chrome:${IMAGE_VERSION}", '{"browserName": "chrome"}'
]
url = "http://127.0.0.1:2375"
[node]
drivers = ["chrome"]
max-sessions = 4
session-timeout = 120
grid-url = "http://localhost:4444"
selenium-manager = true
enable-cdp = true
connection-limit-per-session = 1000
[sessionqueue]
session-request-timeout = 120
[router]
disable-ui = false
EOF

docker network create grid

set -e
until
  docker run -d -p 4444:4444 --shm-size="2g" \
    --net grid \
    --name selenium-standalone \
    --health-cmd='/opt/bin/check-grid.sh --host 0.0.0.0 --port 4444' \
    --health-interval=15s \
    --health-timeout=30s \
    --health-retries=5 \
    -e SE_SCREEN_WIDTH=2048 \
    -e SE_SCREEN_HEIGHT=1280 \
    -e SE_VNC_NO_PASSWORD=1 \
    -e SE_START_VNC=true \
    -e SE_ENABLE_TRACING=false \
    -e SE_OPTS='--enable-managed-downloads true' \
    -v "${PWD}"/config.toml:/opt/selenium/config.toml \
    -v "${PWD}"/config.toml:/opt/selenium/docker.toml \
    -v "${PWD}"/assets:/opt/selenium/assets \
    -v /var/run/docker.sock:/var/run/docker.sock \
    selenium/standalone-docker:${IMAGE_VERSION}
do
  echo "Failed to start grid with version ${IMAGE_VERSION}. Retrying with latest version..."
  IMAGE_VERSION="latest"
done
