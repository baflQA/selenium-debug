#!/bin/bash

docker stop selenium-standalone
docker rm selenium-standalone

IMAGE_VERSION='4.13'
cat <<EOF >config.toml
[docker]
configs = [
    "selenium/standalone-firefox:${IMAGE_VERSION}", '{"browserName": "firefox"}',
    "selenium/standalone-chrome:${IMAGE_VERSION}", '{"browserName": "chrome"}'
]
url = "http://127.0.0.1:2375"
[node]
drivers = ["chrome", "firefox"]
max-sessions = 4
session-timeout = 30
grid-url = "http://localhost:4444"
selenium-manager = true
enable-managed-downloads = true
[sessionqueue]
session-request-timeout = 10
EOF

docker network create grid
docker run -d -p 4444:4444 --shm-size="2g" \
  --net grid \
  --name selenium-standalone \
  --health-cmd='/opt/bin/check-grid.sh --host 0.0.0.0 --port 4444' \
  --health-interval=15s \
  --health-timeout=30s \
  --health-retries=5 \
  -e SE_SCREEN_WIDTH=1920 \
  -e SE_SCREEN_HEIGHT=1080 \
  -e SE_VNC_NO_PASSWORD=1 \
  -e SE_START_VNC=true \
  -v "${PWD}"/config.toml:/opt/bin/config.toml \
  -v "${PWD}"/assets:/opt/selenium/assets \
  -v /var/run/docker.sock:/var/run/docker.sock \
  selenium/standalone-docker:${IMAGE_VERSION}

#waiting for grid to be ready
set -e
url="http://localhost:4444/wd/hub/status"
wait_interval=1
max_wait_time=45
end_time=$((SECONDS + max_wait_time))
time_left=$max_wait_time

while [ $SECONDS -lt $end_time ]; do
    response=$(curl -sL "$url" | jq -r '.value.ready')
    if [ -n "$response"  ]  && [ "$response" ]; then
        echo "Selenium Grid is up - executing tests"
        break
    else
        echo "Waiting for the Grid. Sleeping for $wait_interval second(s). $time_left seconds left until timeout."
        sleep $wait_interval
        time_left=$((time_left - wait_interval))
    fi
done

if [ $SECONDS -ge $end_time ]; then
    echo "Timeout: The Grid was not started within $max_wait_time seconds."
    exit 1
fi