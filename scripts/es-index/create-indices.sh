#!/usr/bin/env bash
#
# 创建搜索服务所需的 Elasticsearch 索引：note（笔记）、user（用户）。
#
# ��注意事项：
#   - number_of_replicas 设为 0：本地单节点 dev 集群，副本无法分配会导致索引 yellow。
#   - user 索引字段 xiaohashu_id 重命名为 hannote_id，与本项目命名保持一致。
#   - 分词策略照旧：建倒排索引用 ik_max_word（查全），搜索用 ik_smart（查准），
#     依赖 ES 已安装 analysis-ik 插件（本环境 analysis-ik 9.4.3 已装）。
#
# 幂等：索引若已存在会先删除再重建（会清空其中的文档）。
#
# 用法：
#   ./create-indices.sh                       # 使用默认 ES 地址
#   ES_URL=http://192.168.1.117:9200 ./create-indices.sh
#
set -euo pipefail

ES_URL="${ES_URL:-http://192.168.1.117:9200}"

# 创建单个索引：$1=索引名 $2=索引定义 JSON
create_index() {
  local name="$1" body="$2"
  echo "==> [${name}] 若已存在则删除 ..."
  curl -s -o /dev/null -w '    DELETE /%{http_code}\n' -X DELETE "${ES_URL}/${name}" || true
  echo "==> [${name}] 创建索引 ..."
  local resp
  resp=$(curl -s -X PUT "${ES_URL}/${name}" \
    -H 'Content-Type: application/json' \
    --data-binary "${body}")
  if echo "${resp}" | jq -e '.acknowledged == true' >/dev/null 2>&1; then
    echo "    OK: ${resp}"
  else
    echo "    ## 创建失败: ${resp}" >&2
    exit 1
  fi
}

NOTE_INDEX='{
  "settings": { "number_of_shards": 1, "number_of_replicas": 0 },
  "mappings": {
    "properties": {
      "id":               { "type": "long" },
      "cover":            { "type": "keyword" },
      "title":            { "type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_smart" },
      "topic":            { "type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_smart" },
      "creator_nickname": { "type": "keyword" },
      "creator_avatar":   { "type": "keyword" },
      "type":             { "type": "integer" },
      "create_time":      { "type": "date", "format": "yyyy-MM-dd HH:mm:ss" },
      "update_time":      { "type": "date", "format": "yyyy-MM-dd HH:mm:ss" },
      "like_total":       { "type": "integer" },
      "collect_total":    { "type": "integer" },
      "comment_total":    { "type": "integer" }
    }
  }
}'

USER_INDEX='{
  "settings": { "number_of_shards": 1, "number_of_replicas": 0 },
  "mappings": {
    "properties": {
      "id":         { "type": "long" },
      "nickname":   { "type": "text", "analyzer": "ik_max_word", "search_analyzer": "ik_smart" },
      "avatar":     { "type": "keyword" },
      "hannote_id": { "type": "keyword" },
      "note_total": { "type": "integer" },
      "fans_total": { "type": "integer" }
    }
  }
}'

echo "ES = ${ES_URL}"
create_index "note" "${NOTE_INDEX}"
create_index "user" "${USER_INDEX}"
echo "==> 索引创建完成。"
