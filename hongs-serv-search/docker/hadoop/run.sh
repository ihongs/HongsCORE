#!/bin/bash
# Hadoop HDFS 测试环境管理脚本 (使用 podman)
# 用法:
#   ./run.sh start   - 启动 HDFS 集群
#   ./run.sh stop    - 停止 HDFS 集群
#   ./run.sh status  - 查看运行状态
#   ./run.sh reset   - 删除数据重新初始化

DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"

IMAGE="docker.io/apache/hadoop:3.3.6"
NET="hadoop_net"
TMP_DIR="$DIR/tmp"
mkdir -p "$TMP_DIR"

case "$1" in
    start)
        # 创建网络
        podman network exists $NET 2>/dev/null || podman network create $NET

        mkdir -p data/namenode data/datanode

        # 修复目录权限 (容器内以 uid=1000 运行)
        podman run --rm --user root \
            -v "$DIR/data/namenode":/opt/hadoop/dfs/name \
            -v "$DIR/data/datanode":/opt/hadoop/dfs/data \
            $IMAGE chown -R 1000:100 /opt/hadoop/dfs/name /opt/hadoop/dfs/data

        # 格式化 namenode (仅首次)
        if [ -z "$(ls -A data/namenode 2>/dev/null)" ]; then
            echo "Formatting namenode..."
            podman run --rm \
                -v "$DIR/data/namenode":/opt/hadoop/dfs/name \
                -v "$DIR/config":/opt/hadoop/etc/hadoop \
                $IMAGE hdfs namenode -format -force
        fi

        # 1) 使用静态 IP
        NN_IP="10.89.1.10"
        DN_IP="10.89.1.11"
        echo "Static IPs - Namenode: $NN_IP, Datanode: $DN_IP"

        # 2) 创建完整的 hosts 文件，两个容器都用得到！
        cat > "$TMP_DIR/hosts.namenode" <<EOF
127.0.0.1 localhost namenode
::1 localhost
$DN_IP datanode
EOF
        cat > "$TMP_DIR/hosts.datanode" <<EOF
127.0.0.1 localhost datanode
::1 localhost
$NN_IP namenode
EOF

        # 3) 现在正式启动 namenode，挂载正确的、预先配置好的 hosts 文件！
        echo "Starting namenode with configured hosts..."
        podman run -d --name namenode --hostname namenode --net $NET --ip $NN_IP \
            -p 9000:9000 -p 9870:9870 \
            -e HADOOP_HOME=/opt/hadoop \
            -e ENSURE_NAMENODE_DIR=true \
            -v "$TMP_DIR/hosts.namenode":/etc/hosts:Z \
            -v "$DIR/data/namenode":/opt/hadoop/dfs/name \
            -v "$DIR/config":/opt/hadoop/etc/hadoop \
            $IMAGE hdfs namenode

        # 4) 等待 namenode 就绪
        echo "Waiting for namenode to be ready..."
        for i in $(seq 1 30); do
            if podman exec namenode hdfs dfsadmin -safemode get 2>/dev/null | grep -q OFF; then
                echo "Namenode is ready!"
                break
            fi
            sleep 2
        done

        # 5) 启动 datanode，同样挂载预先配置好的 hosts 文件
        echo "Starting datanode..."
        podman run -d --name datanode --hostname datanode --net $NET --ip $DN_IP \
            -p 9864:9864 -p 9866:9866 -p 9867:9867 \
            -e HADOOP_HOME=/opt/hadoop \
            -v "$TMP_DIR/hosts.datanode":/etc/hosts:Z \
            -v "$DIR/data/datanode":/opt/hadoop/dfs/data \
            -v "$DIR/config":/opt/hadoop/etc/hadoop \
            $IMAGE hdfs datanode

        echo "HDFS cluster started!"
        echo "Monitor at: http://localhost:9870"
        echo "HDFS URI: hdfs://localhost:9000"
        ;;
    stop)
        podman rm -f datanode namenode 2>/dev/null
        echo "HDFS cluster stopped."
        ;;
    status)
        podman ps --filter name=namenode --filter name=datanode --format 'table {{.Names}}\t{{.Status}}'
        ;;
    reset)
        podman rm -f datanode namenode 2>/dev/null
        podman run --rm --user root \
            -v "$DIR/data/namenode":/opt/hadoop/dfs/name \
            -v "$DIR/data/datanode":/opt/hadoop/dfs/data \
            $IMAGE bash -c 'rm -rf /opt/hadoop/dfs/name/current /opt/hadoop/dfs/name/in_use.lock /opt/hadoop/dfs/data/*'
        rm -rf "$TMP_DIR"
        echo "Cluster data removed. Run './run.sh start' to reinitialize."
        ;;
    *)
        echo "Usage: $0 {start|stop|status|reset}"
        exit 1
        ;;
esac
