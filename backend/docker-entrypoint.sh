#!/bin/sh
set -eu

if [ -n "${DATABASE_URL:-}" ]; then
  case "$DATABASE_URL" in
    postgres://*|postgresql://*)
      jdbc=$(printf '%s' "$DATABASE_URL" | sed -e 's#^postgres://#jdbc:postgresql://#' -e 's#^postgresql://#jdbc:postgresql://#')
      export SPRING_DATASOURCE_URL="$jdbc"
      ;;
    jdbc:*)
      export SPRING_DATASOURCE_URL="$DATABASE_URL"
      ;;
  esac
fi

exec java -jar /app/app.jar
