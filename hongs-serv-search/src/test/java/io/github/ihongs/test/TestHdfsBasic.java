package io.github.ihongs.test;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FSDataInputStream;

public class TestHdfsBasic {

    public static void main(String[] args) throws Exception {
        String uri = args.length > 0 ? args[0] : "hdfs://localhost:9000";

        Configuration conf = new Configuration();
        conf.set("fs.defaultFS", uri);
        conf.set("dfs.client.use.datanode.hostname", "true");

        System.out.println("Connecting to " + uri + " ...");
        FileSystem fs = FileSystem.get(conf);

        // 1. 测试连接
        System.out.println("[1] Connection OK, root exists: " + fs.exists(new Path("/")));

        // 2. 创建目录
        Path dir = new Path("/hongs/data/lucene/test");
        fs.mkdirs(dir);
        System.out.println("[2] mkdir " + dir + " OK, exists: " + fs.exists(dir));

        // 3. 写文件
        Path file = new Path(dir, "hello.txt");
        FSDataOutputStream out = fs.create(file, true);
        out.writeUTF("Hello HDFS!");
        out.close();
        System.out.println("[3] write " + file + " OK");

        // 4. 读文件
        FSDataInputStream in = fs.open(file);
        String content = in.readUTF();
        in.close();
        System.out.println("[4] read " + file + " => " + content);

        // 5. 删除测试数据
        fs.delete(file, false);
        fs.delete(dir, true);
        System.out.println("[5] cleanup OK");

        fs.close();
        System.out.println("All HDFS basic tests passed!");
    }

}
