# hongs-serv-hadoop

将 Lucene 索引存储到 Hadoop HDFS，实现分布式存储与搜索能力，适配 hongs-serv-search 模块。

## 配置说明

### 启用 HadoopConn

在 `default.properties` 中配置：

```properties
# 指定连接获取器为 HadoopConn
core.lucene.conn.getter.class=io.github.ihongs.dh.lucene.conn.HadoopConn$Getter
```

### 配置说明

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `core.hadoop.fs.defaultFS` | HDFS 名称节点地址 | `hdfs://localhost:9000` |
| `core.hadoop.data.path` | 数据路径前缀（相对路径自动拼接，可通过 `${DATA_PATH}` 引用） | 空串 |
| `core.hadoop.dfs.namenode.http-address` | NameNode HTTP 地址 | 自动推断 |
| `core.hadoop.dfs.replication` | 副本数 | `3` |
| `core.hadoop.dfs.blocksize` | 块大小 | `134217728` (128MB) |
| `core.hadoop.ram.buf.size` | RAM 缓冲区大小 | `16` (16MB) |
| `core.hadoop.max.buf.docs` | 最大缓冲文档数 | `-1` (自动) |

### 配置示例

```properties
# 连接配置
core.lucene.conn.getter.class=io.github.ihongs.dh.lucene.conn.HadoopConn$Getter

# Hadoop 配置（本地开发环境）
core.hadoop.fs.defaultFS=hdfs://localhost:9000
core.hadoop.data.path=/hongs/indexes
core.hadoop.dfs.replication=2
core.hadoop.dfs.blocksize=134217728
core.hadoop.ram.buf.size=16
core.hadoop.max.buf.docs=-1
```

#### 路径拼接规则

当 `data.path` 不为空，且 `dbpath` 不以 `/`、`hdfs://` 或 `file://` 开头时，自动拼接 `data.path + "/" + dbpath`

| data.path | dbpath | 实际路径 | 说明 |
|-----------|--------|----------|------|
| `/hongs/indexes` | `my_index` | `/hongs/indexes/my_index` | 相对路径自动拼接 |
| `/hongs/indexes` | `/my/path` | `/my/path` | 绝对路径不拼接 |
| `` (空) | `my_index` | `/my_index` | 等同绝对路径 |

## Hadoop 环境搭建

### 1. 下载 Hadoop

```bash
# 下载 Hadoop 3.3.6（与项目依赖版本一致）
wget https://archive.apache.org/dist/hadoop/common/hadoop-3.3.6/hadoop-3.3.6.tar.gz
tar -xzf hadoop-3.3.6.tar.gz
cd hadoop-3.3.6
```

### 2. 配置环境变量

```bash
# ~/.bashrc 或 ~/.bash_profile
export HADOOP_HOME=/path/to/hadoop-3.3.6
export HADOOP_CONF_DIR=$HADOOP_HOME/etc/hadoop
export PATH=$HADOOP_HOME/bin:$HADOOP_HOME/sbin:$PATH
export JAVA_HOME=/path/to/java
```

### 3. 修改 Hadoop 配置文件

#### `etc/hadoop/core-site.xml`

```xml
<configuration>
    <property>
        <name>fs.defaultFS</name>
        <value>hdfs://localhost:9000</value>
    </property>
    <property>
        <name>hadoop.tmp.dir</name>
        <value>/tmp/hadoop-${user.name}</value>
    </property>
</configuration>
```

#### `etc/hadoop/hdfs-site.xml`

```xml
<configuration>
    <property>
        <name>dfs.replication</name>
        <value>1</value>
    </property>
    <property>
        <name>dfs.namenode.name.dir</name>
        <value>/opt/hadoop/data/namenode</value>
    </property>
    <property>
        <name>dfs.datanode.data.dir</name>
        <value>/opt/hadoop/data/datanode</value>
    </property>
</configuration>
```

#### `etc/hadoop/hadoop-env.sh`

```bash
export JAVA_HOME=/path/to/java
```

### 4. 初始化 NameNode

```bash
# 创建数据目录
mkdir -p /opt/hadoop/data/namenode
mkdir -p /opt/hadoop/data/datanode

# 格式化 NameNode（首次启动前执行）
hdfs namenode -format
```

### 5. 启动 HDFS

```bash
# 启动 NameNode 和 DataNode
start-dfs.sh

# 查看状态
jps
```

### 6. 验证 HDFS

```bash
# 创建目录
hdfs dfs -mkdir -p /hongs/indexes

# 查看目录
hdfs dfs -ls /hongs

# 上传测试文件
echo "test" > test.txt
hdfs dfs -put test.txt /hongs/
```

### 7. 停止 HDFS

```bash
stop-dfs.sh
```

## 分布式集群配置（可选）

### 修改配置文件

#### `etc/hadoop/workers`

```
datanode1.example.com
datanode2.example.com
datanode3.example.com
```

#### `etc/hadoop/hdfs-site.xml`（集群模式）

```xml
<configuration>
    <property>
        <name>dfs.replication</name>
        <value>3</value>
    </property>
    <property>
        <name>dfs.namenode.name.dir</name>
        <value>hdfs://namenode:9000/namenode</value>
    </property>
    <property>
        <name>dfs.datanode.data.dir</name>
        <value>/opt/hadoop/data/datanode</value>
    </property>
</configuration>
```

### 配置 SSH 免密登录

```bash
# 在 NameNode 上生成密钥
ssh-keygen -t rsa -P ""

# 分发公钥到所有 DataNode
ssh-copy-id datanode1.example.com
ssh-copy-id datanode2.example.com
ssh-copy-id datanode3.example.com
```

### 启动集群

```bash
# 在 NameNode 上启动
start-dfs.sh

# 检查集群状态
hdfs dfsadmin -report
```

## 核心实现

### HadoopConn 类结构

```
HadoopConn
├── getWriter()     // 获取索引写入器
├── getReader()     // 获取索引读取器
├── getFinder()     // 获取索引搜索器
├── write()         // 写入文档
└── close()         // 关闭连接
```

### 分布式锁机制

项目实现了基于 ZooKeeper 的分布式锁（使用临时节点）：

```
ZooKeeper 节点结构：
/hongs/locks/
└── /index_path/
    └── write.lock   // 临时节点锁
```

**工作原理**：
1. 获取锁时，在 ZooKeeper 上创建临时节点（EPHEMERAL）
2. ZooKeeper 的原子创建操作确保只有一个进程能成功
3. 释放锁时删除临时节点
4. **关键特性**：进程异常退出（如 kill -9）时，ZooKeeper 会话断开，临时节点自动删除，不会造成死锁

**ZooKeeper 配置（可选）**：

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `core.zookeeper.connect` | ZooKeeper 地址 | `localhost:2181` |
| `core.zookeeper.timeout` | 会话超时时间(ms) | `30000` |

> **锁路径**：`/hongs/locks/{dbname}/write.lock`

**Docker 环境配置**：

```properties
# 本地连接 Docker 中的 ZooKeeper
core.zookeeper.connect=localhost:2181

# 容器内部连接（应用也在容器中）
core.zookeeper.connect=zookeeper:2181
```

## Maven 依赖

```xml
<dependency>
    <groupId>io.github.ihongs</groupId>
    <artifactId>hongs-serv-hadoop</artifactId>
    <version>1.1-SNAPSHOT</version>
</dependency>
```

## 快速部署

项目提供了 Podman/Docker Compose 配置，可以快速搭建一个测试用的 Hadoop 集群（1个 ZooKeeper + 1个 NameNode + 2个 DataNode）。

### 启动集群

```bash
# 进入项目目录
cd hongs-serv-hadoop/docker

# 使用 podman-compose 启动
podman-compose up -d

# 或使用 docker-compose
docker-compose up -d
```

### 集群结构

| 服务 | 容器名 | 端口 | 说明 |
|------|--------|------|------|
| ZooKeeper | `hadoop-zookeeper` | 2181 | 分布式锁服务 |
| NameNode | `hadoop-namenode` | 9870, 9000, 8020 | HDFS 名称节点 |
| DataNode1 | `hadoop-datanode1` | 9864, 9866 | 数据节点1 |
| DataNode2 | `hadoop-datanode2` | 9865, 9867 | 数据节点2 |
| Client | `hadoop-client` | - | 客户端容器 |

### 访问集群

```bash
# 访问 NameNode Web UI
http://localhost:9870

# 进入客户端容器执行 HDFS 命令
podman exec -it hadoop-client bash

# 在容器内执行 HDFS 命令
hdfs dfs -mkdir -p /hongs/indexes
hdfs dfs -ls /hongs
```

### 测试 ZooKeeper 连接

```bash
# 使用 ZooKeeper 客户端测试连接
podman exec -it hadoop-zookeeper zkCli.sh

# 在 ZooKeeper 客户端中查看节点
ls /
ls /hongs/locks

# 或使用本地 zkCli.sh（需安装 ZooKeeper）
zkCli.sh -server localhost:2181
```

### 配置说明

配置文件位于 `docker/config/` 目录：

- `core-site.xml` - HDFS 核心配置
- `hdfs-site.xml` - HDFS 站点配置（副本数=2）

### 停止集群

```bash
# 停止并保留数据
podman-compose down

# 停止并删除数据（重新格式化）
podman-compose down -v
```

### 注意事项

1. **HDFS 写入特性**：HDFS 设计为"一次写入，多次读取"，Lucene 的段合并操作会产生临时文件，需要确保足够的 HDFS 空间
2. **副本数配置**：生产环境建议设置为 3，开发环境可设置为 1
3. **内存配置**：根据索引大小调整 `core.hadoop.ram.buf.size` 参数
4. **ZooKeeper 依赖**：分布式锁依赖 ZooKeeper，需确保 ZooKeeper 服务正常运行
5. **锁自动释放**：使用 ZooKeeper 临时节点，进程异常退出时锁会自动释放，不会造成死锁

### 常见问题

| 问题 | 原因 | 解决方案 |
|------|------|----------|
| 无法连接 HDFS | NameNode 未启动 | 检查 `start-dfs.sh` 输出，查看日志 |
| 权限拒绝 | HDFS 目录权限不足 | 使用 `hdfs dfs -chmod` 修改权限 |
| 无法获取锁 | ZooKeeper 未启动或网络问题 | 检查 ZooKeeper 状态，确保网络连通 |
| 写入缓慢 | 网络延迟或副本数过高 | 检查网络，调整副本数 |
