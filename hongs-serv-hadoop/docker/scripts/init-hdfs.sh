#!/bin/bash

# HDFS 初始化脚本
# 首次启动时格式化 NameNode

echo "Initializing HDFS..."

# 检查 NameNode 数据目录是否为空
if [ -z "$(ls -A /hadoop/dfs/name)" ]; then
    echo "Formatting NameNode..."
    hdfs namenode -format -force
else
    echo "NameNode already formatted, skipping..."
fi

# 启动 NameNode
echo "Starting NameNode..."
hdfs namenode
