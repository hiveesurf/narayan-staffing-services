#!/bin/sh
set -eu

mkdir -p "${FILE_STORAGE_PATH:-/data/storage}"

if [ -n "${DATABASE_URL:-}" ]; then
  case "$DATABASE_URL" in
    postgres://*|postgresql://*)
      without_scheme=${DATABASE_URL#*://}
      userinfo=${without_scheme%%@*}
      hostpart=${without_scheme#*@}
      if [ "$userinfo" != "$without_scheme" ]; then
        user=${userinfo%%:*}
        pass=${userinfo#*:}
        export SPRING_DATASOURCE_USERNAME="$user"
        export SPRING_DATASOURCE_PASSWORD="$pass"
        export SPRING_DATASOURCE_URL="jdbc:postgresql://${hostpart}"
      else
        export SPRING_DATASOURCE_URL="jdbc:postgresql://${without_scheme}"
      fi
      ;;
    jdbc:*)
      export SPRING_DATASOURCE_URL="$DATABASE_URL"
      ;;
  esac
fi

exec java -jar /app/app.jar
