#!/usr/bin/env bash
cd "$(dirname "$0")"
source ./env.sh
exec ./gradle-8.7/bin/gradle "$@"
