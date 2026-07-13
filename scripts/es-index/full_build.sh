#!/usr/bin/env bash
#
# 一次性全量构建 ES 索引）。
#
# 思路：note/user/count 三张表同在一个 PostgreSQL 库（hannote），因此用一条 JOIN SQL
# 即可取出索引所需全部字段，无需 logstash。流程：
#   PostgreSQL --(docker psql, 输出 json_agg 数组)--> jq 拼 _bulk NDJSON --> ES /_bulk
#
# 为什么用 docker psql：本机无 psql/pip/psycopg，而 postgres 官方镜像自带 psql，
# 零安装即可查询（本环境已有 postgres:16-alpine）。ES 侧仅需 curl。
#
# 前置：先执行 ./create-indices.sh 建好索引。
#
# 用法（默认值已对齐本项目 dev 环境，可用环境变量覆盖）：
#   ./full_build.sh
#   PGPASSWORD=xxx PG_HOST=... ES_URL=... ./full_build.sh
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# 凭据不硬编码进脚本：默认从 note 服务的 application-dev.yml（已 gitignore）解析，
# 本地存在即开箱即跑；CI 或他机可用下方环境变量覆盖。
DEV_YML="${DEV_YML:-${SCRIPT_DIR}/../../hannote-note/hannote-note-biz/src/main/resources/application-dev.yml}"

yml_get() { # $1=键名，取 "key: value" 的 value（去引号/注释/空白）
  [ -f "${DEV_YML}" ] || return 0
  grep -m1 -E "^[[:space:]]*$1:" "${DEV_YML}" 2>/dev/null \
    | sed -E 's/^[^:]*:[[:space:]]*//; s/[[:space:]]*#.*$//; s/^["'"'"']//; s/["'"'"']$//; s/[[:space:]]*$//'
}

# 从 jdbc:postgresql://host:port/db 解析出 host/port/db
JDBC_URL="$(yml_get url)"
YML_HOST="$(echo "${JDBC_URL}" | sed -nE 's#.*//([^:/]+).*#\1#p')"
YML_PORT="$(echo "${JDBC_URL}" | sed -nE 's#.*//[^:/]+:([0-9]+).*#\1#p')"
YML_DB="$(echo "${JDBC_URL}"   | sed -nE 's#.*/([^/?]+)(\?.*)?$#\1#p')"

PG_HOST="${PG_HOST:-${YML_HOST:-101.43.61.11}}"
PG_PORT="${PG_PORT:-${YML_PORT:-5432}}"
PG_USER="${PG_USER:-$(yml_get username)}"; PG_USER="${PG_USER:-postgres}"
PG_DB="${PG_DB:-${YML_DB:-hannote}}"
PGPASSWORD="${PGPASSWORD:-$(yml_get password)}"
ES_URL="${ES_URL:-http://192.168.1.117:9200}"
PG_IMAGE="${PG_IMAGE:-postgres:16-alpine}"

if [ -z "${PGPASSWORD}" ]; then
  echo "## 未取得 PG 密码：请设置环境变量 PGPASSWORD，或确保 ${DEV_YML} 内含 password。" >&2
  exit 1
fi

# 通过 docker psql 执行 SQL，将结果集聚合为一个 JSON 数组打印到 stdout。
# $1 = 内层 SELECT 语句（不含末尾分号）
pg_query_json() {
  local inner_sql="$1"
  docker run --rm -e PGPASSWORD="${PGPASSWORD}" "${PG_IMAGE}" \
    psql -h "${PG_HOST}" -p "${PG_PORT}" -U "${PG_USER}" -d "${PG_DB}" -tAc \
    "select coalesce(json_agg(t), '[]'::json) from ( ${inner_sql} ) t;"
}

# 将 JSON 数组灌入指定 ES 索引。$1=索引名 $2=JSON 数组
bulk_load() {
  local index="$1" json="$2" count
  count=$(echo "${json}" | jq 'length')
  echo "==> [${index}] 待导入 ${count} 条 ..."
  if [ "${count}" -eq 0 ]; then
    echo "    (无数据，跳过)"
    return
  fi

  # jq 逐条产出两行：动作行 + 文档行，构成 _bulk 所需的 NDJSON
  local ndjson resp errors
  ndjson=$(echo "${json}" | jq -rc --arg idx "${index}" \
    '.[] | ({ index: { _index: $idx, _id: .id } }), .')

  resp=$(printf '%s\n' "${ndjson}" | curl -s -X POST "${ES_URL}/_bulk" \
    -H 'Content-Type: application/x-ndjson' --data-binary @-)

  errors=$(echo "${resp}" | jq '.errors')
  if [ "${errors}" = "true" ]; then
    echo "    ## _bulk 存在错误，前若干条明细：" >&2
    echo "${resp}" | jq '[.items[] | select(.index.error)][:5]' >&2
    exit 1
  fi
  echo "    OK: 已提交 ${count} 条。"
}

# ---------------- 笔记索引 ----------------
NOTE_SQL="
  select n.id, n.title, coalesce(n.topic_name,'') as topic, n.type,
         coalesce(split_part(n.img_uris,',',1),'') as cover,
         u.nickname as creator_nickname, u.avatar as creator_avatar,
         to_char(n.create_time,'YYYY-MM-DD HH24:MI:SS') as create_time,
         to_char(n.update_time,'YYYY-MM-DD HH24:MI:SS') as update_time,
         coalesce(nc.like_total,0)    as like_total,
         coalesce(nc.collect_total,0) as collect_total,
         coalesce(nc.comment_total,0) as comment_total
  from t_note n
  left join t_user u        on n.creator_id = u.id
  left join t_note_count nc on n.id = nc.note_id
  where n.visible = 0 and n.status = 1
  order by n.id
"

# ---------------- 用户索引 ----------------
USER_SQL="
  select u.id, u.nickname, u.avatar, u.hannote_id,
         coalesce(uc.note_total,0) as note_total,
         coalesce(uc.fans_total,0) as fans_total
  from t_user u
  left join t_user_count uc on u.id = uc.user_id
  where u.status = 0 and u.is_deleted = false
  order by u.id
"

echo "PG = ${PG_USER}@${PG_HOST}:${PG_PORT}/${PG_DB}   ES = ${ES_URL}"
echo "==> 查询笔记数据 ..."
NOTE_JSON=$(pg_query_json "${NOTE_SQL}")
bulk_load "note" "${NOTE_JSON}"

echo "==> 查询用户数据 ..."
USER_JSON=$(pg_query_json "${USER_SQL}")
bulk_load "user" "${USER_JSON}"

# 刷新后按 _count 校验
curl -s -o /dev/null -X POST "${ES_URL}/note,user/_refresh"
echo "==> 校验（ES _count）："
printf '    note = %s\n' "$(curl -s "${ES_URL}/note/_count" | jq '.count')"
printf '    user = %s\n' "$(curl -s "${ES_URL}/user/_count" | jq '.count')"
echo "==> 全量构建完成。"
